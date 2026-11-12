package com.plass.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        BigDecimal totalRevenue,
        long ticketsSold,
        long confirmedBookings,
        long upcomingEvents,
        long registeredUsers,
        List<DailySales> salesLast14Days,
        List<RecentBooking> recentBookings) {

    public record DailySales(LocalDate date, long tickets, BigDecimal revenue) {
    }

    public record RecentBooking(String reference, String customerName, String eventTitle, int tickets,
            BigDecimal amount, Instant confirmedAt) {
    }
}
