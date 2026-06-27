package com.aionemu.commons.network;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import java.util.Objects;

public final class NettyEventLoopProvider {

    private static EventLoopGroup sharedBossGroup;
    private static EventLoopGroup sharedWorkerGroup;

    private NettyEventLoopProvider() {
    }

    public static synchronized void useShared(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        sharedBossGroup = Objects.requireNonNull(bossGroup, "bossGroup");
        sharedWorkerGroup = Objects.requireNonNull(workerGroup, "workerGroup");
    }

    public static synchronized void clearShared(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        if (sharedBossGroup == bossGroup && sharedWorkerGroup == workerGroup) {
            sharedBossGroup = null;
            sharedWorkerGroup = null;
        }
    }

    public static synchronized Allocation acquire() {
        if (sharedBossGroup != null && sharedWorkerGroup != null) {
            return new Allocation(sharedBossGroup, sharedWorkerGroup, false);
        }
        return new Allocation(newBossGroup(), newWorkerGroup(), true);
    }

    public static EventLoopGroup newBossGroup() {
        return new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    }

    public static EventLoopGroup newWorkerGroup() {
        return new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    }

    public record Allocation(EventLoopGroup bossGroup, EventLoopGroup workerGroup, boolean owned) {

        public void shutdownGracefully() {
            if (!owned) {
                return;
            }
            workerGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
    }
}
