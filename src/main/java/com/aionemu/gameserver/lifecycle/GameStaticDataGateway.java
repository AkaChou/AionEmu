package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.dataholders.DataManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 静态数据网关：触发 DataManager 解析与加载。
 * Static-data gateway: triggers DataManager resolution and load.
 */
@Component
public class GameStaticDataGateway {

    /**
     * DataManager 的可选 Spring 提供者。
     * Optional Spring provider for {@link DataManager}.
     */
    private ObjectProvider<DataManager> dataManagerProvider;


    /**
     * 注入 DataManager 提供者。
     * Inject the DataManager provider.
     * @param dataManagerProvider DataManager 提供者 / DataManager provider
     */
    @Autowired(required = false)
    void setDataManagerProvider(ObjectProvider<DataManager> dataManagerProvider) {
        this.dataManagerProvider = dataManagerProvider;
    }


    /**
     * 加载静态数据：解析 DataManager Bean（轻量构造）后显式触发加载。
     * Load static data: resolve the DataManager bean (cheap construction), then trigger loading explicitly.
     * <p>加载必须发生在 Bean 创建回调之外：Spring 创建单例期间持有全局 singletonLock，
     * 若加载在构造中进行，并行加载线程解析任何懒加载 Bean 都会与主线程互相等待形成死锁。
     * Loading must happen outside bean-creation callbacks: Spring holds its global singleton lock
     * while creating a singleton, so loading inside the constructor deadlocks against parallel
     * loader threads resolving lazy beans.
     */
    public void load() {
        dataManager().load();
    }

    /**
     * 解析 DataManager：优先 Spring，否则走运行时桥。
     * Resolve DataManager: prefer Spring, otherwise the runtime bridge.
     * @return DataManager 实例 / DataManager instance
     */
    private DataManager dataManager() {
        return dataManagerProvider.getObject();
    }

}
