package com.aionemu.boot.lifecycle;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class AionProcessRuntimeBridge {

    public void exit(int status) {
        System.exit(status);
    }

    public void halt(int status) {
        Runtime.getRuntime().halt(status);
    }
}
