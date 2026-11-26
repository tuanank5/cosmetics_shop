package com.beautyshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
class XacThuc_Config {
    @Bean
    TaskExecutor passwordResetExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("password-reset-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(64);
        return executor;
    }
}
