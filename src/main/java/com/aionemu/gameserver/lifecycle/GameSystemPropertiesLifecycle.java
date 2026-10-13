package com.aionemu.gameserver.lifecycle;

import org.springframework.stereotype.Component;

/**
 * 系统属性生命周期：在启动序列中应用系统属性（UTF-8、优先 IPv4），加载状态由基类承载。
 * System-properties lifecycle: applies system properties (UTF-8, prefer IPv4) during startup;
 * load state is hosted by the base class.
 */
@Component
public class GameSystemPropertiesLifecycle extends GameStartupStage {

    /**
     * 启动本阶段：应用默认系统属性。
     * Start this stage: apply default system properties.
     */
    @Override
    protected void runStage() {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
    }
}
