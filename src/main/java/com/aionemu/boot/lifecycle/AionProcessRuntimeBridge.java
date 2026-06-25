package com.aionemu.boot.lifecycle;

import com.aionemu.commons.utils.AionProcessExit;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class AionProcessRuntimeBridge {

    public void exit(int status) {
        AionProcessExit.exit(status);
    }

    public void halt(int status) {
        AionProcessExit.halt(status);
    }
}
