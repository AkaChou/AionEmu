package com.aionemu.boot.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 {@code aion.services} 前缀的服务开关与传输模式配置。
 * Boot configuration for service toggles and transport mode under {@code aion.services}.
 */
@Getter
@ConfigurationProperties(prefix = "aion.services")
public class AionServicesProperties {

    private final Service game = new Service(true);
    private final Service login = new Service(true);
    private final Service chat = new Service(false);
    private final Transport transport = new Transport();

    /**
     * 单个内嵌服务是否启用。
     * Whether a single embedded service is enabled.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Service {
        private boolean enabled;
    }

    /**
     * 网络传输模式配置（字符串绑定后规范化为枚举）。
     * Network transport mode configuration (normalizes a bound string into the enum).
     */
    @Getter
    public static class Transport {
        private String mode = TransportMode.NETTY.name();

        /**
         * 解析传输模式；空值回落为 {@link TransportMode#NETTY}。
         * Resolves the transport mode; blank values fall back to {@link TransportMode#NETTY}.
         *
         * @return 传输模式枚举 / transport mode enum
         */
        public TransportMode getMode() {
            if (mode == null || mode.isBlank()) {
                return TransportMode.NETTY;
            }
            return TransportMode.valueOf(mode.toUpperCase().replace('-', '_'));
        }

        /**
         * 设置原始传输模式字符串（由配置绑定写入）。
         * Sets the raw transport mode string (written by configuration binding).
         *
         * @param mode 模式字符串 / mode string
         */
        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    /**
     * 支持的网络传输实现。
     * Supported network transport implementations.
     */
    public enum TransportMode {
        NETTY
    }
}
