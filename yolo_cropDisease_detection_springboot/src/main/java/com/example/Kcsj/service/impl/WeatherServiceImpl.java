package com.example.Kcsj.service.impl;

import com.example.Kcsj.dto.WeatherData;
import com.example.Kcsj.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Weather integration built on the Open-Meteo public API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private static final String API_TEMPLATE =
            "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=%s&longitude=%s"
                    + "&hourly=temperature_2m,relativehumidity_2m,precipitation_probability,precipitation,windspeed_10m,shortwave_radiation"
                    + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,sunshine_duration"
                    + "&current_weather=true&forecast_days=7&timezone=Asia/Shanghai";

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${weather.cache-ttl-minutes:30}")
    private long cacheTtlMinutes;

    @Value("${weather.default-latitude:30.67}")
    private double defaultLatitude;

    @Value("${weather.default-longitude:104.06}")
    private double defaultLongitude;

    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        // Preheat cache for default location
        try {
            refreshForecast(defaultLatitude, defaultLongitude);
        } catch (Exception ex) {
            log.warn("预热默认气象缓存失败: {}", ex.getMessage());
        }
    }

    @Override
    public WeatherData getWeatherSnapshot(double latitude, double longitude) {
        String key = cacheKey(latitude, longitude);
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getData();
        }
        return refreshForecast(latitude, longitude);
    }

    @Override
    public WeatherData refreshForecast(double latitude, double longitude) {
        WeatherData data = fetchFromApi(latitude, longitude);
        cache.put(cacheKey(latitude, longitude),
                new CacheEntry(data, Instant.now().plus(Duration.ofMinutes(cacheTtlMinutes))));
        return data;
    }

    @Override
    public WeatherData getDefaultWeatherSnapshot() {
        return getWeatherSnapshot(defaultLatitude, defaultLongitude);
    }

    @Scheduled(fixedDelayString = "${weather.refresh-interval-ms:3600000}")
    public void scheduledRefresh() {
        try {
            refreshForecast(defaultLatitude, defaultLongitude);
            log.debug("默认坐标气象数据已刷新。");
        } catch (Exception ex) {
            log.warn("默认坐标气象刷新失败: {}", ex.getMessage());
        }
    }

    private WeatherData fetchFromApi(double latitude, double longitude) {
        String url = String.format(Locale.ROOT, API_TEMPLATE, latitude, longitude);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("气象服务返回异常，HTTP状态：" + response.getStatusCode());
        }
        Map<String, Object> payload = response.getBody();
        return adaptPayload(latitude, longitude, payload);
    }

    @SuppressWarnings("unchecked")
    private WeatherData adaptPayload(double latitude, double longitude, Map<String, Object> payload) {
        Map<String, Object> currentWeather = (Map<String, Object>) payload.getOrDefault("current_weather", Collections.emptyMap());
        Map<String, Object> hourly = (Map<String, Object>) payload.getOrDefault("hourly", Collections.emptyMap());
        Map<String, Object> daily = (Map<String, Object>) payload.getOrDefault("daily", Collections.emptyMap());

        BigDecimal temperature = getBigDecimal(currentWeather.get("temperature"));
        BigDecimal windSpeed = getBigDecimal(currentWeather.get("windspeed"));

        List<BigDecimal> precipitationProbability = toBigDecimalList(hourly.get("precipitation_probability"));
        List<BigDecimal> precipitation = toBigDecimalList(hourly.get("precipitation"));
        List<BigDecimal> humidity = toBigDecimalList(hourly.get("relativehumidity_2m"));
        List<BigDecimal> radiation = toBigDecimalList(hourly.get("shortwave_radiation"));

        BigDecimal next24hProbability = averageFirstN(precipitationProbability, 24);
        BigDecimal next24hPrecipitation = sumFirstN(precipitation, 24);
        BigDecimal currentHumidity = averageFirstN(humidity, 3);
        BigDecimal currentRadiation = averageFirstN(radiation, 12);

        List<WeatherData.DailyForecast> forecasts = buildDailyForecasts(daily);

        return WeatherData.builder()
                .currentTemperature(temperature)
                .windSpeed(windSpeed)
                .currentHumidity(currentHumidity)
                .precipitationProbability(next24hProbability)
                .precipitationAmount(next24hPrecipitation)
                .solarRadiation(currentRadiation)
                .weatherSummary(buildSummary(payload, next24hProbability, next24hPrecipitation))
                .source("open-meteo.com")
                .fetchedAt(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("Asia/Shanghai")))
                .forecast(forecasts)
                .build();
    }

    private List<WeatherData.DailyForecast> buildDailyForecasts(Map<String, Object> daily) {
        List<String> days = (List<String>) daily.getOrDefault("time", Collections.emptyList());
        List<BigDecimal> tMax = toBigDecimalList(daily.get("temperature_2m_max"));
        List<BigDecimal> tMin = toBigDecimalList(daily.get("temperature_2m_min"));
        List<BigDecimal> precipitationSum = toBigDecimalList(daily.get("precipitation_sum"));
        List<BigDecimal> sunshine = toBigDecimalList(daily.get("sunshine_duration"));

        List<WeatherData.DailyForecast> result = new ArrayList<>();
        for (int i = 0; i < days.size(); i++) {
            LocalDate date = LocalDate.parse(days.get(i));
            result.add(WeatherData.DailyForecast.builder()
                    .date(date)
                    .temperatureMax(getSafe(tMax, i))
                    .temperatureMin(getSafe(tMin, i))
                    .precipitationSum(getSafe(precipitationSum, i))
                    .sunshineDuration(getSafe(sunshine, i))
                    .build());
        }
        return result;
    }

    private BigDecimal getSafe(List<BigDecimal> list, int index) {
        if (CollectionUtils.isEmpty(list) || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    private BigDecimal sumFirstN(List<BigDecimal> values, int n) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return values.stream()
                .limit(n)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal averageFirstN(List<BigDecimal> values, int n) {
        if (CollectionUtils.isEmpty(values) || values.stream().noneMatch(v -> v != null)) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (BigDecimal value : values) {
            if (value == null) {
                continue;
            }
            sum = sum.add(value);
            count++;
            if (count == n) {
                break;
            }
        }
        if (count == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> toBigDecimalList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> source = (List<?>) value;
        List<BigDecimal> target = new ArrayList<>(source.size());
        for (Object o : source) {
            target.add(getBigDecimal(o));
        }
        return target;
    }

    private BigDecimal getBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            log.debug("无法转换为BigDecimal: {}", value);
            return null;
        }
    }

    private String buildSummary(Map<String, Object> payload, BigDecimal probability, BigDecimal precipitation) {
        List<String> summary = new ArrayList<>();
        Optional.ofNullable(probability)
                .ifPresent(p -> summary.add("降雨概率 " + p.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "%"));
        Optional.ofNullable(precipitation)
                .ifPresent(p -> summary.add("预计降雨量 " + p.setScale(1, RoundingMode.HALF_UP) + " mm"));
        if (summary.isEmpty()) {
            summary.add("暂无显著降雨风险。");
        }
        return String.join("，", summary);
    }

    private String cacheKey(double latitude, double longitude) {
        return String.format(Locale.ROOT, "%.2f-%.2f", latitude, longitude);
    }

    private static class CacheEntry {
        private final WeatherData data;
        private final Instant expireAt;

        CacheEntry(WeatherData data, Instant expireAt) {
            this.data = data;
            this.expireAt = expireAt;
        }

        WeatherData getData() {
            return data;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }
    }
}
