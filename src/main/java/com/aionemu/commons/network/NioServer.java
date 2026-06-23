/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 *
 * You should have received a copy of the GNU General Public License along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Credits goes to all Open Source Core Developer Groups listed below Please do not change here something, ragarding the developer credits, except the
 * "developed by XXXX". Even if you edit a lot of files in this source, you still have no rights to call it as "your Core". Everybody knows that this
 * Emulator Core was developed by Aion Lightning
 * 
 * @-Aion-Unique-
 * @-Aion-Lightning
 * @Aion-Engine
 * @Aion-Extreme
 * @Aion-NextGen
 * @Aion-Core Dev.
 */
package com.aionemu.commons.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.options.Assertion;

/**
 * NIO服务器类
 * NIO Server Class
 *
 * 该类负责管理网络连接，包括接受新连接、处理读写操作和管理连接的生命周期
 * This class is responsible for managing network connections, including accepting new connections,
 * handling read/write operations, and managing connection lifecycles
 */
public class NioServer implements ServerTransport {
    
    /**
     * 日志记录器
     * Logger for NioServer
     */
    private static final Logger log = LoggerFactory.getLogger(NioServer.class);
    
    /**
     * 服务器通道的选择键列表
     * List of selection keys for server channels
     */
    private final List<SelectionKey> serverChannelKeys = new ArrayList<>();
    
    /**
     * 接受连接的调度器
     * Dispatcher for accepting connections
     */
    private Dispatcher acceptDispatcher;
    
    /**
     * 读写调度器的负载均衡计数器
     * Load balancing counter for read/write dispatchers
     */
    private int currentReadWriteDispatcher;
    
    /**
     * 读写操作的调度器数组
     * Array of dispatchers for read/write operations
     */
    private Dispatcher[] readWriteDispatchers;
    
    /**
     * 断开连接任务的线程池
     * Thread pool for disconnection tasks
     */
    private final Executor dcPool;
    
    /**
     * 读写线程数量
     * Number of read/write threads
     */
    private int readWriteThreads;
    
    /**
     * 服务器配置数组
     * Array of server configurations
     */
    private ServerCfg[] cfgs;

    /**
     * 构造函数
     * Constructor
     *
     * @param readWriteThreads 用于处理读写操作的线程数 / Number of threads for handling read/write operations
     * @param cfgs 服务器配置数组 / Array of server configurations
     */
    public NioServer(int readWriteThreads, ServerCfg... cfgs) {
        if (Assertion.NetworkAssertion) {
            boolean assertionEnabled = false;
            assert assertionEnabled = true;
            if (!assertionEnabled) {
                throw new RuntimeException("This is unstable build. Assertion must be enabled! Add -ea to your start script or consider using stable build instead.");
            }
        }
        this.dcPool = ThreadPoolManager.getInstance();
        this.readWriteThreads = readWriteThreads;
        this.cfgs = cfgs;
    }

    /**
     * 连接并启动服务器
     * Connect and start the server
     */
    @Override
    public void connect() {
        try {
            initDispatchers(readWriteThreads, dcPool);
            
            for (ServerCfg cfg : cfgs) {
                ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.configureBlocking(false);
                
                InetSocketAddress isa;
                if ("*".equals(cfg.hostName)) {
                    isa = new InetSocketAddress(cfg.port);
                    log.info("Server listening on all available IPs on Port {} for {}", cfg.port, cfg.connectionName);
                } else {
                    isa = new InetSocketAddress(cfg.hostName, cfg.port);
                    log.info("Server listening on IP: {} Port {} for {}", cfg.hostName, cfg.port, cfg.connectionName);
                }
                serverChannel.socket().bind(isa);
                
                SelectionKey acceptKey = getAcceptDispatcher().register(
                    serverChannel, 
                    SelectionKey.OP_ACCEPT, 
                    new Acceptor(cfg.factory, this)
                );
                serverChannelKeys.add(acceptKey);
            }
        } catch (Exception e) {
            log.error("NioServer Initialization Error: ", e);
            try {
                shutdown();
            } catch (Exception shutdownError) {
                log.warn("Failed to clean up NioServer after initialization error.", shutdownError);
            }
            throw new Error("NioServer Initialization Error!", e);
        }
    }

    /**
     * 获取接受连接的调度器
     * Get the dispatcher for accepting connections
     */
    public final Dispatcher getAcceptDispatcher() {
        return acceptDispatcher;
    }

    /**
     * 获取读写操作的调度器
     * Get a dispatcher for read/write operations
     */
    public final Dispatcher getReadWriteDispatcher() {
        if (readWriteDispatchers == null) {
            return acceptDispatcher;
        }
        
        if (readWriteDispatchers.length == 1) {
            return readWriteDispatchers[0];
        }
        
        if (currentReadWriteDispatcher >= readWriteDispatchers.length) {
            currentReadWriteDispatcher = 0;
        }
        return readWriteDispatchers[currentReadWriteDispatcher++];
    }

    /**
     * 初始化调度器
     * Initialize dispatchers
     */
    private void initDispatchers(int readWriteThreads, Executor dcPool) throws IOException {
        if (readWriteThreads < 1) {
            acceptDispatcher = new AcceptReadWriteDispatcherImpl("AcceptReadWrite Dispatcher", dcPool);
            acceptDispatcher.start();
        } else {
            acceptDispatcher = new AcceptDispatcherImpl("Accept Dispatcher");
            acceptDispatcher.start();
            
            readWriteDispatchers = new Dispatcher[readWriteThreads];
            for (int i = 0; i < readWriteDispatchers.length; i++) {
                readWriteDispatchers[i] = new AcceptReadWriteDispatcherImpl(
                    "ReadWrite-" + i + " Dispatcher", 
                    dcPool
                );
                readWriteDispatchers[i].start();
            }
        }
    }

    /**
     * 获取活动连接数
     * Get the number of active connections
     */
    @Override
    public final int getActiveConnections() {
        int count = 0;
        if (readWriteDispatchers != null) {
            for (Dispatcher d : readWriteDispatchers) {
                count += activeConnections(d);
            }
        } else {
            count = activeConnections(acceptDispatcher);
        }
        return count;
    }

    /**
     * 关闭服务器
     * Shutdown the server
     */
    @Override
    public final void shutdown() {
        log.info("Closing ServerChannels...");
        closeServerChannels();
        notifyServerClose();
        sleepDuringShutdown();

        log.info("Active connections: {}", getActiveConnections());
        log.info("Forced Disconnecting all connections...");
        closeAll();
        sleepDuringShutdown();
        closeDispatcherChannels();
        log.info("Active connections: {}", getActiveConnections());

        shutdownDispatchers();
    }

    private int activeConnections(Dispatcher dispatcher) {
        if (dispatcher == null) {
            return 0;
        }
        try {
            return (int) dispatcher.selector().keys().stream()
                .filter(key -> key.attachment() instanceof AConnection)
                .count();
        } catch (ClosedSelectorException e) {
            return 0;
        }
    }

    private void closeServerChannels() {
        for (SelectionKey key : new ArrayList<>(serverChannelKeys)) {
            closeKeyChannel(key);
        }
        serverChannelKeys.clear();
        log.info("ServerChannels closed.");
    }

    private void sleepDuringShutdown() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.warn("Nio thread was interrupted during shutdown", e);
            Thread.currentThread().interrupt();
        }
    }

    private void closeDispatcherChannels() {
        if (readWriteDispatchers != null) {
            for (Dispatcher d : readWriteDispatchers) {
                closeDispatcherChannels(d);
            }
        } else {
            closeDispatcherChannels(acceptDispatcher);
        }
    }

    private void closeDispatcherChannels(Dispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        try {
            for (SelectionKey key : new ArrayList<>(dispatcher.selector().keys())) {
                closeKeyChannel(key);
            }
        } catch (ClosedSelectorException ignored) {
        }
    }

    private void closeKeyChannel(SelectionKey key) {
        try {
            Channel channel = key.channel();
            key.cancel();
            if (channel != null) {
                channel.close();
            }
        } catch (Exception e) {
            log.warn("Error closing NIO channel", e);
        }
    }

    private void shutdownDispatchers() {
        if (readWriteDispatchers != null) {
            for (Dispatcher dispatcher : readWriteDispatchers) {
                shutdownDispatcher(dispatcher);
            }
        }
        shutdownDispatcher(acceptDispatcher);
        readWriteDispatchers = null;
        acceptDispatcher = null;
    }

    private void shutdownDispatcher(Dispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        dispatcher.shutdown();
        try {
            dispatcher.join(1000);
        } catch (InterruptedException e) {
            log.warn("Interrupted waiting for dispatcher shutdown", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 通知所有活动连接服务器即将关闭
     * Notify all active connections that the server is closing
     */
    private void notifyServerClose() {
        if (readWriteDispatchers != null) {
            for (Dispatcher d : readWriteDispatchers) {
                notifyServerClose(d);
            }
        } else {
            notifyServerClose(acceptDispatcher);
        }
    }

    private void notifyServerClose(Dispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        try {
            dispatcher.selector().keys().stream()
                .filter(key -> key.attachment() instanceof AConnection)
                .forEach(key -> ((AConnection) key.attachment()).onServerClose());
        } catch (ClosedSelectorException ignored) {
        }
    }

    /**
     * 关闭所有活动连接
     * Close all active connections
     */
    private void closeAll() {
        if (readWriteDispatchers != null) {
            for (Dispatcher d : readWriteDispatchers) {
                closeAll(d);
            }
        } else {
            closeAll(acceptDispatcher);
        }
    }

    private void closeAll(Dispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        try {
            dispatcher.selector().keys().stream()
                .filter(key -> key.attachment() instanceof AConnection)
                .forEach(key -> ((AConnection) key.attachment()).close(true));
        } catch (ClosedSelectorException ignored) {
        }
    }
}
