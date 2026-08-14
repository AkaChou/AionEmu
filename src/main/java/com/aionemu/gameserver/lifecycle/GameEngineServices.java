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
     * 副本引擎的 Spring 提供者。
     * Spring provider for the instance engine.
     */
    private static volatile ObjectProvider<InstanceEngine> instanceEngineProvider;
    /**
     * AI2 引擎的 Spring 提供者。
     * Spring provider for the AI2 engine.
     */
    private static volatile ObjectProvider<AI2Engine> ai2EngineProvider;
    /**
     * 聊天处理器的 Spring 提供者。
     * Spring provider for the chat processor.
     */
    private static volatile ObjectProvider<ChatProcessor> chatProcessorProvider;

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
        ObjectProvider<QuestEngine> provider = questEngineProvider;
        if (provider == null) {
            return GameEngineServiceFallbacks.questEngine();
        }
        return provider.getIfAvailable(GameEngineServiceFallbacks::questEngine);
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
        ObjectProvider<InstanceEngine> provider = instanceEngineProvider;
        if (provider == null) {
            return InstanceEngine.getInstance();
        }
        return provider.getIfAvailable(InstanceEngine::getInstance);
    }

    /**
     * 解析 AI2 引擎：优先 Spring 提供者，否则单例。
     * Resolve the AI2 engine: prefer Spring provider, otherwise singleton.
     *
     * @return AI2 引擎 / AI2 engine
     */
    public static AI2Engine ai2Engine() {
        ObjectProvider<AI2Engine> provider = ai2EngineProvider;
        if (provider == null) {
            return AI2Engine.getInstance();
        }
        return provider.getIfAvailable(AI2Engine::getInstance);
    }

    /**
     * 解析聊天处理器：优先 Spring 提供者，否则单例。
     * Resolve the chat processor: prefer Spring provider, otherwise singleton.
     *
     * @return 聊天处理器 / Chat processor
     */
    public static ChatProcessor chatProcessor() {
        ObjectProvider<ChatProcessor> provider = chatProcessorProvider;
        if (provider == null) {
            return ChatProcessor.getInstance();
        }
        return provider.getIfAvailable(ChatProcessor::getInstance);
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
        instanceEngineProvider = null;
        ai2EngineProvider = null;
        chatProcessorProvider = null;
        QuestEngine.setInstanceProvider(null);
        SkillEngine.setInstanceProvider(null);
        InstanceEngine.setInstanceProvider(null);
        AI2Engine.setInstanceProvider(null);
        ChatProcessor.setInstanceProvider(null);
    }
}
