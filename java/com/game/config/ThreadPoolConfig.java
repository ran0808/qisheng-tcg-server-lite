package com.game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {
    @Bean("matchExecutor")
    public ExecutorService matchExecutor() {
        return new ThreadPoolExecutor(
                4, // 核心线程数
                16, // 最大线程数
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "match-thread-" + counter.getAndIncrement());
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    @Bean("battleExecutor")
    public ExecutorService battleExecutor() {
        int coreCount = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                coreCount,
                coreCount * 2,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(512),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "battle-thread-" + counter.getAndIncrement());
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
    @Bean("cardExecutor")
    public ExecutorService cardExecutor() {
        return new ThreadPoolExecutor(
                8,
                24,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "card-thread-" + counter.getAndIncrement());
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
