package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.stereotype.Component;

@Component
public class GameThreadPoolGateway {

    public void start() {
        ThreadPoolManager.getInstance();
    }

    public void stop() {
        ThreadPoolManager.getInstance().shutdown();
    }
}
