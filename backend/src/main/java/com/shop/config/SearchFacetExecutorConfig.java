package com.shop.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 검색 facet용 병렬 DB 호출 전용 스레드 풀. @Async 기본 풀과 분리한다.
 */
@Configuration
public class SearchFacetExecutorConfig {

    public static final String SEARCH_FACET_EXECUTOR = "searchFacetExecutor";

    @Bean(name = SEARCH_FACET_EXECUTOR)
    public Executor searchFacetExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("search-facet-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.initialize();
        return ex;
    }
}
