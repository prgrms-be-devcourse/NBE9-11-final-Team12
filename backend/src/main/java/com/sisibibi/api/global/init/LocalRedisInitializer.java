//package com.sisibibi.api.global.init;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.context.annotation.Profile;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.data.redis.core.RedisCallback;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@Profile("local")
//@RequiredArgsConstructor
//@Order(Ordered.HIGHEST_PRECEDENCE)
//@ConditionalOnProperty(
//        prefix = "app.local.redis-reset-on-startup",
//        name = "enabled",
//        havingValue = "true"
//)
//public class LocalRedisInitializer implements ApplicationRunner {
//
//    private final StringRedisTemplate redisTemplate;
//
//    @Override
//    public void run(ApplicationArguments args) {
//        try {
//            redisTemplate.execute((RedisCallback<Void>) connection -> {
//                connection.serverCommands().flushDb();
//                return null;
//            });
//            log.info("Local Redis DB reset completed on application startup.");
//        } catch (RuntimeException redisException) {
//            log.warn("Failed to reset local Redis DB on application startup.", redisException);
//        }
//    }
//}
