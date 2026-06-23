package com.aionemu.commons.network;

/**
 * 服务器配置类,用于存储服务器的基本配置信息
 * Server configuration class for storing basic server configuration information
 *
 * 该类包含以下配置:
 * This class contains the following configurations:
 * 1. 服务器主机名 / Server hostname
 * 2. 服务器端口 / Server port
 * 3. 连接名称 / Connection name
 * 4. 连接工厂 / Connection factory
 */
public class ServerCfg {
    
    /**
     * 服务器主机名
     * Server hostname
     */
    public final String hostName;
    
    /**
     * 服务器端口
     * Server port
     */
    public final int port;
    
    /**
     * 连接名称
     * Connection name
     */
    public final String connectionName;
    
    /**
     * 连接工厂
     * Connection factory
     */
    public final ConnectionFactory factory;

    /**
     * 构造函数
     * Constructor
     *
     * @param hostName 服务器主机名 / Server hostname
     * @param port 服务器端口 / Server port
     * @param connectionName 连接名称 / Connection name
     * @param factory 连接工厂 / Connection factory
     */
    public ServerCfg(String hostName, int port, String connectionName, ConnectionFactory factory) {
        this.hostName = hostName;
        this.port = port;
        this.connectionName = connectionName;
        this.factory = factory;
    }
}
