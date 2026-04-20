package com.hotelio.booking.service;

import com.hotelio.booking.entity.Booking;
import com.hotelio.booking.repo.BookingRepository;
import com.hotelio.booking.kafka.BookingEventProducer;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class BookingServiceImpl {

    private final BookingRepository repo;
    private final BookingEventProducer producer;
    private final RestTemplate rest;

    public BookingServiceImpl(BookingRepository repo,
                              BookingEventProducer producer,
                              RestTemplate rest) {
        this.repo = repo;
        this.producer = producer;
        this.rest = rest;
    }

    public Booking create(String userId, String hotelId, String promo) {

        try {
            rest.getForObject("http://hotelio-monolith:8080/api/users/" + userId, String.class);

            String authorized = rest.getForObject(
                    "http://hotelio-monolith:8080/api/users/" + userId + "/authorized",
                    String.class
            );

            if (!"true".equalsIgnoreCase(authorized)) {
                throw new RuntimeException("User not authorized");
            }

            rest.getForObject("http://hotelio-monolith:8080/api/hotels/" + hotelId, String.class);

            // не работает
            String operational = rest.getForObject(
                    "http://hotelio-monolith:8080/api/hotels/" + hotelId + "/operational",
                    String.class
            );

            if (!"true".equalsIgnoreCase(operational)) {
                throw new RuntimeException("Hotel is not operational");
            }

            // полностью забронирован
            String fullyBooked = rest.getForObject(
                    "http://hotelio-monolith:8080/api/hotels/" + hotelId + "/fully-booked",
                    String.class
            );

            if ("true".equalsIgnoreCase(fullyBooked)) {
                throw new RuntimeException("Hotel is fully booked");
            }

            // недоверенный
            String trusted = rest.getForObject(
                    "http://hotelio-monolith:8080/api/reviews/hotel/" + hotelId + "/trusted",
                    String.class
            );

            if (!"true".equalsIgnoreCase(trusted)) {
                throw new RuntimeException("Hotel is not trusted");
            }

            if (promo != null && !promo.isEmpty()) {
                String url = "http://hotelio-monolith:8080/api/promos/validate"
                        + "?code=" + promo
                        + "&userId=" + userId;

                rest.postForObject(url, null, String.class);
            }

        } catch (Exception e) {
            throw new RuntimeException("Validation failed: " + e.getMessage(), e);
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setHotelId(hotelId);
        booking.setPromoCode(promo);
        booking.setPrice(100.0);

        repo.save(booking);
        producer.send(booking);

        return booking;
    }

    public List<Booking> findAll() {
        return repo.findAll();
    }

    public List<Booking> findByUserId(String userId) {
        return repo.findByUserId(userId);
    }
}