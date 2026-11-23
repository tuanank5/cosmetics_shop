package com.beautyshop.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Bộ lọc kỳ của Dashboard: {@code range=7d|30d|90d} hoặc {@code from}/{@code to} dạng yyyy-MM-dd. */
public class BoLocThongKe_Form {
    private String range;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range == null ? null : range.strip(); }
    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
}
