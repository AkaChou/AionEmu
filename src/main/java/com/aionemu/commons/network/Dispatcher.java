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
 */
package com.aionemu.commons.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.options.Assertion;

/**
 * 网络事件调度器,负责分发和处理Selector选择的SelectionKey集合
 * Network event dispatcher that dispatches and processes SelectionKey sets selected by Selector
 *
 * 该类实现了基于NIO的网络事件处理机制:
 * This class implements NIO-based network event handling mechanism:
 * 1. 使用Selector监听网络事件 / Uses Selector to monitor network events
 * 2. 分发读写事件到对应的处理方法 / Dispatches read/write events to corresponding handlers 
 * 3. 管理连接的生命周期 / Manages connection lifecycle
 * 4. 处理连接的关闭 / Handles connection closure
 *
 * @author -Nemesiss-
 */
public abstract class Dispatcher extends Thread {
    
    /**
     * 日志记录器
     * Logger for Dispatcher
     */
    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);
    
    /**
     * 用于选择就绪键的选择器
     * Selector for selecting ready keys
     */
    protected Selector selector;
    
    /**
     * 用于执行断开连接任务的执行器
     * Executor for executing disconnection tasks
     */
    private final Executor dcPool;
    
    /**
     * 用于同步register和selector.select操作的对象
     * Object for synchronizing register and selector.select operations
     */
    private final Object gate = new Object();
    
    /**
     * 构造函数
     * Constructor
     *
     * @param name 线程名称 / Thread name
     * @param dcPool 断开连接任务执行器 / Disconnection task executor
     * @throws IOException 如果选择器创建失败 / If selector creation fails
     */
    public Dispatcher(String name, Executor dcPool) throws IOException {
        super(name);
        this.selector = SelectorProvider.provider().openSelector();
        this.dcPool = dcPool;
    }
    
    /**
     * 将连接添加到待关闭列表,该连接将尽快被此调度器关闭
     * Add connection to pending close list, connection will be closed by this dispatcher as soon as possible
     *
     * @param con 要关闭的连接 / Connection to close
     */
    abstract void closeConnection(AConnection con);
    
    /**
     * 分发选定的键并处理待关闭的连接
     * Dispatch selected keys and process pending closes
     *
     * @throws IOException 如果发生I/O错误 / If an I/O error occurs
     */
    abstract void dispatch() throws IOException;
    
    /**
     * 获取此调度器的选择器
     * Get selector of this dispatcher
     *
     * @return 选择器实例 / Selector instance
     */
    public final Selector selector() {
        return this.selector;
    }
    
    /**
     * 调度器主循环,分发选定的键并处理待关闭的连接
     * Main dispatcher loop that dispatches selected keys and processes pending closes
     */
    @Override
    public void run() {
        for (;;) {
            try {
                dispatch();
                
                synchronized (gate) {}
            } catch (Exception e) {
                log.error("Dispatcher error! " + e, e);
            }
        }
    }
    
    /**
     * 注册新客户端连接到此调度器,并将注册结果(SelectionKey)设置为给定AConnection的键
     * Register new client connection to this dispatcher and set registration result (SelectionKey) as key of given AConnection
     *
     * @param ch 可选择通道 / Selectable channel
     * @param ops 操作集 / Operation set
     * @param att 附件对象 / Attachment object
     * @throws IOException 如果注册失败 / If registration fails
     */
    public final void register(SelectableChannel ch, int ops, AConnection att) throws IOException {
        synchronized (gate) {
            selector.wakeup();
            att.setKey(ch.register(selector, ops, att));
        }
    }
    
    /**
     * 注册新的接收器到此调度器并返回注册结果(SelectionKey)
     * Register new acceptor to this dispatcher and return registration result (SelectionKey)
     *
     * @param ch 可选择通道 / Selectable channel
     * @param ops 操作集 / Operation set
     * @param att 附件对象 / Attachment object
     * @return 代表此注册的SelectionKey / SelectionKey representing this registration
     * @throws IOException 如果注册失败 / If registration fails
     */
    public final SelectionKey register(SelectableChannel ch, int ops, Acceptor att) throws IOException {
        synchronized (gate) {
            selector.wakeup();
            return ch.register(selector, ops, att);
        }
    }
    
    /**
     * 接受新连接
     * Accept new connection
     *
     * @param key 选择键 / Selection key
     */
    final void accept(SelectionKey key) {
        try {
            ((Acceptor) key.attachment()).accept(key);
        } catch (Exception e) {
            log.error("Error while accepting connection: " + e, e);
        }
    }
    
    /**
     * 从SelectionKey表示的socketChannel读取数据,解析并处理数据,为下次读取准备缓冲区
     * Read data from socketChannel represented by SelectionKey, parse and process data, prepare buffer for next read
     *
     * @param key 选择键 / Selection key
     */
    final void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        AConnection con = (AConnection) key.attachment();
        
        ByteBuffer rb = con.readBuffer;
        
        if (Assertion.NetworkAssertion) {
            assert con.readBuffer.hasRemaining();
        }
        
        int numRead;
        try {
            numRead = socketChannel.read(rb);
        } catch (IOException e) {
            closeConnectionImpl(con);
            return;
        }
        
        if (numRead == -1) {
            closeConnectionImpl(con);
            return;
        } else if (numRead == 0) {
            return;
        }
        
        rb.flip();
        while (rb.remaining() > 2 && rb.remaining() >= rb.getShort(rb.position())) {
            if (!parse(con, rb)) {
                closeConnectionImpl(con);
                return;
            }
        }
        
        if (rb.hasRemaining()) {
            con.readBuffer.compact();
            
            if (Assertion.NetworkAssertion) {
                assert con.readBuffer.hasRemaining();
            }
        } else {
            rb.clear();
        }
    }
    
    /**
     * 从缓冲区解析数据并准备缓冲区以读取一个数据包 - 调用processData(ByteBuffer b)
     * Parse data from buffer and prepare buffer for reading one packet - calls processData(ByteBuffer b)
     *
     * @param con 连接 / Connection
     * @param buf 包含数据包数据的缓冲区 / Buffer with packet data
     * @return 如果数据包解析成功则为true / True if packet was parsed
     */
    private boolean parse(AConnection con, ByteBuffer buf) {
        short sz = 0;
        try {
            sz = buf.getShort();
            if (sz > 1) {
                sz -= 2;
            }
            ByteBuffer b = (ByteBuffer) buf.slice().limit(sz);
            b.order(ByteOrder.LITTLE_ENDIAN);
            
            buf.position(buf.position() + sz);
            
            return con.processData(b);
        } catch (IllegalArgumentException e) {
            log.warn("Error parsing input from client - account: " + con + " packet size: " + sz + " real size:" + buf.remaining(), e);
            return false;
        }
    }
    
    /**
     * 尽可能多地将数据写入由SelectionKey表示的socketChannel,如果所有数据都写入则禁用键的写入兴趣
     * Write as much data as possible to socketChannel represented by SelectionKey, disable key write interest if all data written
     *
     * @param key 选择键 / Selection key
     */
    final void write(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        AConnection con = (AConnection) key.attachment();
        
        int numWrite;
        ByteBuffer wb = con.writeBuffer;
        
        if (wb.hasRemaining()) {
            try {
                numWrite = socketChannel.write(wb);
            } catch (IOException e) {
                closeConnectionImpl(con);
                return;
            }
            
            if (numWrite == 0) {
                log.info("Write " + numWrite + " ip: " + con.getIP());
                return;
            }
            
            if (wb.hasRemaining()) {
                return;
            }
        }
        
        while (true) {
            wb.clear();
            boolean writeFailed = !con.writeData(wb);
            
            if (writeFailed) {
                wb.limit(0);
                break;
            }
            
            try {
                numWrite = socketChannel.write(wb);
            } catch (IOException e) {
                closeConnectionImpl(con);
                return;
            }
            
            if (numWrite == 0) {
                log.info("Write " + numWrite + " ip: " + con.getIP());
                return;
            }
            
            if (wb.hasRemaining()) {
                return;
            }
        }
        
        if (Assertion.NetworkAssertion) {
            assert !wb.hasRemaining();
        }
        
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        
        if (con.isPendingClose()) {
            closeConnectionImpl(con);
        }
    }
    
    /**
     * 关闭连接并在另一个线程上执行onDisconnect()方法
     * Close connection and execute onDisconnect() method on another thread
     *
     * @param con 要关闭的连接 / Connection to close
     */
    protected final void closeConnectionImpl(AConnection con) {
        if (Assertion.NetworkAssertion) {
            assert Thread.currentThread() == this;
        }
        
        if (con.onlyClose()) {
            dcPool.execute(new DisconnectionTask(con));
        }
    }
}
