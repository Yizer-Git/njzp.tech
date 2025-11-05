package com.example.Kcsj.controller;

import com.example.Kcsj.dto.WeatherData;
import com.example.Kcsj.service.WeatherService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Basic smoke validation for solution/ weather endpoints to capture payload shape.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
public class SolutionControllerTest {

    private static final Logger log = LoggerFactory.getLogger(SolutionControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherService weatherService;

    private WeatherData sampleWeather() {
        return WeatherData.builder()
                .currentTemperature(BigDecimal.valueOf(22.4))
                .currentHumidity(BigDecimal.valueOf(0.65))
                .windSpeed(BigDecimal.valueOf(2.8))
                .precipitationProbability(BigDecimal.valueOf(0.35))
                .precipitationAmount(BigDecimal.valueOf(3.2))
                .solarRadiation(BigDecimal.valueOf(120.0))
                .weatherSummary("降雨概率 35%，预计降雨量 3.2 mm")
                .source("test-mock")
                .fetchedAt(LocalDateTime.now())
                .forecast(Collections.emptyList())
                .build();
    }

    @Test
    public void catalogPayload() throws Exception {
        when(weatherService.getDefaultWeatherSnapshot()).thenReturn(sampleWeather());
        when(weatherService.getWeatherSnapshot(anyDouble(), anyDouble())).thenReturn(sampleWeather());

        MvcResult result = mockMvc.perform(get("/solution/catalog"))
                .andExpect(status().isOk())
                .andReturn();

        log.info("/solution/catalog => {}", result.getResponse().getContentAsString());
    }

    @Test
    public void generatePayload() throws Exception {
        when(weatherService.getDefaultWeatherSnapshot()).thenReturn(sampleWeather());
        when(weatherService.getWeatherSnapshot(anyDouble(), anyDouble())).thenReturn(sampleWeather());

        String body = "{\"diseaseId\":1,\"cropId\":1,\"latitude\":30.67,\"longitude\":104.06}";
        MvcResult result = mockMvc.perform(post("/solution/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        log.info("/solution/generate => {}", result.getResponse().getContentAsString());
    }
}
