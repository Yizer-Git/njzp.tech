package com.example.Kcsj.config;

import com.example.Kcsj.entity.SensorData;
import com.example.Kcsj.service.SensorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;

@Configuration
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class MqttConfig {

    private final SensorService sensorService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.broker.url}")
    private String brokerUrl;
    @Value("${mqtt.broker.username:}")
    private String username;
    @Value("${mqtt.broker.password:}")
    private String password;
    @Value("${mqtt.client.id:njzp-backend}")
    private String clientId;
    @Value("${mqtt.topic.sensor-upload}")
    private String sensorUploadTopic;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        if (StringUtils.hasText(username)) {
            options.setUserName(username);
        }
        if (StringUtils.hasText(password)) {
            options.setPassword(password.toCharArray());
        }
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "_inbound", mqttClientFactory(), sensorUploadTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttInputHandler() {
        return message -> {
            String payload = String.valueOf(message.getPayload());
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            log.debug("Received MQTT message from topic {}: {}", topic, payload);

            try {
                SensorData sensorData = objectMapper.readValue(payload, SensorData.class);

                if (StringUtils.hasText(topic)) {
                    String deviceId = topic.substring(topic.lastIndexOf('/') + 1);
                    sensorData.setDeviceId(deviceId);
                }

                if (sensorData.getTimestamp() == null) {
                    sensorData.setTimestamp(LocalDateTime.now());
                }
                if (sensorData.getCreatedAt() == null) {
                    sensorData.setCreatedAt(new Date());
                }

                sensorService.saveSensorData(sensorData);
            } catch (Exception ex) {
                log.error("Failed to process MQTT message", ex);
            }
        };
    }

    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutboundHandler() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId + "_outbound", mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }

    @MessagingGateway(defaultRequestChannel = "mqttOutputChannel")
    public interface MqttGateway {
        void sendToMqtt(String topic, String payload);
    }
}
