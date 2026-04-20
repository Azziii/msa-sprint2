package com.hotelio.booking.kafka;

import org.springframework.stereotype.Component;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import com.hotelio.booking.entity.Booking;

import java.util.Properties;

@Component
public class BookingEventProducer {

    private final KafkaProducer<String, String> producer;

    public BookingEventProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "kafka:9092");
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);
    }

    public void send(Booking booking) {
        String event = String.format(
            "{\"userId\":\"%s\",\"hotelId\":\"%s\",\"promoCode\":\"%s\"}",
            booking.getUserId(),
            booking.getHotelId(),
            booking.getPromoCode() == null ? "" : booking.getPromoCode()
        );

        producer.send(new ProducerRecord<>("booking.created", event));
    }
}