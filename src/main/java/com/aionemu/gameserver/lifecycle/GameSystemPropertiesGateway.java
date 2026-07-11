package com.aionemu.gameserver.lifecycle;

import org.springframework.stereotype.Component;

/**
 * 系统属性网关：设置文件编码与 IPv4 相关 JVM 属性。
 * System-properties gateway: sets file encoding and IPv4-related JVM properties.
 */
@Component
public class GameSystemPropertiesGateway {

    /**
     * 应用默认系统属性（UTF-8、优先 IPv4）。
     * Apply default system properties (UTF-8, prefer IPv4).
     */
    public void start() {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
    }
}
