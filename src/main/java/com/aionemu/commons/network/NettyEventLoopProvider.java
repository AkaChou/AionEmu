package com.aionemu.commons.network;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
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
        return new Allocation(new NioEventLoopGroup(1), new NioEventLoopGroup(), true);
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
