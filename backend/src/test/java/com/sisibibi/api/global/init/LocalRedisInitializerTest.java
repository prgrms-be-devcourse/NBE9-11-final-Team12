package com.sisibibi.api.global.init;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalRedisInitializerTest {

    @Test
    void run_flushesCurrentRedisDatabase() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnection connection = mock(RedisConnection.class);
        RedisServerCommands serverCommands = mock(RedisServerCommands.class);
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            return callback.doInRedis(connection);
        });
        LocalRedisInitializer initializer = new LocalRedisInitializer(redisTemplate);

        initializer.run(new DefaultApplicationArguments());

        verify(serverCommands).flushDb();
    }
}
