package com.aionemu.commons.network;

/**
 * Netty 服务端绑定配置。
 * Netty server bind configuration.
 * 监听主机（"*" 表示所有地址） / Bind host ("*" for all addresses)
 * 监听端口 / Bind port
 * 连接名称（日志用） / Connection name for logging
 * 连接工厂 / Connection factory
 */
public record NettyServerCfg(String hostName, int port, String connectionName, NettyConnectionFactory factory) {
}
