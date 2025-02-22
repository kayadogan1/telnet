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
import java.time.format.DateTimeFormatter;

@Service
public class RateProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RateProcessor.class); // SLF4J Kullan

    private final KafkaRateProducer kafkaProducer;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    public RateProcessor(KafkaRateProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
        System.out.println("RateProcessor is working");
    }


    @KafkaListener(topics = "forex-rates", groupId = "data-storage-group")
    public void listen(ConsumerRecord<String, String> record) {
        try {
            String message = record.value();
            logger.info("Kafka'dan alınan ham veri: {}", message); // Doğru log formatı
            System.out.println("📥 Kafka'dan mesaj alındı! İçerik: " + record.value()); // Konsola yazdır
            Rate rawRate = parseRate(message);
            if (rawRate != null) {
                CalculatedRate calculatedRate = calculateDerivedRates(rawRate);
                kafkaProducer.sendCalculatedRate("forex-rates", rawRate.toString());
                kafkaProducer.sendCalculatedRate("calculated-rates", calculatedRate.toString());
                logger.info("Hesaplanan veri kafkaya gönderildi: {}", calculatedRate);
            }
        } catch (Exception e) {
            logger.error("Veri işleme hatası: {}", record.value(), e);
        }
    }

    @PostConstruct
    public void testKafkaListener() {
        System.out.println("🔥 Test: Kafka Listener metodunu manuel çağırıyoruz.");
        listen(new ConsumerRecord<>("forex-rates", 0, 0L, "test-key", "EURTR|1.123|1.456|2025-02-17T18:55:00.809154Z"));
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
                logger.warn("Geçersiz veri formatı: {}", message);
                return null;
            }
        } catch (Exception e) {
            logger.error("Veri ayrıştırma hatası: {}", message, e);
            return null;
        }
    }

    private CalculatedRate calculateDerivedRates(Rate rawRate) {
        if (rawRate.getRateName().equals("PF1_USDTRY")) {
            return new CalculatedRate("USDTRY", rawRate.getBid(), rawRate.getAsk(), rawRate.getTimestamp());
        } else if (rawRate.getRateName().equals("PF1_EURTRY")) {
            return new CalculatedRate("EURTRY", rawRate.getBid(), rawRate.getAsk(), rawRate.getTimestamp());
        }
        return new CalculatedRate(rawRate.getRateName(), rawRate.getBid(), rawRate.getAsk(), rawRate.getTimestamp());
    }
}
