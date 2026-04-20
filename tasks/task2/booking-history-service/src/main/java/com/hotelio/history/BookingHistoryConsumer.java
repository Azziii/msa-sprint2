package com.hotelio.history;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Component
public class BookingHistoryConsumer {

    private final BookingHistoryRepository repo;

    public BookingHistoryConsumer(BookingHistoryRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void listen() {
        new Thread(() -> {

            Properties props = new Properties();
            props.put("bootstrap.servers", "kafka:9092");
            props.put("group.id", "history");
            props.put("auto.offset.reset", "earliest");
            props.put("key.deserializer", StringDeserializer.class.getName());
            props.put("value.deserializer", StringDeserializer.class.getName());

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(List.of("booking.created"));

            while (true) {
                for (var rec : consumer.poll(Duration.ofSeconds(1))) {

                    BookingHistory h = new BookingHistory();
                    h.setPayload(rec.value());

                    repo.save(h);

                    System.out.println("SAVED EVENT: " + rec.value());
                }
            }

        }).start();
    }
}