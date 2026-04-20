package com.hotelio.booking.grpc;

import com.hotelio.booking.service.BookingServiceImpl;
import com.hotelio.booking.entity.Booking;

import net.devh.boot.grpc.server.service.GrpcService;
import io.grpc.stub.StreamObserver;

import com.hotelio.proto.booking.*;

import java.util.List;

@GrpcService
public class GrpcBookingService extends BookingServiceGrpc.BookingServiceImplBase {

    private final BookingServiceImpl service;

    public GrpcBookingService(BookingServiceImpl service) {
        this.service = service;
    }

    @Override
    public void createBooking(BookingRequest req, StreamObserver<BookingResponse> resp) {

        Booking b = service.create(
                req.getUserId(),
                req.getHotelId(),
                req.getPromoCode()
        );

        resp.onNext(BookingResponse.newBuilder()
                .setId(b.getId().toString())
                .setUserId(safe(b.getUserId()))
                .setHotelId(safe(b.getHotelId()))
                .setPromoCode(safe(b.getPromoCode()))
                .setPrice(b.getPrice())
                .setDiscountPercent(0.0)
                .setCreatedAt(b.getCreatedAt().toString())
                .build());

        resp.onCompleted();
    }

    @Override
    public void listBookings(BookingListRequest req,
                            StreamObserver<BookingListResponse> resp) {

        List<Booking> bookings;

        String userId = "";

        try {
            if (req != null) {
                userId = req.getUserId();
            }
        } catch (Exception ignored) {
        }

        try {
            if (userId == null || userId.isBlank()) {
                bookings = service.findAll();
            } else {
                bookings = service.findByUserId(userId);
            }
        } catch (Exception e) {
            bookings = service.findAll();
        }

        BookingListResponse.Builder response = BookingListResponse.newBuilder();

        for (Booking b : bookings) {
            response.addBookings(
                    BookingResponse.newBuilder()
                            .setId(String.valueOf(b.getId()))
                            .setUserId(safe(b.getUserId()))
                            .setHotelId(safe(b.getHotelId()))
                            .setPromoCode(safe(b.getPromoCode()))
                            .setPrice(b.getPrice())
                            .setDiscountPercent(0.0)
                            .setCreatedAt(b.getCreatedAt().toString())
                            .build()
            );
        }

        resp.onNext(response.build());
        resp.onCompleted();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}