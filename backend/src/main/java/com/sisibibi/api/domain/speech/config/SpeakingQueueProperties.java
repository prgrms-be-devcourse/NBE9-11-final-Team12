package com.sisibibi.api.domain.speech.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.speaking")
public class SpeakingQueueProperties {

    private Duration turnDuration = Duration.ofMinutes(3);
    private Queue queue = new Queue();
    private Idle idle = new Idle();

    @Getter
    @Setter
    public static class Queue {

        private int summarySize = 5;
        private int defaultPageSize = 20;
        private int maxPageSize = 100;
    }

    @Getter
    @Setter
    public static class Idle {

        private Duration warningDelay = Duration.ofSeconds(20);
        private Duration timeoutDelayAfterWarning = Duration.ofSeconds(20);
        private Duration warningSuppressionBeforeExpiration = Duration.ofSeconds(40);
    }
}
