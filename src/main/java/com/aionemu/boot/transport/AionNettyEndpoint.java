package com.aionemu.boot.transport;

import java.net.InetSocketAddress;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Netty 监听端点描述（名称、主机、端口）。
 * Netty listen endpoint descriptor (name, host, port).
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class AionNettyEndpoint {

    /**
     * 端点逻辑名称。
     * Logical endpoint name.
     */
    private final String name;

    /**
     * 绑定主机；{@code "*"} 表示任意地址。
     * Bind host; {@code "*"} means any local address.
     */
    private final String host;

    /**
     * 绑定端口。
     * Bind port.
     */
    private final int port;

    /**
     * 创建端点描述。
     * Create an endpoint descriptor.
     *
     * @param name 逻辑名称 / Logical name
     * @param host 绑定主机，{@code "*"} 表示任意地址 / Bind host, {@code "*"} for any address
     * @param port 绑定端口 / Bind port
     * @return 端点描述实例 / Endpoint instance
     */
    public static AionNettyEndpoint of(String name, String host, int port) {
        return new AionNettyEndpoint(name, host, port);
    }

    /**
     * 解析为 {@link InetSocketAddress}；主机为 {@code "*"} 时仅绑定端口。
     * Resolve to {@link InetSocketAddress}; when host is {@code "*"}, bind port only.
     *
     * @return 套接字地址 / Socket address
     */
    public InetSocketAddress getAddress() {
        if ("*".equals(host)) {
            return new InetSocketAddress(port);
        }
        return new InetSocketAddress(host, port);
    }
}
