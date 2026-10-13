package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 线程池服务静态绑定：把 Spring ObjectProvider 喂入线程池域服务自持的 fail-fast 单例通道。
 * Thread-pool static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Thread-pool services.
 * <p>本组件取代已退役的线程池静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Thread-pool static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class ThreadPoolServiceBindings implements DisposableBean {

    /**
     * 构造并注册各线程池服务的实例提供者。
     * Construct and register instance providers for Thread-pool services.
     * @param threadPoolManagerProvider ThreadPoolManager 提供者 / ThreadPoolManager provider
     */
    public ThreadPoolServiceBindings(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        ThreadPoolManager.setInstanceProvider(threadPoolManagerProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        ThreadPoolManager.setInstanceProvider(null);
    }
}
