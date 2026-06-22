package com.sisibibi.api.global.realtime;

public interface RealtimeEventPublisher {

    void publish(String destination, Object event);
}
