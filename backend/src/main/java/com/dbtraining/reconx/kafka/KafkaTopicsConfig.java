package com.dbtraining.reconx.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/**
 * KafkaTopicsConfig is a configuration class that defines Kafka topics for the application.
 * It creates topics for trade events, dead-letter queue (DLQ) for trade events, reconciliation results,
 * and system alerts. This configuration is only active in non-development and non-test profiles.
 */ 
@Configuration
@Profile("!dev & !test")
public class KafkaTopicsConfig {

    public static final String TRADE_EVENTS = "trade-events";
    public static final String TRADE_EVENTS_DLQ = "trade-events-dlq";
    public static final String RECON_RESULTS = "recon-results";
    public static final String SYSTEM_ALERTS = "system-alerts";

    @Bean
    public NewTopic tradeEvents() {
        return TopicBuilder.name(TRADE_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tradeEventsDlq() {
        return TopicBuilder.name(TRADE_EVENTS_DLQ)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reconResults() {
        return TopicBuilder.name(RECON_RESULTS)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic systemAlerts() {
        return TopicBuilder.name(SYSTEM_ALERTS)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
