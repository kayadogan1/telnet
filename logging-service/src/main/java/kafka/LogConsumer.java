package kafka;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LogConsumer {

    private static final Logger logger = LoggerFactory.getLogger(LogConsumer.class);
    @PostConstruct
    public void deneme(){
        System.out.println("deneme");
    }
    @KafkaListener(topics = {"forex-rates", "calculated-rates"}, groupId = "logging-group")  // Hem ham hem de hesaplanmış oranları dinliyoruz
    public void listen(ConsumerRecord<String, String> record) {
        try {
            String message = record.value();
            logger.info("Kafka'dan alınan log: {}", message); // Log4j2 ile loglama
            // Opensearch/Elasticsearch'e gönderme işlemleri burada yapılabilir (opsiyonel)
        } catch (Exception e) {
            logger.error("Log işleme hatası: {}", record.value(), e);
        }
    }
}