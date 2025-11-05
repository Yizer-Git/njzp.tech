package com.example.Kcsj.service;

import com.example.Kcsj.dto.WeatherData;

public interface WeatherService {

    /**
     * Get weather snapshot for given coordinates, reading from cache when possible.
     */
    WeatherData getWeatherSnapshot(double latitude, double longitude);

    /**
     * Force refresh and cache the latest weather for the provided coordinates.
     */
    WeatherData refreshForecast(double latitude, double longitude);

    /**
     * Retrieve default cached weather for configured coordinates.
     */
    WeatherData getDefaultWeatherSnapshot();
}
