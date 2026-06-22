package com.aionemu.commons.network;

public record NettyServerCfg(String hostName, int port, String connectionName, NettyConnectionFactory factory) {
}
