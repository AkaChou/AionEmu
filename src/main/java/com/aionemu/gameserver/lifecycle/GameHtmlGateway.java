package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HTML 对话框/页面网关：打印分区并解析 HTML 缓存服务。
 * HTML dialog/page gateway: prints the section and resolves the HTML cache service.
 */
@Component
public class GameHtmlGateway {

    /**
     * HTML 缓存服务提供者。
     * HTML cache service provider.
     */
    private ObjectProvider<HTMLCache> htmlCacheProvider;

    /**
     * 核心服务运行时桥提供者。
     * Core-services runtime-bridge provider.
     */
    private ObjectProvider<GameCoreServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入 HTML 缓存服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of HTML cache service.
     *
     * @param htmlCacheProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setHtmlCacheProvider(ObjectProvider<HTMLCache> htmlCacheProvider) {
        this.htmlCacheProvider = htmlCacheProvider;
    }

    /**
     * 可选注入核心服务运行时桥 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of core-services runtime bridge.
     *
     * @param runtimeBridgeProvider 运行时桥提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameCoreServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 启动 HTML：打印分区并解析 HTML 缓存。
     * Start HTML: print the section and resolve the HTML cache.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.html"));
        htmlCache();
    }

    /**
     * 解析 HTML 缓存：优先 Spring，否则经运行时桥回退。
     * Resolve HTML cache: prefer Spring, otherwise fall back via runtime bridge.
     *
     * @return HTML 缓存实例 / HTML cache instance
     */
    private HTMLCache htmlCache() {
        if (htmlCacheProvider == null) {
            return runtimeBridge().htmlCache();
        }
        return htmlCacheProvider.getIfAvailable(() -> runtimeBridge().htmlCache());
    }

    /**
     * 解析核心服务运行时桥：优先 Spring，否则新建。
     * Resolve core-services runtime bridge: prefer Spring, otherwise create new.
     *
     * @return 运行时桥实例 / Runtime-bridge instance
     */
    private GameCoreServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameCoreServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameCoreServicesRuntimeBridge::new);
    }
}
