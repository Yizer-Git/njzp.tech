package com.example.Kcsj.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Simplified weather snapshot used by solution generation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeatherData implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal currentTemperature;
    private BigDecimal currentHumidity;
    private BigDecimal windSpeed;
    private BigDecimal precipitationProbability;
    private BigDecimal precipitationAmount;
    private BigDecimal solarRadiation;
    private String weatherSummary;
    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fetchedAt;

    private List<DailyForecast> forecast;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyForecast implements Serializable {
        private static final long serialVersionUID = 1L;

        private LocalDate date;
        private BigDecimal temperatureMax;
        private BigDecimal temperatureMin;
        private BigDecimal precipitationSum;
        private BigDecimal sunshineDuration;
    }
}
