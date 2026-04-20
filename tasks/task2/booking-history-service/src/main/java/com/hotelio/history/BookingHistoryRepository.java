package com.hotelio.history;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingHistoryRepository
        extends JpaRepository<BookingHistory, Long> {
}