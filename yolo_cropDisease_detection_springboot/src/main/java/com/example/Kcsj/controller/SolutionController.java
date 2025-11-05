package com.example.Kcsj.controller;

import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.GenerateSolutionRequest;
import com.example.Kcsj.dto.SolutionOption;
import com.example.Kcsj.dto.SolutionRecommendation;
import com.example.Kcsj.dto.WeatherData;
import com.example.Kcsj.service.SolutionService;
import com.example.Kcsj.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint for generating intelligent plant protection solutions.
 */
@RestController
@RequestMapping("/solution")
@RequiredArgsConstructor
public class SolutionController {

    private final SolutionService solutionService;
    private final WeatherService weatherService;

    @GetMapping("/catalog")
    public Result<List<SolutionOption>> catalog() {
        return Result.success(solutionService.listCatalog());
    }

    @PostMapping("/generate")
    public Result<SolutionRecommendation> generate(@RequestBody GenerateSolutionRequest request) {
        WeatherData weatherData = request.getOverrideWeather();
        if (weatherData == null && request.getLatitude() != null && request.getLongitude() != null) {
            weatherData = weatherService.getWeatherSnapshot(request.getLatitude(), request.getLongitude());
        } else if (weatherData == null) {
            weatherData = weatherService.getDefaultWeatherSnapshot();
        }

        SolutionRecommendation recommendation = solutionService.generateSolution(
                request.getDiseaseId(),
                request.getCropId(),
                weatherData
        );
        return Result.success(recommendation);
    }
}
