package com.aionemu.boot.lifecycle;

import org.springframework.boot.ApplicationArguments;

public interface AionServiceLifecycle {

    String getName();

    int getPhase();

    boolean isEnabled();

    void start(ApplicationArguments args) throws Exception;

    default void stop() throws Exception {
    }
}
