package com.example.ticketing.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource masterDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    public DataSource replicaDataSource(
            @Value("${spring.datasource.replica-url:}") String replicaUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Qualifier("masterDataSource") DataSource masterDataSource) {
        if (replicaUrl == null || replicaUrl.isBlank()) {
            return masterDataSource;
        }
        return DataSourceBuilder.create()
                .url(replicaUrl)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource master,
            @Qualifier("replicaDataSource") DataSource replica) {
        ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put("master", master);
        targets.put("replica", replica);
        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(master);
        return routingDataSource;
    }
}
