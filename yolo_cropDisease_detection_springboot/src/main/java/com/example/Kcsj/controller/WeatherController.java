package com.example.Kcsj.controller;

import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.WeatherData;
import com.example.Kcsj.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST interface for meteorological integration and cache control.
 */
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/current")
    public Result<WeatherData> current(@RequestParam(required = false) Double latitude,
                                       @RequestParam(required = false) Double longitude) {
        if (latitude == null || longitude == null) {
            return Result.success(weatherService.getDefaultWeatherSnapshot());
        }
        return Result.success(weatherService.getWeatherSnapshot(latitude, longitude));
    }

    @PostMapping("/refresh")
    public Result<WeatherData> refresh(@RequestParam Double latitude,
                                       @RequestParam Double longitude) {
        return Result.success(weatherService.refreshForecast(latitude, longitude));
    }
}
