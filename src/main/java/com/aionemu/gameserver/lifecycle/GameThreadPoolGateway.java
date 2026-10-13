package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 线程池网关：解析并启动/关闭 ThreadPoolManager。
 * Thread-pool gateway: resolves and starts/shuts down ThreadPoolManager.
 */
@Component
public class GameThreadPoolGateway {

    /**
     * ThreadPoolManager 的可选 Spring 提供者。
     * Optional Spring provider for {@link ThreadPoolManager}.
     */
    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;


    /**
     * 注入 ThreadPoolManager 提供者。
     * Inject the ThreadPoolManager provider.
     * @param threadPoolManagerProvider ThreadPoolManager 提供者 / ThreadPoolManager provider
     */
    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
    }


    /**
     * 启动线程池（解析 ThreadPoolManager 以触发初始化）。
     * Start the thread pool (resolve ThreadPoolManager to trigger initialization).
     */
    public void start() {
        threadPoolManager();
    }

    /**
     * 关闭线程池。
     * Shut down the thread pool.
     */
    public void stop() {
        threadPoolManager().shutdown();
    }

    /**
     * 解析 ThreadPoolManager 并缓存解析结果。
     * Resolve ThreadPoolManager and remember the resolved instance.
     * @return ThreadPoolManager 实例 / ThreadPoolManager instance
     */
    private ThreadPoolManager threadPoolManager() {
        return GameThreadPoolServices.rememberThreadPoolManager(threadPoolManagerProvider.getObject());
    }

}
