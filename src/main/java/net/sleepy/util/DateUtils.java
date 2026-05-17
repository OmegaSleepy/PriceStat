package net.sleepy.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DateUtils {

    public static List<String> getDatesInRange(LocalDate startDate, LocalDate endDate) {
        List<String> dates = new ArrayList<>();

        if (startDate.isAfter(endDate)) {
            return dates; 
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);

        for (int i = 0; i <= daysBetween; i++) {
            LocalDate current = startDate.plusDays(i);
            dates.add(current.format(formatter));
        }

        return dates;
    }

}