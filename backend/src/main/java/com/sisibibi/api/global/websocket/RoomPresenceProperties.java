package com.sisibibi.api.global.websocket;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.websocket.presence")
public class RoomPresenceProperties {

    private boolean enabled = true;
    private Duration disconnectGracePeriod = Duration.ofSeconds(60);
    private int expirationBatchSize = 100;
}
