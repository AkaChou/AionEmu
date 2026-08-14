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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 游戏引擎网关：对引擎列表、线程池执行与分区打印的委托入口。
 * Game-engines gateway: delegation entry for engine list, thread-pool execution, and section printing.
 */
@Component
public class GameEnginesGateway {

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
     * 运行时桥接提供者。
     * Runtime-bridge provider.
     */
    private ObjectProvider<GameEnginesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入任务引擎提供者。
     * Optionally inject the quest-engine provider.
     *
     * @param questEngineProvider 任务引擎提供者 / Quest-engine provider
     */
    @Autowired(required = false)
    void setQuestEngineProvider(ObjectProvider<QuestEngine> questEngineProvider) {
        this.questEngineProvider = questEngineProvider;
    }

    /**
     * 可选注入副本引擎提供者。
     * Optionally inject the instance-engine provider.
     *
     * @param instanceEngineProvider 副本引擎提供者 / Instance-engine provider
     */
    @Autowired(required = false)
    void setInstanceEngineProvider(ObjectProvider<InstanceEngine> instanceEngineProvider) {
        this.instanceEngineProvider = instanceEngineProvider;
    }

    /**
     * 可选注入 AI2 引擎提供者。
     * Optionally inject the AI2-engine provider.
     *
     * @param ai2EngineProvider AI2 引擎提供者 / AI2-engine provider
     */
    @Autowired(required = false)
    void setAi2EngineProvider(ObjectProvider<AI2Engine> ai2EngineProvider) {
        this.ai2EngineProvider = ai2EngineProvider;
    }

    /**
     * 可选注入聊天处理器提供者。
     * Optionally inject the chat-processor provider.
     *
     * @param chatProcessorProvider 聊天处理器提供者 / Chat-processor provider
     */
    @Autowired(required = false)
    void setChatProcessorProvider(ObjectProvider<ChatProcessor> chatProcessorProvider) {
        this.chatProcessorProvider = chatProcessorProvider;
    }

    /**
     * 可选注入线程池管理器提供者。
     * Optionally inject the thread-pool-manager provider.
     *
     * @param threadPoolManagerProvider 线程池管理器提供者 / Thread-pool-manager provider
     */
    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    /**
     * 可选注入运行时桥接提供者。
     * Optionally inject the runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameEnginesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 打印引擎启动分区标题。
     * Print the engines startup section header.
     */
    public void printSection() {
        Util.printSection(I18n.get("console.section.engines"));
    }

    /**
     * 返回待加载的游戏引擎列表。
     * Return the list of game engines to load.
     *
     * @return 引擎列表 / Engine list
     */
    public List<GameEngine> engines() {
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
     *
     * @param runnable 待执行任务 / Task to run
     */
    public void execute(Runnable runnable) {
        threadPoolManager().execute(runnable);
    }

    /**
     * 解析任务引擎。
     * Resolve the quest engine.
     *
     * @return 任务引擎 / Quest engine
     */
    private QuestEngine questEngine() {
        if (questEngineProvider == null) {
            return runtimeBridge().questEngine();
        }
        return questEngineProvider.getIfAvailable(() -> runtimeBridge().questEngine());
    }

    /**
     * 解析副本引擎。
     * Resolve the instance engine.
     *
     * @return 副本引擎 / Instance engine
     */
    private InstanceEngine instanceEngine() {
        if (instanceEngineProvider == null) {
            return runtimeBridge().instanceEngine();
        }
        return instanceEngineProvider.getIfAvailable(() -> runtimeBridge().instanceEngine());
    }

    /**
     * 解析 AI2 引擎。
     * Resolve the AI2 engine.
     *
     * @return AI2 引擎 / AI2 engine
     */
    private AI2Engine ai2Engine() {
        if (ai2EngineProvider == null) {
            return runtimeBridge().ai2Engine();
        }
        return ai2EngineProvider.getIfAvailable(() -> runtimeBridge().ai2Engine());
    }

    /**
     * 解析聊天处理器。
     * Resolve the chat processor.
     *
     * @return 聊天处理器 / Chat processor
     */
    private ChatProcessor chatProcessor() {
        if (chatProcessorProvider == null) {
            return runtimeBridge().chatProcessor();
        }
        return chatProcessorProvider.getIfAvailable(() -> runtimeBridge().chatProcessor());
    }

    /**
     * 解析线程池管理器。
     * Resolve the thread-pool manager.
     *
     * @return 线程池管理器 / Thread-pool manager
     */
    private ThreadPoolManager threadPoolManager() {
        if (threadPoolManagerProvider == null) {
            return runtimeBridge().threadPoolManager();
        }
        return threadPoolManagerProvider.getIfAvailable(() -> runtimeBridge().threadPoolManager());
    }

    /**
     * 解析运行时桥接。
     * Resolve the runtime bridge.
     *
     * @return 运行时桥接 / Runtime bridge
     */
    private GameEnginesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameEnginesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameEnginesRuntimeBridge::new);
    }
}
