package service.dynamicCalculation.Implamention;

import model.Rate;
import service.dynamicCalculation.ICoordinatorCallbacks;
import service.dynamicCalculation.IDataFetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPDataFetcher implements IDataFetcher, Runnable {

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