package com.kefir.logistics.launcher_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);           // Основное количество потоков
        executor.setMaxPoolSize(50);            // Максимальное количество потоков
        executor.setQueueCapacity(100);         // Размер очереди задач
        executor.setThreadNamePrefix("LauncherAsync-");
        executor.initialize();
        return executor;
    }
}