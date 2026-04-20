package com.hotelio.history;

import jakarta.persistence.*;

@Entity
public class BookingHistory {

    @Id
    @GeneratedValue
    private Long id;

    private String payload;

    public Long getId() { return id; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}