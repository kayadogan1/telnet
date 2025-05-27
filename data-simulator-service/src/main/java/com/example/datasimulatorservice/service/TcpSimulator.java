package com.example.datasimulatorservice.service;

import com.example.datasimulatorservice.model.Rate;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

@Configuration
public class TcpSimulator {

    private static final Logger logger = LoggerFactory.getLogger(TcpSimulator.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private int port;

    @Value("${tcp.server.port:8082}")
    public void setPort(int port) {
        this.port = port;
    }

    public TcpSimulator(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                logger.info("TCP Sunucusu başlatıldı, port: {}", port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                logger.error("TCP sunucusu hatası: ", e);
            }
        }).start();
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private final Map<String, Boolean> localSubscriptions = new ConcurrentHashMap<>();
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                startBroadcastLoop();

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    logger.info("Client'tan gelen istek: {}", inputLine);
                    processCommand(inputLine);
                }

            } catch (IOException e) {
                logger.error("Client hatası: ", e);
            } finally {
                scheduler.shutdownNow(); // bu metod ScheduledExecutorService içinde var
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                    clientSocket.close();
                } catch (IOException e) {
                    logger.error("Client kapatma hatası: ", e);
                }
            }
        }

        private void processCommand(String command) {
            String[] parts = command.split("\\|");
            String cmd = parts[0];

            switch (cmd) {
                case "subscribe":
                    if (parts.length == 2) {
                        String rateName = parts[1];
                        if (!localSubscriptions.containsKey(rateName)) {
                            localSubscriptions.put(rateName, true);
                            Rate rate = generateRandomRate(rateName);
                            out.println("Subscribed to " + rateName);
                            out.println(formatRate(rate));
                            logger.info("Abone olunan oran: {}", rateName);
                        } else {
                            out.println("Already subscribed to " + rateName);
                        }
                    } else {
                        out.println("ERROR|Invalid subscribe format");
                    }
                    break;
                case "unsubscribe":
                    if (parts.length == 2) {
                        String rateName = parts[1];
                        if (localSubscriptions.containsKey(rateName)) {
                            localSubscriptions.remove(rateName);
                            out.println("Unsubscribed from " + rateName);
                            logger.info("Abonelikten çıkılan oran: {}", rateName);
                        } else {
                            out.println("ERROR|Not subscribed to " + rateName);
                        }
                    } else {
                        out.println("ERROR|Invalid unsubscribe format");
                    }
                    break;
                default:
                    out.println("ERROR|Invalid command");
            }
        }

        private void startBroadcastLoop() {
            scheduler.scheduleAtFixedRate(() -> {
                for (Map.Entry<String, Boolean> entry : localSubscriptions.entrySet()) {
                    if (entry.getValue()) {
                        String rateName = entry.getKey();
                        Rate rate = generateRandomRate(rateName);
                        String message = formatRate(rate);

                        out.println(message);
                        kafkaTemplate.send("forex-rates", message);
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }

        private String formatRate(Rate rate) {
            return rate.getRateName() + "|22:number:" + rate.getBid()
                    + "|25:number:" + rate.getAsk()
                    + "|5:timestamp:" + rate.getTimestamp();
        }
    }

    private Rate generateRandomRate(String rateName) {
        Random random = new Random();
        double bid = 1.0 + random.nextDouble() * 0.2;
        double ask = bid + random.nextDouble() * 0.01;
        return new Rate(rateName, BigDecimal.valueOf(bid), BigDecimal.valueOf(ask), Instant.now());
    }
}
