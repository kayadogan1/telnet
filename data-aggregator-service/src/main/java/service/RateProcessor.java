package service;

import jakarta.annotation.PostConstruct;
import model.CalculatedRate;
import model.Rate;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import service.kafka.KafkaRateProducer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RateProcessor.class);
    private final KafkaRateProducer kafkaProducer;

    // Platformlardan gelen oranları burada tutacağız
    private final Map<String, List<Rate>> incomingRates = new ConcurrentHashMap<>();

    public RateProcessor(KafkaRateProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @KafkaListener(topics = "forex-rates", groupId = "data-aggregator-group")
    public void listen(ConsumerRecord<String, String> record) {
        try {
            String message = record.value();
            logger.info("Kafka'dan gelen veri: {}", message);
            Rate rawRate = parseRate(message);
            if (rawRate != null) {
                handleRate(rawRate);
            }
        } catch (Exception e) {
            logger.error("Veri işlenirken hata oluştu: {}", record.value(), e);
        }
    }

    private Rate parseRate(String message) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length == 4) {
                String rateName = parts[0];
                BigDecimal bid = new BigDecimal(parts[1]);
                BigDecimal ask = new BigDecimal(parts[2]);
                Instant timestamp = Instant.parse(parts[3]);
                return new Rate(rateName, bid, ask, timestamp);
            } else {
                logger.warn("Geçersiz format: {}", message);
                return null;
            }
        } catch (Exception e) {
            logger.error("Rate ayrıştırılamadı: {}", message, e);
            return null;
        }
    }

    private void handleRate(Rate rate) {
        // PF1_USDTRY → USDTRY
        String pureName = extractPureRateName(rate.getRateName());

        incomingRates.putIfAbsent(pureName, new ArrayList<>());
        List<Rate> list = incomingRates.get(pureName);

        // Aynı platformdan gelen tekrar veri varsa güncelle
        list.removeIf(r -> getPlatform(r.getRateName()).equals(getPlatform(rate.getRateName())));
        list.add(rate);

        // Yeterli sayıya ulaştıysa hesapla
        if (list.size() >= 2) {
            CalculatedRate result = calculateAverage(pureName, list);
            kafkaProducer.sendCalculatedRate("calculated-rates", result.toString());
            logger.info("Hesaplanan rate gönderildi: {}", result);
        }
    }

    private CalculatedRate calculateAverage(String rateName, List<Rate> list) {
        BigDecimal totalBid = BigDecimal.ZERO;
        BigDecimal totalAsk = BigDecimal.ZERO;

        for (Rate rate : list) {
            totalBid = totalBid.add(rate.getBid());
            totalAsk = totalAsk.add(rate.getAsk());
        }

        BigDecimal avgBid = totalBid.divide(BigDecimal.valueOf(list.size()), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal avgAsk = totalAsk.divide(BigDecimal.valueOf(list.size()), 6, BigDecimal.ROUND_HALF_UP);

        return new CalculatedRate(rateName, avgBid, avgAsk, Instant.now());
    }

    private String extractPureRateName(String platformRateName) {
        // PF1_USDTRY → USDTRY
        int index = platformRateName.indexOf("_");
        return (index > 0) ? platformRateName.substring(index + 1) : platformRateName;
    }

    private String getPlatform(String rateName) {
        // PF1_USDTRY → PF1
        int index = rateName.indexOf("_");
        return (index > 0) ? rateName.substring(0, index) : "UNKNOWN";
    }

    @PostConstruct
    public void testInit() {
        System.out.println("✅ RateProcessor hazır.");
    }
}
