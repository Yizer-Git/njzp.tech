package com.example.Kcsj.dto;

import lombok.Data;

/**
 * Request payload for solution generation.
 */
@Data
public class GenerateSolutionRequest {
    private Long diseaseId;
    private Long cropId;
    private Double latitude;
    private Double longitude;
    private WeatherData overrideWeather;
}
