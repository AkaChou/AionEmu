package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 游戏引擎运行时桥接：在 Spring 提供者与回退工厂之间解析引擎与线程池。
 * Game-engines runtime bridge: resolves engines and the thread pool between Spring providers and fallback factories.
 */
@Component
public class GameEnginesRuntimeBridge {

    /**
     * 任务引擎提供者。
     * Quest-engine provider.
     */
    private ObjectProvider<QuestEngine> questEngineProvider;
    /**
     * 技能引擎提供者。
     * Skill-engine provider.
     */
    private ObjectProvider<SkillEngine> skillEngineProvider;
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
     * 可选注入技能引擎提供者。
     * Optionally inject the skill-engine provider.
     *
     * @param skillEngineProvider 技能引擎提供者 / Skill-engine provider
     */
    @Autowired(required = false)
    void setSkillEngineProvider(ObjectProvider<SkillEngine> skillEngineProvider) {
        this.skillEngineProvider = skillEngineProvider;
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
     * 解析任务引擎。
     * Resolve the quest engine.
     *
     * @return 任务引擎 / Quest engine
     */
    public QuestEngine questEngine() {
        if (questEngineProvider == null) {
            return GameEngineServiceFallbacks.questEngine();
        }
        return questEngineProvider.getIfAvailable(GameEngineServiceFallbacks::questEngine);
    }

    /**
     * 解析技能引擎。
     * Resolve the skill engine.
     *
     * @return 技能引擎 / Skill engine
     */
    public SkillEngine skillEngine() {
        if (skillEngineProvider == null) {
            return GameEngineServiceFallbacks.skillEngine();
        }
        return skillEngineProvider.getIfAvailable(GameEngineServiceFallbacks::skillEngine);
    }

    /**
     * 解析副本引擎。
     * Resolve the instance engine.
     *
     * @return 副本引擎 / Instance engine
     */
    public InstanceEngine instanceEngine() {
        if (instanceEngineProvider == null) {
            return GameEngineServiceFallbacks.instanceEngine();
        }
        return instanceEngineProvider.getIfAvailable(GameEngineServiceFallbacks::instanceEngine);
    }

    /**
     * 解析 AI2 引擎。
     * Resolve the AI2 engine.
     *
     * @return AI2 引擎 / AI2 engine
     */
    public AI2Engine ai2Engine() {
        if (ai2EngineProvider == null) {
            return GameEngineServiceFallbacks.ai2Engine();
        }
        return ai2EngineProvider.getIfAvailable(GameEngineServiceFallbacks::ai2Engine);
    }

    /**
     * 解析聊天处理器。
     * Resolve the chat processor.
     *
     * @return 聊天处理器 / Chat processor
     */
    public ChatProcessor chatProcessor() {
        if (chatProcessorProvider == null) {
            return GameEngineServiceFallbacks.chatProcessor();
        }
        return chatProcessorProvider.getIfAvailable(GameEngineServiceFallbacks::chatProcessor);
    }

    /**
     * 解析线程池管理器。
     * Resolve the thread-pool manager.
     *
     * @return 线程池管理器 / Thread-pool manager
     */
    public ThreadPoolManager threadPoolManager() {
        if (threadPoolManagerProvider == null) {
            return GameEngineServiceFallbacks.threadPoolManager();
        }
        return threadPoolManagerProvider.getIfAvailable(GameEngineServiceFallbacks::threadPoolManager);
    }
}
