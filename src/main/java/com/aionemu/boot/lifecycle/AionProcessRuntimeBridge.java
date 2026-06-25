package com.aionemu.boot.lifecycle;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class AionProcessRuntimeBridge {

    public void halt(int status) {
        Runtime.getRuntime().halt(status);
    }
}
