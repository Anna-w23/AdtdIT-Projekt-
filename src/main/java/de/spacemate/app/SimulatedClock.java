package de.spacemate.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class SimulatedClock {

    private LocalDate today;
    private LocalTime time;

    public SimulatedClock() {
        this.today = LocalDate.now();
        this.time = LocalTime.MIDNIGHT;
    }

    public LocalDate today() {
        return today;
    }

    public LocalTime time() {
        return time;
    }

    public LocalDateTime now() {
        return LocalDateTime.of(today, time);
    }

    public void advanceTimeTo(LocalTime newTime) {
        if (newTime.isAfter(this.time)) {
            this.time = newTime;
        }
    }

    public void advanceOneDay() {
        today = today.plusDays(1);
        time = LocalTime.MIDNIGHT;
    }

    public void setToday(LocalDate date) {
        today = date;
        time = LocalTime.MIDNIGHT;
    }
}
