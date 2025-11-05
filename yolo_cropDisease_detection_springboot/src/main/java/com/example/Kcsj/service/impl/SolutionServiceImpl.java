package com.example.Kcsj.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.Kcsj.dto.SolutionOption;
import com.example.Kcsj.dto.SolutionRecommendation;
import com.example.Kcsj.dto.TaskCreationDTO;
import com.example.Kcsj.dto.WeatherData;
import com.example.Kcsj.entity.DiseaseInfo;
import com.example.Kcsj.entity.Remedy;
import com.example.Kcsj.entity.SensorData;
import com.example.Kcsj.entity.SolutionPlan;
import com.example.Kcsj.mapper.DiseaseInfoMapper;
import com.example.Kcsj.mapper.RemedyMapper;
import com.example.Kcsj.mapper.SensorDataMapper;
import com.example.Kcsj.mapper.SolutionPlanMapper;
import com.example.Kcsj.service.TaskService;
import com.example.Kcsj.service.SolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core solution generation logic that bridges disease knowledge and remedies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolutionServiceImpl implements SolutionService {

    private final DiseaseInfoMapper diseaseInfoMapper;
    private final RemedyMapper remedyMapper;
    private final SolutionPlanMapper solutionPlanMapper;
    private final SensorDataMapper sensorDataMapper;
    private final TaskService taskService;

    @Override
    @Transactional(readOnly = true)
    public SolutionRecommendation generateSolution(Long diseaseId, Long cropId, WeatherData weatherData) {
        DiseaseInfo diseaseInfo = Optional.ofNullable(diseaseInfoMapper.selectById(diseaseId))
                .orElseThrow(() -> new IllegalArgumentException("未找到病害知识库数据，diseaseId=" + diseaseId));

        SolutionPlan plan = findActivePlan(diseaseId, cropId);
        Remedy remedy = Optional.ofNullable(remedyMapper.selectById(plan.getRemedyId()))
                .orElseThrow(() -> new IllegalStateException("方案缺少药剂信息，remedyId=" + plan.getRemedyId()));

        BigDecimal computedCost = computeCost(plan, remedy);

        String weatherAdvisory = buildWeatherAdvisory(plan, weatherData);
        List<String> applicationNotes = splitToList(plan.getNotes());
        List<String> riskWarnings = buildRiskWarnings(diseaseInfo, remedy, weatherData);
        
        // 获取最新传感器数据
        SensorData sensorData = getLatestSensorData();
        
        // 计算施药时间窗口
        List<String> timeWindows = calculateApplicationTimeWindows(weatherData, sensorData, plan);
        
        // 构建气象限制详情
        Map<String, String> restrictions = buildApplicationRestrictions(weatherData, sensorData, plan);

        return SolutionRecommendation.builder()
                .diseaseId(diseaseInfo.getId())
                .diseaseName(diseaseInfo.getDiseaseName())
                .diseaseRiskLevel(diseaseInfo.getRiskLevel())
                .cropId(plan.getCropId())
                .cropName(Optional.ofNullable(plan.getCropName()).orElse(diseaseInfo.getCropName()))
                .remedyId(remedy.getId())
                .remedyName(remedy.getRemedyName())
                .activeIngredient(remedy.getActiveIngredient())
                .formulation(remedy.getFormulation())
                .intervalDays(remedy.getIntervalDays())
                .safetyIntervalDays(remedy.getSafetyIntervalDays())
                .maxApplicationsPerSeason(remedy.getMaxApplicationsPerSeason())
                .recommendedDosage(plan.getRecommendedDosage())
                .dosageUnit(plan.getDosageUnit())
                .computedCost(computedCost)
                .currency(remedy.getCurrency())
                .costBreakdown(buildCostBreakdown(plan, remedy, computedCost))
                .applicationStage(plan.getApplicationStage())
                .applicationTiming(plan.getApplicationTiming())
                .applicationNotes(applicationNotes)
                .riskWarnings(riskWarnings)
                .weatherAdvisory(weatherAdvisory)
                .weatherData(weatherData)
                .recommendedTimeWindows(timeWindows)
                .applicationRestrictions(restrictions)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private SolutionPlan findActivePlan(Long diseaseId, Long cropId) {
        LambdaQueryWrapper<SolutionPlan> wrapper = new LambdaQueryWrapper<SolutionPlan>()
                .eq(SolutionPlan::getDiseaseId, diseaseId)
                .eq(SolutionPlan::getStatus, "ACTIVE")
                .orderByDesc(SolutionPlan::getUpdatedAt)
                .last("LIMIT 1");
        if (cropId != null) {
            wrapper.eq(SolutionPlan::getCropId, cropId);
        }
        SolutionPlan plan = solutionPlanMapper.selectOne(wrapper);
        if (plan == null && cropId != null) {
            log.warn("未找到指定作物id={}的方案，退化为病害默认方案。", cropId);
            wrapper = new LambdaQueryWrapper<SolutionPlan>()
                    .eq(SolutionPlan::getDiseaseId, diseaseId)
                    .eq(SolutionPlan::getStatus, "ACTIVE")
                    .orderByDesc(SolutionPlan::getUpdatedAt)
                    .last("LIMIT 1");
            plan = Optional.ofNullable(solutionPlanMapper.selectOne(wrapper))
                    .orElseThrow(() -> new IllegalStateException("知识库中不存在可用的防治方案。"));
        } else if (plan == null) {
            throw new IllegalStateException("知识库中不存在可用的防治方案。");
        }
        return plan;
    }

    private BigDecimal computeCost(SolutionPlan plan, Remedy remedy) {
        if (plan.getExpectedCost() != null) {
            return plan.getExpectedCost().setScale(2, RoundingMode.HALF_UP);
        }
        if (remedy.getCostPerUnit() == null || remedy.getSafeDosage() == null) {
            return null;
        }
        // cost proportional to dosage compared with safe dosage reference
        BigDecimal ratio = plan.getRecommendedDosage()
                .divide(remedy.getSafeDosage(), 6, RoundingMode.HALF_UP);
        return remedy.getCostPerUnit()
                .multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String buildCostBreakdown(SolutionPlan plan, Remedy remedy, BigDecimal computedCost) {
        if (computedCost == null) {
            return "缺少完整的药剂成本信息，无法估算费用。";
        }
        return StrUtil.format("推荐剂量 {} {}，单价 {} {}/{}，估算成本约 {} {}。",
                plan.getRecommendedDosage().stripTrailingZeros().toPlainString(),
                plan.getDosageUnit(),
                remedy.getCostPerUnit().stripTrailingZeros().toPlainString(),
                remedy.getCurrency(),
                remedy.getDosageUnit(),
                computedCost.stripTrailingZeros().toPlainString(),
                remedy.getCurrency());
    }

    private List<String> splitToList(String notes) {
        if (StrUtil.isBlank(notes)) {
            return new ArrayList<>();
        }
        String sanitized = notes.replace("\r\n", "\n");
        List<String> segments = new ArrayList<>();
        Arrays.stream(sanitized.split("[\\n;；]"))
                .map(String::trim)
                .filter(StrUtil::isNotEmpty)
                .forEach(segments::add);
        return segments;
    }

    private List<String> buildRiskWarnings(DiseaseInfo diseaseInfo, Remedy remedy, WeatherData weatherData) {
        List<String> warnings = new ArrayList<>();
        if (StrUtil.isNotBlank(diseaseInfo.getRiskLevel())) {
            warnings.add("病害风险等级：" + diseaseInfo.getRiskLevel());
        }
        if (remedy.getSafetyIntervalDays() != null) {
            warnings.add("采收安全间隔期不少于 " + remedy.getSafetyIntervalDays() + " 天。");
        }
        if (remedy.getMaxApplicationsPerSeason() != null) {
            warnings.add("单季施用次数最多 " + remedy.getMaxApplicationsPerSeason() + " 次，注意轮换药剂。");
        }
        if (weatherData != null && weatherData.getPrecipitationProbability() != null) {
            BigDecimal prob = weatherData.getPrecipitationProbability().multiply(BigDecimal.valueOf(100));
            if (prob.compareTo(BigDecimal.valueOf(70)) > 0) {
                warnings.add("未来短期降雨概率高 (" + prob.setScale(0, RoundingMode.HALF_UP) + "%)，注意调整施药时间。");
            }
        }
        if (StrUtil.isNotBlank(remedy.getCaution())) {
            warnings.add(remedy.getCaution());
        }
        return warnings;
    }

    private String buildWeatherAdvisory(SolutionPlan plan, WeatherData weatherData) {
        if (weatherData == null) {
            return "缺少实时气象数据，建议在施药前自行确认未来天气。";
        }
        List<String> tips = new ArrayList<>();
        tips.add("气象来源：" + weatherData.getSource());
        if (weatherData.getCurrentTemperature() != null) {
            tips.add("当前气温 " + weatherData.getCurrentTemperature().setScale(1, RoundingMode.HALF_UP) + "℃。");
        }
        if (weatherData.getWindSpeed() != null) {
            tips.add("风速约 " + weatherData.getWindSpeed().setScale(1, RoundingMode.HALF_UP) + " m/s。");
        }
        if (weatherData.getPrecipitationProbability() != null) {
            tips.add("短时降雨概率 " + weatherData.getPrecipitationProbability()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP) + "%。");
        }
        if (weatherData.getPrecipitationAmount() != null) {
            tips.add("未来24h 累计降雨预计 " + weatherData.getPrecipitationAmount().setScale(1, RoundingMode.HALF_UP) + " mm。");
        }
        if (CollUtil.isNotEmpty(weatherData.getForecast())) {
            tips.add("已缓存未来 " + weatherData.getForecast().size() + " 天降雨和日照数据可用于作业排程。");
        }
        if (StrUtil.isNotBlank(plan.getWeatherConstraints())) {
            tips.add("方案限制：" + plan.getWeatherConstraints());
        }
        return String.join(" ", tips);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolutionOption> listCatalog() {
        List<SolutionPlan> plans = solutionPlanMapper.selectList(new LambdaQueryWrapper<SolutionPlan>()
                .eq(SolutionPlan::getStatus, "ACTIVE")
                .orderByAsc(SolutionPlan::getDiseaseId)
                .orderByAsc(SolutionPlan::getCropId));
        if (plans.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> diseaseIds = plans.stream()
                .map(SolutionPlan::getDiseaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> remedyIds = plans.stream()
                .map(SolutionPlan::getRemedyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, DiseaseInfo> diseaseMap = Collections.emptyMap();
        if (!diseaseIds.isEmpty()) {
            diseaseMap = diseaseInfoMapper.selectBatchIds(diseaseIds).stream()
                    .collect(Collectors.toMap(DiseaseInfo::getId, Function.identity()));
        }
        Map<Long, Remedy> remedyMap = Collections.emptyMap();
        if (!remedyIds.isEmpty()) {
            remedyMap = remedyMapper.selectBatchIds(remedyIds).stream()
                    .collect(Collectors.toMap(Remedy::getId, Function.identity()));
        }

        Map<Long, DiseaseInfo> finalDiseaseMap = diseaseMap;
        Map<Long, Remedy> finalRemedyMap = remedyMap;

        return plans.stream().map(plan -> {
            DiseaseInfo disease = finalDiseaseMap.get(plan.getDiseaseId());
            Remedy remedy = finalRemedyMap.get(plan.getRemedyId());
            return SolutionOption.builder()
                    .solutionId(plan.getId())
                    .diseaseId(plan.getDiseaseId())
                    .diseaseName(disease != null ? disease.getDiseaseName() : null)
                    .diseaseRiskLevel(disease != null ? disease.getRiskLevel() : null)
                    .cropId(plan.getCropId())
                    .cropName(plan.getCropName())
                    .remedyId(plan.getRemedyId())
                    .remedyName(remedy != null ? remedy.getRemedyName() : null)
                    .recommendedDosage(plan.getRecommendedDosage())
                    .dosageUnit(plan.getDosageUnit())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 获取最新的传感器数据
     */
    private SensorData getLatestSensorData() {
        try {
            LambdaQueryWrapper<SensorData> wrapper = new LambdaQueryWrapper<SensorData>()
                    .orderByDesc(SensorData::getTimestamp)
                    .last("LIMIT 1");
            return sensorDataMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.warn("获取传感器数据失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算施药时间窗口（未来48小时内的适宜时段）
     * 综合考虑：降雨、风速、温度、光照、露水等因素
     */
    private List<String> calculateApplicationTimeWindows(WeatherData weatherData, SensorData sensorData, SolutionPlan plan) {
        List<String> timeWindows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM月dd日");
        
        try {
            // 遍历未来48小时，每2小时检查一次
            for (int hourOffset = 0; hourOffset < 48; hourOffset += 2) {
                LocalDateTime checkTime = now.plusHours(hourOffset);
                int hour = checkTime.getHour();
                
                // 评估该时段是否适宜施药
                boolean isSuitable = evaluateTimeSlotSuitability(checkTime, hour, weatherData, sensorData, plan);
                
                if (isSuitable) {
                    // 生成时间窗口描述
                    String dateStr = hourOffset < 24 ? "今日" : (hourOffset < 48 ? "明日" : checkTime.format(dateFormatter));
                    String timeWindow = String.format("%s %02d:00-%02d:00", 
                            dateStr, 
                            hour, 
                            (hour + 2) % 24);
                    timeWindows.add(timeWindow);
                }
                
                // 限制返回最多6个时间窗口
                if (timeWindows.size() >= 6) {
                    break;
                }
            }
            
            if (timeWindows.isEmpty()) {
                timeWindows.add("近48小时内无适宜施药时段，请关注天气变化");
            }
            
        } catch (Exception e) {
            log.error("计算施药时间窗口失败", e);
            timeWindows.add("时间窗口计算失败，请根据实际天气情况判断");
        }
        
        return timeWindows;
    }

    /**
     * 评估某个时段是否适宜施药
     */
    private boolean evaluateTimeSlotSuitability(LocalDateTime checkTime, int hour, 
                                                 WeatherData weatherData, SensorData sensorData, SolutionPlan plan) {
        // 1. 避开露水时段（凌晨2点-早上7点）
        if (hour >= 2 && hour < 7) {
            return false;
        }
        
        // 2. 避开强光高温时段（中午11点-下午15点，且温度可能超过32℃）
        BigDecimal currentTemp = weatherData != null ? weatherData.getCurrentTemperature() : 
                                 (sensorData != null ? sensorData.getTemperature() : null);
        
        if (hour >= 11 && hour < 15) {
            if (currentTemp != null && currentTemp.compareTo(new BigDecimal("32")) > 0) {
                return false;
            }
            // 即使温度未知，中午12-14点也尽量避开
            if (hour >= 12 && hour < 14) {
                return false;
            }
        }
        
        // 3. 检查降雨概率（>70%时不适宜）
        if (weatherData != null && weatherData.getPrecipitationProbability() != null) {
            BigDecimal rainProb = weatherData.getPrecipitationProbability().multiply(new BigDecimal("100"));
            if (rainProb.compareTo(new BigDecimal("70")) > 0) {
                return false;
            }
        }
        
        // 4. 检查风速（>7 m/s时不适宜）
        if (weatherData != null && weatherData.getWindSpeed() != null) {
            if (weatherData.getWindSpeed().compareTo(new BigDecimal("7")) > 0) {
                return false;
            }
        }
        
        // 5. 检查光照强度（传感器数据）- 强光时段需要避开
        if (sensorData != null && sensorData.getLightIntensity() != null) {
            BigDecimal lightIntensity = sensorData.getLightIntensity();
            // 光照强度超过70000 lux时不适宜（强烈阳光）
            if (lightIntensity.compareTo(new BigDecimal("70000")) > 0) {
                return false;
            }
        }
        
        // 6. 推荐时段：早上7-10点，下午16-19点
        boolean isRecommendedTime = (hour >= 7 && hour < 10) || (hour >= 16 && hour < 19);
        
        return isRecommendedTime;
    }

    /**
     * 构建气象限制详情
     */
    private Map<String, String> buildApplicationRestrictions(WeatherData weatherData, 
                                                              SensorData sensorData, SolutionPlan plan) {
        Map<String, String> restrictions = new LinkedHashMap<>();
        
        try {
            // 1. 温度限制
            if (weatherData != null && weatherData.getCurrentTemperature() != null) {
                BigDecimal temp = weatherData.getCurrentTemperature();
                if (temp.compareTo(new BigDecimal("32")) > 0) {
                    restrictions.put("温度", String.format("当前气温 %.1f℃，超过32℃，应避开高温时段施药", temp));
                } else if (temp.compareTo(new BigDecimal("10")) < 0) {
                    restrictions.put("温度", String.format("当前气温 %.1f℃，低于10℃，药效可能受影响", temp));
                } else {
                    restrictions.put("温度", String.format("当前气温 %.1f℃，适宜施药", temp));
                }
            } else if (sensorData != null && sensorData.getTemperature() != null) {
                BigDecimal temp = sensorData.getTemperature();
                restrictions.put("温度", String.format("传感器温度 %.1f℃", temp));
            }
            
            // 2. 湿度提示
            if (weatherData != null && weatherData.getCurrentHumidity() != null) {
                BigDecimal humidity = weatherData.getCurrentHumidity();
                if (humidity.compareTo(new BigDecimal("85")) > 0) {
                    restrictions.put("湿度", String.format("当前湿度 %.0f%%，过高可能影响药效，建议通风后施药", humidity));
                } else {
                    restrictions.put("湿度", String.format("当前湿度 %.0f%%，适宜", humidity));
                }
            } else if (sensorData != null && sensorData.getHumidity() != null) {
                BigDecimal humidity = sensorData.getHumidity();
                restrictions.put("湿度", String.format("传感器湿度 %.0f%%", humidity));
            }
            
            // 3. 风速限制
            if (weatherData != null && weatherData.getWindSpeed() != null) {
                BigDecimal windSpeed = weatherData.getWindSpeed();
                if (windSpeed.compareTo(new BigDecimal("7")) > 0) {
                    restrictions.put("风速", String.format("当前风速 %.1f m/s，超过7 m/s，不宜喷施", windSpeed));
                } else if (windSpeed.compareTo(new BigDecimal("3")) < 0) {
                    restrictions.put("风速", String.format("当前风速 %.1f m/s，适宜", windSpeed));
                } else {
                    restrictions.put("风速", String.format("当前风速 %.1f m/s，可以施药", windSpeed));
                }
            }
            
            // 4. 降雨预警
            if (weatherData != null && weatherData.getPrecipitationProbability() != null) {
                BigDecimal rainProb = weatherData.getPrecipitationProbability().multiply(new BigDecimal("100"));
                if (rainProb.compareTo(new BigDecimal("70")) > 0) {
                    restrictions.put("降雨", String.format("未来降雨概率 %.0f%%，不宜施药", rainProb));
                } else if (rainProb.compareTo(new BigDecimal("30")) > 0) {
                    restrictions.put("降雨", String.format("未来降雨概率 %.0f%%，需关注天气变化", rainProb));
                } else {
                    restrictions.put("降雨", String.format("未来降雨概率 %.0f%%，适宜", rainProb));
                }
            }
            
            // 5. 光照提示
            if (sensorData != null && sensorData.getLightIntensity() != null) {
                BigDecimal light = sensorData.getLightIntensity();
                if (light.compareTo(new BigDecimal("70000")) > 0) {
                    restrictions.put("光照", "当前光照强烈，建议避开");
                } else if (light.compareTo(new BigDecimal("1000")) < 0) {
                    restrictions.put("光照", "当前光照较弱，适宜施药");
                } else {
                    restrictions.put("光照", "当前光照适中");
                }
            }
            
            // 6. 土壤墒情
            if (sensorData != null && sensorData.getSoilMoisture() != null) {
                BigDecimal soilMoisture = sensorData.getSoilMoisture();
                if (soilMoisture.compareTo(new BigDecimal("70")) > 0) {
                    restrictions.put("土壤墒情", String.format("土壤湿度 %.0f%%，过湿，注意排水", soilMoisture));
                } else if (soilMoisture.compareTo(new BigDecimal("30")) < 0) {
                    restrictions.put("土壤墒情", String.format("土壤湿度 %.0f%%，偏干，注意灌溉", soilMoisture));
                } else {
                    restrictions.put("土壤墒情", String.format("土壤湿度 %.0f%%，适宜", soilMoisture));
                }
            }
            
            // 7. 方案约束条件
            if (StrUtil.isNotBlank(plan.getWeatherConstraints())) {
                restrictions.put("方案约束", plan.getWeatherConstraints());
            }
            
            // 8. 推荐施药时段总结
            restrictions.put("推荐时段", "早上7-10点或傍晚16-19点，避开露水、高温、强光和大风");
            
        } catch (Exception e) {
            log.error("构建气象限制详情失败", e);
            restrictions.put("提示", "气象数据分析异常，请根据实际情况判断");
        }
        
        return restrictions;
    }

    @Override
    @Transactional
    public Long applySolution(Long solutionId, Long recordId, TaskCreationDTO payload) {
        if (payload == null) {
            throw new IllegalArgumentException("任务创建参数不能为空");
        }
        return taskService.createFromSolution(solutionId, recordId, payload);
    }
}

