//package com.sisibibi.api.global.init;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.context.annotation.Profile;
//import org.springframework.data.redis.connection.RedisConnection;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@Profile("local")
//@RequiredArgsConstructor
//public class LocalRedisInitializer implements ApplicationRunner {
//
//    private final StringRedisTemplate redisTemplate;
//
//    @Override
//    public void run(ApplicationArguments args) {
//        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
//        if (connectionFactory == null) {
//            log.warn("Local Redis flush skipped because RedisConnectionFactory is missing.");
//            return;
//        }
//
//        try (RedisConnection connection = connectionFactory.getConnection()) {
//            connection.serverCommands().flushDb();
//            log.info("Local Redis DB flushed.");
//        }
//    }
//}
