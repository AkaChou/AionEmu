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
    /**
     * 已解析的技能引擎缓存；语义同 {@link #resolvedQuestEngine}。
     * Resolved skill-engine cache; same contract as {@link #resolvedQuestEngine}.
     */
    private static volatile ResolvedEngine<SkillEngine> resolvedSkillEngine;
    /**
     * 已解析的任务引擎缓存；仅在真正解析到 Spring bean 后缓存，且与产生它的 provider 绑定：
     * provider 被替换（测试夹具换装、容器重建）时缓存自动失效，不会钉住上一套引擎。
     * Resolved quest-engine cache; filled only after a real Spring bean is resolved and bound to the
     * provider that produced it, so replacing the provider (test fixture swap, container rebuild)
     * invalidates the cache instead of pinning the previous engine.
     */
    private static volatile ResolvedEngine<QuestEngine> resolvedQuestEngine;
    /**
     * 副本引擎的 Spring 提供者。
     * Spring provider for the instance engine.
     */
    private static volatile ObjectProvider<InstanceEngine> instanceEngineProvider;
    /**
     * 已解析的副本引擎缓存；语义同 {@link #resolvedQuestEngine}。
     * Resolved instance-engine cache; same contract as {@link #resolvedQuestEngine}.
     */
    private static volatile ResolvedEngine<InstanceEngine> resolvedInstanceEngine;
    /**
     * AI2 引擎的 Spring 提供者。
     * Spring provider for the AI2 engine.
     */
    private static volatile ObjectProvider<AI2Engine> ai2EngineProvider;
    /**
     * 已解析的 AI2 引擎缓存；语义同 {@link #resolvedQuestEngine}。
     * Resolved AI2-engine cache; same contract as {@link #resolvedQuestEngine}.
     */
    private static volatile ResolvedEngine<AI2Engine> resolvedAi2Engine;
    /**
     * 聊天处理器的 Spring 提供者。
     * Spring provider for the chat processor.
     */
    private static volatile ObjectProvider<ChatProcessor> chatProcessorProvider;
    /**
     * 已解析的聊天处理器缓存；语义同 {@link #resolvedQuestEngine}。
     * Resolved chat-processor cache; same contract as {@link #resolvedQuestEngine}.
     */
    private static volatile ResolvedEngine<ChatProcessor> resolvedChatProcessor;

    /**
     * 构造并注册各引擎的实例提供者。
     * Construct and register instance providers for each engine.
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
        GameEngineServices.instanceEngineProvider = instanceEngineProvider;
        GameEngineServices.ai2EngineProvider = ai2EngineProvider;
        GameEngineServices.chatProcessorProvider = chatProcessorProvider;
        // 新容器重建前先清空上一套引擎缓存，避免跨上下文复用。
        // Clear the previous container's engine caches before a rebuild.
        resolvedQuestEngine = null;
        resolvedSkillEngine = null;
        resolvedInstanceEngine = null;
        resolvedAi2Engine = null;
        resolvedChatProcessor = null;
        QuestEngine.setInstanceProvider(questEngineProvider);
        SkillEngine.setInstanceProvider(skillEngineProvider);
        InstanceEngine.setInstanceProvider(instanceEngineProvider);
        AI2Engine.setInstanceProvider(ai2EngineProvider);
        ChatProcessor.setInstanceProvider(chatProcessorProvider);
    }

    /**
     * 解析任务引擎：优先 Spring 提供者，否则回退工厂。
     * Resolve the quest engine: prefer Spring provider, otherwise fallback factory.
     * @return 任务引擎 / Quest engine
     */
    public static QuestEngine questEngine() {
        ObjectProvider<QuestEngine> provider = questEngineProvider;
        ResolvedEngine<QuestEngine> cached = resolvedQuestEngine;
        if (cached != null && cached.provider() == provider) {
            return cached.engine();
        }
        if (provider == null) {
            return GameEngineServiceFallbacks.questEngine();
        }
        QuestEngine resolved = provider.getIfAvailable();
        if (resolved == null) {
            return GameEngineServiceFallbacks.questEngine();
        }
        resolvedQuestEngine = new ResolvedEngine<>(provider, resolved);
        return resolved;
    }

    /**
     * 解析技能引擎：优先 Spring 提供者，否则回退工厂。
     * Resolve the skill engine: prefer Spring provider, otherwise fallback factory.
     * @return 技能引擎 / Skill engine
     */
    public static SkillEngine skillEngine() {
        ObjectProvider<SkillEngine> provider = skillEngineProvider;
        ResolvedEngine<SkillEngine> cached = resolvedSkillEngine;
        if (cached != null && cached.provider() == provider) {
            return cached.engine();
        }
        SkillEngine resolved = provider == null ? GameEngineServiceFallbacks.skillEngine()
                : provider.getIfAvailable(GameEngineServiceFallbacks::skillEngine);
        resolvedSkillEngine = new ResolvedEngine<>(provider, resolved);
        return resolved;
    }

    /**
     * 解析副本引擎：优先 Spring 提供者，否则单例。
     * Resolve the instance engine: prefer Spring provider, otherwise singleton.
     * @return 副本引擎 / Instance engine
     */
    public static InstanceEngine instanceEngine() {
        ObjectProvider<InstanceEngine> provider = instanceEngineProvider;
        ResolvedEngine<InstanceEngine> cached = resolvedInstanceEngine;
        if (cached != null && cached.provider() == provider) {
            return cached.engine();
        }
        if (provider == null) {
            return InstanceEngine.getInstance();
        }
        InstanceEngine resolved = provider.getIfAvailable();
        if (resolved == null) {
            return InstanceEngine.getInstance();
        }
        resolvedInstanceEngine = new ResolvedEngine<>(provider, resolved);
        return resolved;
    }

    /**
     * 解析 AI2 引擎：优先 Spring 提供者，否则单例。
     * Resolve the AI2 engine: prefer Spring provider, otherwise singleton.
     * @return AI2 引擎 / AI2 engine
     */
    public static AI2Engine ai2Engine() {
        ObjectProvider<AI2Engine> provider = ai2EngineProvider;
        ResolvedEngine<AI2Engine> cached = resolvedAi2Engine;
        if (cached != null && cached.provider() == provider) {
            return cached.engine();
        }
        if (provider == null) {
            return AI2Engine.getInstance();
        }
        AI2Engine resolved = provider.getIfAvailable();
        if (resolved == null) {
            return AI2Engine.getInstance();
        }
        resolvedAi2Engine = new ResolvedEngine<>(provider, resolved);
        return resolved;
    }

    /**
     * 解析聊天处理器：优先 Spring 提供者，否则单例。
     * Resolve the chat processor: prefer Spring provider, otherwise singleton.
     * @return 聊天处理器 / Chat processor
     */
    public static ChatProcessor chatProcessor() {
        ObjectProvider<ChatProcessor> provider = chatProcessorProvider;
        ResolvedEngine<ChatProcessor> cached = resolvedChatProcessor;
        if (cached != null && cached.provider() == provider) {
            return cached.engine();
        }
        if (provider == null) {
            return ChatProcessor.getInstance();
        }
        ChatProcessor resolved = provider.getIfAvailable();
        if (resolved == null) {
            return ChatProcessor.getInstance();
        }
        resolvedChatProcessor = new ResolvedEngine<>(provider, resolved);
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

    /**
     * 引擎解析缓存条目：把已解析实例与产生它的 provider 绑定。
     * Resolved-engine cache entry binding a resolved instance to the provider that produced it.
     * <p>provider 身份变化即视为缓存失效，因此换装 provider 后不会继续返回上一套引擎；
     * 同一 provider 的重复解析仍然零分配，保持 {@code getIfAvailable} 热路径优化。</p>
     * <p>A different provider identity invalidates the entry, so a swapped provider never keeps
     * serving the previous engine, while repeated lookups on the same provider stay allocation-free
     * and keep the {@code getIfAvailable} hot-path saving.</p>
     * @param <T> 引擎类型 / engine type
     */
    private record ResolvedEngine<T>(ObjectProvider<T> provider, T engine) {
    }
}
