package com.aionemu.commons.network;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import java.util.Objects;

/**
 * Netty 事件循环提供器，支持共享组或按需创建自持有组。
 * Netty event-loop provider supporting shared groups or on-demand owned groups.
 */
public final class NettyEventLoopProvider {

    private static EventLoopGroup sharedBossGroup;
    private static EventLoopGroup sharedWorkerGroup;

    /**
     * 禁止实例化。
     * Prevent instantiation.
     */
    private NettyEventLoopProvider() {
    }

    /**
     * 注册进程级共享事件循环组。
     * Register process-wide shared event loop groups.
     *
     * @param bossGroup Boss 组 / Boss group
     * @param workerGroup Worker 组 / Worker group
     */
    public static synchronized void useShared(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        sharedBossGroup = Objects.requireNonNull(bossGroup, "bossGroup");
        sharedWorkerGroup = Objects.requireNonNull(workerGroup, "workerGroup");
    }

    /**
     * 清除匹配的共享事件循环组引用。
     * Clear shared event loop group references when they match.
     *
     * @param bossGroup Boss 组 / Boss group
     * @param workerGroup Worker 组 / Worker group
     */
    public static synchronized void clearShared(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        if (sharedBossGroup == bossGroup && sharedWorkerGroup == workerGroup) {
            sharedBossGroup = null;
            sharedWorkerGroup = null;
        }
    }

    /**
     * 获取事件循环分配（优先共享，否则新建并标记自持有）。
     * Acquire event-loop allocation (shared preferred, otherwise newly owned).
     *
     * @return 事件循环分配 / Event-loop allocation
     */
    public static synchronized Allocation acquire() {
        if (sharedBossGroup != null && sharedWorkerGroup != null) {
            return new Allocation(sharedBossGroup, sharedWorkerGroup, false);
        }
        return new Allocation(newBossGroup(), newWorkerGroup(), true);
    }

    /**
     * 创建单线程 Boss 组。
     * Create a single-thread boss group.
     *
     * @return Boss 组 / Boss group
     */
    public static EventLoopGroup newBossGroup() {
        return new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    }

    /**
     * 创建默认 Worker 组。
     * Create a default worker group.
     *
     * @return Worker 组 / Worker group
     */
    public static EventLoopGroup newWorkerGroup() {
        return new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    }

    /**
     * 事件循环分配结果。
     * Event-loop allocation result.
     *
     * @param bossGroup Boss 组 / Boss group
     * @param workerGroup Worker 组 / Worker group
     * @param owned 是否由调用方负责关闭 / Whether caller owns shutdown
     */
    public record Allocation(EventLoopGroup bossGroup, EventLoopGroup workerGroup, boolean owned) {

        /**
         * 若自持有则优雅关闭两组。
         * Gracefully shut down both groups when owned.
         */
        public void shutdownGracefully() {
            if (!owned) {
                return;
            }
            workerGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
    }
}
