package com.ap.ama.tools;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;

import org.springframework.stereotype.Component;

@Component
public class TimeTools {

    @Tool(description = "Returns the current local time")
    public String currentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Tool(description = "Returns the current local day and date")
    public String currentDay() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));
    }
}
