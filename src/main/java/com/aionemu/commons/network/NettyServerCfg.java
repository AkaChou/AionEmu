package com.aionemu.commons.network;

/**
 * Netty 服务端绑定配置。
 * Netty server bind configuration.
 *
 * @param hostName 监听主机（"*" 表示所有地址） / Bind host ("*" for all addresses)
 * @param port 监听端口 / Bind port
 * @param connectionName 连接名称（日志用） / Connection name for logging
 * @param factory 连接工厂 / Connection factory
 */
public record NettyServerCfg(String hostName, int port, String connectionName, NettyConnectionFactory factory) {
}
