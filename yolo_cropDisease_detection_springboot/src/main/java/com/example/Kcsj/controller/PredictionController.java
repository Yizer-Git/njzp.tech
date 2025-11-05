package com.example.Kcsj.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.SolutionRecommendation;
import com.example.Kcsj.dto.WeatherData;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.mapper.ImgRecordsMapper;
import com.example.Kcsj.service.DiseaseMapperService;
import com.example.Kcsj.service.SolutionService;
import com.example.Kcsj.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/flask")
@Slf4j
public class PredictionController {
    @Resource
    ImgRecordsMapper imgRecordsMapper;
    
    @Resource
    DiseaseMapperService diseaseMapperService;
    
    @Resource
    SolutionService solutionService;
    
    @Resource
    WeatherService weatherService;

    private final RestTemplate restTemplate = new RestTemplate();

    // 定义接收的参数类
    public static class PredictRequest {
        private String startTime;
        private String weight;
        private String username;
        private String inputImg;
        private String kind;
        private String conf;
        private Integer taskId;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getWeight() {
            return weight;
        }

        public void setWeight(String weight) {
            this.weight = weight;
        }

        public String getInputImg() {
            return inputImg;
        }

        public void setInputImg(String inputImg) {
            this.inputImg = inputImg;
        }

        public String getConf() {
            return conf;
        }

        public void setConf(String conf) {
            this.conf = conf;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public Integer getTaskId() {
            return taskId;
        }

        public void setTaskId(Integer taskId) {
            this.taskId = taskId;
        }
    }

    @PostMapping("/predict")
    public Result<?> predict(@RequestBody PredictRequest request) {
        if (request == null || request.getInputImg() == null || request.getInputImg().isEmpty()) {
            return Result.error("-1", "未提供图片链接");
        } else if (request.getWeight() == null || request.getWeight().isEmpty()) {
            return Result.error("-1", "未提供权重");
        }

        try {
            // 创建请求体
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PredictRequest> requestEntity = new HttpEntity<>(request, headers);

            // 调用 Flask API (端口5001)
            String response = restTemplate.postForObject("http://localhost:5001/predictImg", requestEntity, String.class);
            System.out.println("Received response: " + response);
            JSONObject responses = JSONObject.parseObject(response);
            if(responses.get("status").equals(400)){
                return Result.error("-1", "Error: " + responses.get("message"));
            }else {
                // 1. 保存识别记录
                ImgRecords imgRecords = new ImgRecords();
                imgRecords.setWeight(request.getWeight());
                imgRecords.setConf(request.getConf());
                imgRecords.setKind(request.getKind());
                imgRecords.setInputImg(request.getInputImg());
                imgRecords.setUsername(request.getUsername());
                imgRecords.setStartTime(request.getStartTime());
                imgRecords.setLable(String.valueOf(responses.get("label")));
                imgRecords.setConfidence(String.valueOf(responses.get("confidence")));
                imgRecords.setAllTime(String.valueOf(responses.get("allTime")));
                imgRecords.setOutImg(String.valueOf(responses.get("outImg")));
                imgRecords.setTaskId(request.getTaskId());
                imgRecordsMapper.insert(imgRecords);
                responses.put("taskId", request.getTaskId());
                
                // 2. 图像识别与决策联动：生成防治方案
                try {
                    // 2.1 提取第一个识别到的病害标签
                    String labelStr = String.valueOf(responses.get("label"));
                    if (labelStr != null && !labelStr.equals("null") && !labelStr.isEmpty()) {
                        // 解析标签（可能是 JSON 数组格式："[\"blight（疫病）\"]"）
                        String firstLabel = extractFirstLabel(labelStr);
                        
                        if (firstLabel != null && !firstLabel.isEmpty()) {
                            log.info("检测到病害标签：{}", firstLabel);
                            
                            // 2.2 将 Flask 病害标签映射为 diseaseId
                            Long diseaseId = diseaseMapperService.mapFlaskLabelToDiseaseId(firstLabel, request.getKind());
                            
                            if (diseaseId != null) {
                                log.info("映射到病害ID：{}", diseaseId);
                                
                                // 2.3 获取作物ID（从kind推断）
                                Long cropId = getCropIdFromKind(request.getKind());
                                
                                // 2.4 获取气象数据（使用缓存数据）
                                WeatherData weatherData = weatherService.getDefaultWeatherSnapshot();
                                
                                // 2.5 生成防治方案
                                SolutionRecommendation solution = solutionService.generateSolution(diseaseId, cropId, weatherData);
                                
                                // 2.6 将防治方案添加到响应中
                                responses.put("solutionRecommendation", solution);
                                responses.put("timeWindows", solution.getRecommendedTimeWindows());
                                
                                log.info("成功生成防治方案，病害：{}", solution.getDiseaseName());
                            } else {
                                log.warn("未能映射病害标签：{}", firstLabel);
                                responses.put("solutionRecommendation", null);
                                responses.put("solutionMessage", "未找到对应的防治方案");
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("生成防治方案失败", e);
                    // 即使方案生成失败，仍返回识别结果
                    responses.put("solutionRecommendation", null);
                    responses.put("solutionMessage", "方案生成失败：" + e.getMessage());
                }
                
                return Result.success(responses);
            }
        } catch (Exception e) {
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }

    @GetMapping("/file_names")
    public Result<?> getFileNames() {
        try {
            // 调用 Flask API (端口5001)
            String response = restTemplate.getForObject("http://127.0.0.1:5001/file_names", String.class);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }

    /**
     * 从标签字符串中提取第一个病害标签
     * 输入示例：["blight（疫病）"] 或 ["blight（疫病）", "rust（锈病）"]
     */
    private String extractFirstLabel(String labelStr) {
        try {
            // 去除首尾的方括号和引号
            if (labelStr.startsWith("[") && labelStr.endsWith("]")) {
                labelStr = labelStr.substring(1, labelStr.length() - 1);
            }
            
            // 处理可能的 JSON 数组格式
            if (labelStr.contains(",")) {
                String[] labels = labelStr.split(",");
                labelStr = labels[0].trim();
            }
            
            // 去除引号和转义字符
            labelStr = labelStr.replace("\"", "").replace("\\", "").trim();
            
            return labelStr;
        } catch (Exception e) {
            log.error("解析病害标签失败：{}", labelStr, e);
            return null;
        }
    }

    /**
     * 根据 kind (作物类型) 获取作物ID
     * kind 可能的值：corn, rice, tomato, strawberry
     */
    private Long getCropIdFromKind(String kind) {
        if (kind == null) {
            return null;
        }
        
        switch (kind.toLowerCase()) {
            case "rice":
                return 1L;  // 水稻
            case "corn":
                return 2L;  // 玉米
            case "wheat":
                return 3L;  // 小麦
            case "tomato":
                return 4L;  // 番茄
            case "strawberry":
                return 5L;  // 草莓
            default:
                log.warn("未知的作物类型：{}", kind);
                return null;
        }
    }
}
