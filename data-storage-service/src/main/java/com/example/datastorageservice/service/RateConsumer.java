package com.example.datastorageservice.service;


import com.example.datastorageservice.model.Rate;
import com.example.datastorageservice.repository.RateRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Component
public class RateConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RateConsumer.class);
    private final RateRepository rateRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;


    public RateConsumer(RateRepository rateRepository) {
        this.rateRepository = rateRepository;
    }

    @KafkaListener(topics = "forex-rates", groupId = "data-storage-group")  // Kafka konusunu ve grubu belirtir
    public void listen(ConsumerRecord<String, String> record) {
        try {
            String[] parts = record.value().split("\\|"); // Veriyi ayırmak için "|" karakterini kullanıyoruz
            if (parts.length == 4) {
                String rateName = parts[0];
                BigDecimal bid = new BigDecimal(parts[1]);
                BigDecimal ask = new BigDecimal(parts[2]);
                Instant rateUpdatetime = Instant.parse(parts[3]);

                Rate rate = new Rate(rateName, bid, ask, rateUpdatetime);
                rateRepository.save(rate);
                logger.info("Veritabanına kaydedildi: {}", rate);
            } else {
                logger.warn("Geçersiz veri formatı: {}", record.value());
            }
        } catch (Exception e) {
            logger.error("Veri işleme hatası: {}", record.value(), e);
        }
    }
}