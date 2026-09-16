package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 游戏引擎 Spring 服务门面 / 静态访问桥：注册引擎实例提供者并对外提供静态访问。
 * static access bridge for game engines: registers engine instance providers and exposes static accessors.
 */
@Component
public final class GameEngineServices implements DisposableBean {

    /**
     * 任务引擎的 Spring 提供者。
     * Spring provider for the quest engine.
     */
    private static volatile ObjectProvider<QuestEngine> questEngineProvider;
    /**
     * 技能引擎的 Spring 提供者。
     * Spring provider for the skill engine.
     */
    private static volatile ObjectProvider<SkillEngine> skillEngineProvider;
    private static volatile SkillEngine resolvedSkillEngine;
    /**
     * 已解析的任务引擎单例；仅在真正解析到 Spring bean 后缓存，避免把回退实例钉住。
     * Resolved quest-engine singleton; cached only after a real Spring bean is resolved, so a fallback
     * instance is never pinned.
     */
    private static volatile QuestEngine resolvedQuestEngine;
    /**
     * 副本引擎的 Spring 提供者。
     * Spring provider for the instance engine.
     */
    private static volatile ObjectProvider<InstanceEngine> instanceEngineProvider;
    /**
     * 已解析的副本引擎单例；语义同 {@link #resolvedQuestEngine}。
     * Resolved instance-engine singleton; same contract as {@link #resolvedQuestEngine}.
     */
    private static volatile InstanceEngine resolvedInstanceEngine;
    /**
     * AI2 引擎的 Spring 提供者。
     * Spring provider for the AI2 engine.
     */
    private static volatile ObjectProvider<AI2Engine> ai2EngineProvider;
    /**
     * 已解析的 AI2 引擎单例；语义同 {@link #resolvedQuestEngine}。
     * Resolved AI2-engine singleton; same contract as {@link #resolvedQuestEngine}.
     */
    private static volatile AI2Engine resolvedAi2Engine;
    /**
     * 聊天处理器的 Spring 提供者。
     * Spring provider for the chat processor.
     */
    private static volatile ObjectProvider<ChatProcessor> chatProcessorProvider;
    /**
     * 已解析的聊天处理器单例；语义同 {@link #resolvedQuestEngine}。
     * Resolved chat-processor singleton; same contract as {@link #resolvedQuestEngine}.
     */
    private static volatile ChatProcessor resolvedChatProcessor;

    /**
     * 构造并注册各引擎的实例提供者。
     * Construct and register instance providers for each engine.
     *
     * @param questEngineProvider 任务引擎提供者 / Quest-engine provider
     * @param skillEngineProvider 技能引擎提供者 / Skill-engine provider
     * @param instanceEngineProvider 副本引擎提供者 / Instance-engine provider
     * @param ai2EngineProvider AI2 引擎提供者 / AI2-engine provider
     * @param chatProcessorProvider 聊天处理器提供者 / Chat-processor provider
     */
    public GameEngineServices(ObjectProvider<QuestEngine> questEngineProvider,
            ObjectProvider<SkillEngine> skillEngineProvider, ObjectProvider<InstanceEngine> instanceEngineProvider,
            ObjectProvider<AI2Engine> ai2EngineProvider, ObjectProvider<ChatProcessor> chatProcessorProvider) {
        GameEngineServices.questEngineProvider = questEngineProvider;
        GameEngineServices.skillEngineProvider = skillEngineProvider;
        resolvedSkillEngine = null;
        GameEngineServices.instanceEngineProvider = instanceEngineProvider;
        GameEngineServices.ai2EngineProvider = ai2EngineProvider;
        GameEngineServices.chatProcessorProvider = chatProcessorProvider;
        QuestEngine.setInstanceProvider(questEngineProvider);
        SkillEngine.setInstanceProvider(skillEngineProvider);
        InstanceEngine.setInstanceProvider(instanceEngineProvider);
        AI2Engine.setInstanceProvider(ai2EngineProvider);
        ChatProcessor.setInstanceProvider(chatProcessorProvider);
    }

    /**
     * 解析任务引擎：优先 Spring 提供者，否则回退工厂。
     * Resolve the quest engine: prefer Spring provider, otherwise fallback factory.
     *
     * @return 任务引擎 / Quest engine
     */
    public static QuestEngine questEngine() {
        QuestEngine resolved = resolvedQuestEngine;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<QuestEngine> provider = questEngineProvider;
        if (provider == null) {
            return GameEngineServiceFallbacks.questEngine();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return GameEngineServiceFallbacks.questEngine();
        }
        resolvedQuestEngine = resolved;
        return resolved;
    }

    /**
     * 解析技能引擎：优先 Spring 提供者，否则回退工厂。
     * Resolve the skill engine: prefer Spring provider, otherwise fallback factory.
     *
     * @return 技能引擎 / Skill engine
     */
    public static SkillEngine skillEngine() {
        SkillEngine resolved = resolvedSkillEngine;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<SkillEngine> provider = skillEngineProvider;
        resolved = provider == null ? GameEngineServiceFallbacks.skillEngine()
                : provider.getIfAvailable(GameEngineServiceFallbacks::skillEngine);
        resolvedSkillEngine = resolved;
        return resolved;
    }

    /**
     * 解析副本引擎：优先 Spring 提供者，否则单例。
     * Resolve the instance engine: prefer Spring provider, otherwise singleton.
     *
     * @return 副本引擎 / Instance engine
     */
    public static InstanceEngine instanceEngine() {
        InstanceEngine resolved = resolvedInstanceEngine;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<InstanceEngine> provider = instanceEngineProvider;
        if (provider == null) {
            return InstanceEngine.getInstance();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return InstanceEngine.getInstance();
        }
        resolvedInstanceEngine = resolved;
        return resolved;
    }

    /**
     * 解析 AI2 引擎：优先 Spring 提供者，否则单例。
     * Resolve the AI2 engine: prefer Spring provider, otherwise singleton.
     *
     * @return AI2 引擎 / AI2 engine
     */
    public static AI2Engine ai2Engine() {
        AI2Engine resolved = resolvedAi2Engine;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<AI2Engine> provider = ai2EngineProvider;
        if (provider == null) {
            return AI2Engine.getInstance();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return AI2Engine.getInstance();
        }
        resolvedAi2Engine = resolved;
        return resolved;
    }

    /**
     * 解析聊天处理器：优先 Spring 提供者，否则单例。
     * Resolve the chat processor: prefer Spring provider, otherwise singleton.
     *
     * @return 聊天处理器 / Chat processor
     */
    public static ChatProcessor chatProcessor() {
        ChatProcessor resolved = resolvedChatProcessor;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<ChatProcessor> provider = chatProcessorProvider;
        if (provider == null) {
            return ChatProcessor.getInstance();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return ChatProcessor.getInstance();
        }
        resolvedChatProcessor = resolved;
        return resolved;
    }

    /**
     * 销毁时清理静态提供者与引擎实例桥。
     * Clear static providers and engine instance bridges on destroy.
     */
    @Override
    public void destroy() {
        questEngineProvider = null;
        skillEngineProvider = null;
        resolvedSkillEngine = null;
        resolvedQuestEngine = null;
        instanceEngineProvider = null;
        ai2EngineProvider = null;
        chatProcessorProvider = null;
        resolvedInstanceEngine = null;
        resolvedAi2Engine = null;
        resolvedChatProcessor = null;
        QuestEngine.setInstanceProvider(null);
        SkillEngine.setInstanceProvider(null);
        InstanceEngine.setInstanceProvider(null);
        AI2Engine.setInstanceProvider(null);
        ChatProcessor.setInstanceProvider(null);
    }
}
