package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HTML 生命周期：打印分区并解析 HTML 缓存服务，加载状态由基类承载。
 * HTML lifecycle: prints the section and resolves the HTML cache service; load state is hosted
 * by the base class.
 */
@Component
public class GameHtmlLifecycle extends GameStartupStage {

    /**
     * HTML 缓存服务提供者。
     * HTML cache service provider.
     */
    private ObjectProvider<HTMLCache> htmlCacheProvider;

    /**
     * 可选注入 HTML 缓存服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of HTML cache service.
     * @param htmlCacheProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setHtmlCacheProvider(ObjectProvider<HTMLCache> htmlCacheProvider) {
        this.htmlCacheProvider = htmlCacheProvider;
    }

    /**
     * 启动本阶段：打印 HTML 分区并解析 HTML 缓存。
     * Start this stage: print the HTML section and resolve the HTML cache.
     */
    @Override
    protected void runStage() {
        Util.printSectionIfVerbose(I18n.get("console.section.html"));
        htmlCacheProvider.getObject();
    }
}
