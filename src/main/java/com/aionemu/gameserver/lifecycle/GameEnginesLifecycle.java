package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 游戏引擎生命周期：并行加载任务 / 副本 / AI2 / 聊天等引擎，加载状态由基类承载。
 * Game-engines lifecycle: loads quest / instance / AI2 / chat engines in parallel; load state is
 * hosted by the base class.
 */
@Component
@Slf4j
public class GameEnginesLifecycle extends GameStartupStage {

    /**
     * 任务引擎提供者。
     * Quest-engine provider.
     */
    private ObjectProvider<QuestEngine> questEngineProvider;
    /**
     * 副本引擎提供者。
     * Instance-engine provider.
     */
    private ObjectProvider<InstanceEngine> instanceEngineProvider;
    /**
     * AI2 引擎提供者。
     * AI2-engine provider.
     */
    private ObjectProvider<AI2Engine> ai2EngineProvider;
    /**
     * 聊天处理器提供者。
     * Chat-processor provider.
     */
    private ObjectProvider<ChatProcessor> chatProcessorProvider;
    /**
     * 线程池管理器提供者。
     * Thread-pool-manager provider.
     */
    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;
    /**
     * 是否已启动 raw 任务目录预加载。
     * Whether raw quest-catalog preload has started.
     */
    private boolean productionCatalogPreloadStarted;

    /**
     * 可选注入任务引擎提供者。
     * Optionally inject the quest-engine provider.
     * @param questEngineProvider 任务引擎提供者 / Quest-engine provider
     */
    @Autowired(required = false)
    void setQuestEngineProvider(ObjectProvider<QuestEngine> questEngineProvider) {
        this.questEngineProvider = questEngineProvider;
    }

    /**
     * 可选注入副本引擎提供者。
     * Optionally inject the instance-engine provider.
     * @param instanceEngineProvider 副本引擎提供者 / Instance-engine provider
     */
    @Autowired(required = false)
    void setInstanceEngineProvider(ObjectProvider<InstanceEngine> instanceEngineProvider) {
        this.instanceEngineProvider = instanceEngineProvider;
    }

    /**
     * 可选注入 AI2 引擎提供者。
     * Optionally inject the AI2-engine provider.
     * @param ai2EngineProvider AI2 引擎提供者 / AI2-engine provider
     */
    @Autowired(required = false)
    void setAi2EngineProvider(ObjectProvider<AI2Engine> ai2EngineProvider) {
        this.ai2EngineProvider = ai2EngineProvider;
    }

    /**
     * 可选注入聊天处理器提供者。
     * Optionally inject the chat-processor provider.
     * @param chatProcessorProvider 聊天处理器提供者 / Chat-processor provider
     */
    @Autowired(required = false)
    void setChatProcessorProvider(ObjectProvider<ChatProcessor> chatProcessorProvider) {
        this.chatProcessorProvider = chatProcessorProvider;
    }

    /**
     * 可选注入线程池管理器提供者。
     * Optionally inject the thread-pool-manager provider.
     * @param threadPoolManagerProvider 线程池管理器提供者 / Thread-pool-manager provider
     */
    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
    }


    /**
     * 启动本阶段：并行加载全部游戏引擎。
     * Start this stage: load all game engines in parallel.
     */
    @Override
    protected void runStage() {
        printSection();
        List<GameEngine> engines = List.copyOf(engines());
        CountDownLatch progressLatch = new CountDownLatch(engines.size());
        for (GameEngine engine : engines) {
            execute(() -> engine.load(progressLatch));
        }
        await(progressLatch);
    }

    /**
     * 在长耗时静态数据加载前预编译任务目录（幂等）。
     * Precompile the quest catalog before long-running static-data loading (idempotent).
     */
    public synchronized void preloadProductionCatalog() {
        if (isLoaded() || productionCatalogPreloadStarted) {
            return;
        }
        preloadQuestCatalog();
        productionCatalogPreloadStarted = true;
    }

    /**
     * 打印引擎启动分区标题。
     * Print the engines startup section header.
     */
    protected void printSection() {
        Util.printSection(I18n.get("console.section.engines"));
    }

    /**
     * 提前启动 raw 任务目录编译，供引擎加载阶段消费。
     * Start raw quest-catalog compilation early for consumption by engine loading.
     */
    protected void preloadQuestCatalog() {
        questEngine().preloadProductionCatalog();
    }

    /**
     * 返回待加载的游戏引擎列表。
     * Return the list of game engines to load.
     * @return 引擎列表 / Engine list
     */
    protected List<GameEngine> engines() {
        return List.of(
            questEngine(),
            instanceEngine(),
            ai2Engine(),
            chatProcessor()
        );
    }

    /**
     * 通过线程池异步执行任务。
     * Execute a task asynchronously via the thread pool.
     * @param runnable 待执行任务 / Task to run
     */
    protected void execute(Runnable runnable) {
        threadPoolManager().execute(runnable);
    }

    /**
     * 等待全部引擎加载完成；中断时记录警告并恢复中断标志。
     * Await completion of all engine loads; on interrupt log a warning and restore the interrupt flag.
     * @param progressLatch 进度闩 / Progress latch
     */
    private void await(CountDownLatch progressLatch) {
        try {
            progressLatch.await();
        } catch (InterruptedException e) {
            log.warn(I18n.get("console.startup.engines_interrupted"), e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 解析任务引擎。
     * Resolve the quest engine.
     * @return 任务引擎 / Quest engine
     */
    private QuestEngine questEngine() {
        return questEngineProvider.getObject();
    }

    /**
     * 解析副本引擎。
     * Resolve the instance engine.
     * @return 副本引擎 / Instance engine
     */
    private InstanceEngine instanceEngine() {
        return instanceEngineProvider.getObject();
    }

    /**
     * 解析 AI2 引擎。
     * Resolve the AI2 engine.
     * @return AI2 引擎 / AI2 engine
     */
    private AI2Engine ai2Engine() {
        return ai2EngineProvider.getObject();
    }

    /**
     * 解析聊天处理器。
     * Resolve the chat processor.
     * @return 聊天处理器 / Chat processor
     */
    private ChatProcessor chatProcessor() {
        return chatProcessorProvider.getObject();
    }

    /**
     * 解析线程池管理器。
     * Resolve the thread-pool manager.
     * @return 线程池管理器 / Thread-pool manager
     */
    private ThreadPoolManager threadPoolManager() {
        return threadPoolManagerProvider.getObject();
    }
}
