package service.dynamicCalculation.Implamention;

import model.Rate;
import org.springframework.web.client.RestTemplate;
import service.dynamicCalculation.ICoordinatorCallbacks;
import service.dynamicCalculation.IDataFetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RestDataFetcher implements IDataFetcher {
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ICoordinatorCallbacks coordinator;
    private Timer timer;
    private boolean running = false;

    private final String[] rateNames = {"PF2_USDTRY", "PF2_EURUSD", "PF2_GBPUSD"}; // İsteğe göre dinamikleştirilebilir

    public RestDataFetcher(String baseUrl, ICoordinatorCallbacks coordinator) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
        this.coordinator = coordinator;
    }

    @Override
    public void connect() {
        System.out.println("[INFO] Connected to REST API: " + baseUrl);
        running = true;
        coordinator.onConnect("REST", true);
        startFetching();
    }

    @Override
    public void disconnect() {
        running = false;
        if (timer != null) {
            timer.cancel();
        }
        System.out.println("[INFO] Disconnected from REST API: " + baseUrl);
        coordinator.onDisconnect("REST", true);
    }

    @Override
    public void subscribe(String rateName) {
        System.out.println("[INFO] Subscribed to: " + rateName);
        // İstersen rateNames listesine ekleyebilirsin
    }

    @Override
    public void unsubscribe(String rateName) {
        System.out.println("[INFO] Unsubscribed from: " + rateName);
        // İstersen rateNames listesinden çıkarabilirsin
    }

    private void startFetching() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (running) {
                    fetchRates();
                }
            }
        }, 0, 5000); // Her 5 saniyede bir
    }

    private void fetchRates() {
        for (String rateName : rateNames) {
            try {
                String url = baseUrl + "/api/rates/" + rateName;
                Rate rate = restTemplate.getForObject(url, Rate.class);
                if (rate != null) {
                    coordinator.onRateAvailable("REST", rateName, rate);
                } else {
                    System.err.println("[WARN] Null veri döndü: " + rateName);
                }
            } catch (Exception e) {
                System.err.println("[ERROR] REST veri çekme hatası: " + rateName + " | " + e.getMessage());
            }
        }
    }

    public static class TCPDataFetcher implements IDataFetcher, Runnable {

        private static final Logger logger = Logger.getLogger(TCPDataFetcher.class.getName());

        private final String host;
        private final int port;
        private volatile boolean running = true;

        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private final ICoordinatorCallbacks coordinator;

        public TCPDataFetcher(String host, int port, ICoordinatorCallbacks coordinator) {
            if (host == null || coordinator == null) {
                throw new IllegalArgumentException("Host and coordinator cannot be null");
            }
            this.host = host;
            this.port = port;
            this.coordinator = coordinator;
        }

        @Override
        public void connect() {
            try {
                socket = new Socket(host, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                logger.info("Connected to TCP server: " + host + ":" + port);
                coordinator.onConnect("TCP", true);

                new Thread(this, "TCP-Fetcher-Thread").start();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Connection error to TCP server: " + host + ":" + port, e);
                coordinator.onConnect("TCP", false);
            }
        }

        @Override
        public void disconnect() {
            running = false;
            try {
                if (writer != null) writer.close();
                if (reader != null) reader.close();
                if (socket != null && !socket.isClosed()) socket.close();

                logger.info("Disconnected from TCP server: " + host + ":" + port);
                coordinator.onDisconnect("TCP", true);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Disconnection error", e);
                coordinator.onDisconnect("TCP", false);
            }
        }

        @Override
        public void subscribe(String rateName) {
            sendCommand("subscribe|" + rateName);
        }

        @Override
        public void unsubscribe(String rateName) {
            sendCommand("unsubscribe|" + rateName);
        }

        private void sendCommand(String command) {
            if (socket != null && socket.isConnected() && writer != null) {
                writer.println(command);
                writer.flush();
            } else {
                logger.warning("Attempted to send command on closed connection: " + command);
            }
        }

        @Override
        public void run() {
            try {
                while (running) {
                    String data = reader.readLine();
                    if (data != null && !data.trim().isEmpty()) {
                        processIncomingData(data);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error during TCP read loop", e);
            } finally {
                disconnect(); // Bağlantı kapanırsa temizle
            }
        }

        private void processIncomingData(String data) {
            try {
                logger.fine("Received TCP data: " + data);

                String[] parts = data.split("\\|");
                if (parts.length == 4) {
                    String rateName = parts[0].trim();
                    BigDecimal bid = new BigDecimal(parts[1].trim());
                    BigDecimal ask = new BigDecimal(parts[2].trim());
                    Instant timestamp = Instant.parse(parts[3].trim());

                    Rate rate = new Rate(rateName, bid, ask, timestamp);
                    coordinator.onRateAvailable("TCP", rateName, rate);
                } else {
                    logger.warning("Invalid data format received: " + data);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to parse TCP data: " + data, e);
            }
        }
    }
}









