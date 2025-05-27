package service.kafka;

import jakarta.annotation.PostConstruct;
import model.Rate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class KafkaRateProducer {
    private static final Logger logger = Logger.getLogger(KafkaRateProducer.class.getName());
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaRateProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendRate(Rate rate) {
        String message = rate.toString();
        kafkaTemplate.send("forex-rates", message)
                .thenAccept(result -> logger.info("Kafka'ya gönderildi: " + message))
                .exceptionally(ex -> {
                    logger.severe("Kafka'ya gönderilemedi: " + message + " Hata: " + ex.getMessage());
                    return null;
                });
    }

    public void sendCalculatedRate(String topic, String message) {
        kafkaTemplate.send(topic, message)
                .thenAccept(result -> logger.info("Kafka'ya gönderildi: " + message))
                .exceptionally(ex -> {
                    logger.severe("Kafka'ya gönderilemedi: " + message + " Hata: " + ex.getMessage());
                    return null;
                });
    }
}
