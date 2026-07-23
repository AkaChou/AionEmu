package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.scriptEngine.ScriptEngine;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;

/**
 * 游戏引擎服务的回退工厂：在 Spring 提供者不可用时返回各引擎单例。
 * Fallback factory for game-engine services: returns each engine singleton when Spring providers are unavailable.
 */
final class GameEngineServiceFallbacks {

    /**
     * 禁止实例化。
     * Prevent instantiation.
     */
    private GameEngineServiceFallbacks() {
    }

    /**
     * 返回任务引擎回退实例。
     * Return the quest-engine fallback instance.
     *
     * Quest engine
     */
    static QuestEngine questEngine() {
        return QuestEngineFallback.INSTANCE;
    }

    /**
     * 返回技能引擎回退实例。
     * Return the skill-engine fallback instance.
     *
     * Skill engine
     */
    static SkillEngine skillEngine() {
        return SkillEngineFallback.INSTANCE;
    }

    /**
     * 返回副本引擎回退实例。
     * Return the instance-engine fallback instance.
     *
     * Instance engine
     */
    static InstanceEngine instanceEngine() {
        return InstanceEngineFallback.INSTANCE;
    }

    /**
     * 返回 AI2 引擎回退实例。
     * Return the AI2-engine fallback instance.
     *
     * AI2 engine
     */
    static AI2Engine ai2Engine() {
        return Ai2EngineFallback.INSTANCE;
    }

    /**
     * 返回脚本引擎回退实例。
     * Return the script-engine fallback instance.
     *
     * @return 脚本引擎 / Script engine
     */
    static ScriptEngine scriptEngine() {
        return ScriptEngineFallback.INSTANCE;
    }

    /**
     * 返回聊天处理器回退实例。
     * Return the chat-processor fallback instance.
     *
     * @return 聊天处理器 / Chat processor
     */
    static ChatProcessor chatProcessor() {
        return ChatProcessorFallback.INSTANCE;
    }

    /**
     * 返回线程池管理器回退实例（委托核心服务回退）。
     * Return the thread-pool-manager fallback instance (delegates to core-service fallbacks).
     *
     * @return 线程池管理器 / Thread-pool manager
     */
    static ThreadPoolManager threadPoolManager() {
        return GameCoreServiceFallbacks.threadPoolManager();
    }

    /**
     * 任务引擎懒加载回退持有者。
     * Lazy fallback holder for the quest engine.
     */
    private static final class QuestEngineFallback {
        private static final QuestEngine INSTANCE = QuestEngine.getInstance();
    }

    /**
     * 技能引擎懒加载回退持有者。
     * Lazy fallback holder for the skill engine.
     */
    private static final class SkillEngineFallback {
        private static final SkillEngine INSTANCE = SkillEngine.getInstance();
    }

    /**
     * 副本引擎懒加载回退持有者。
     * Lazy fallback holder for the instance engine.
     */
    private static final class InstanceEngineFallback {
        private static final InstanceEngine INSTANCE = InstanceEngine.getInstance();
    }

    /**
     * AI2 引擎懒加载回退持有者。
     * Lazy fallback holder for the AI2 engine.
     */
    private static final class Ai2EngineFallback {
        private static final AI2Engine INSTANCE = AI2Engine.getInstance();
    }

    /**
     * 脚本引擎懒加载回退持有者。
     * Lazy fallback holder for the script engine.
     */
    private static final class ScriptEngineFallback {
        private static final ScriptEngine INSTANCE = ScriptEngine.getInstance();
    }

    /**
     * 聊天处理器懒加载回退持有者。
     * Lazy fallback holder for the chat processor.
     */
    private static final class ChatProcessorFallback {
        private static final ChatProcessor INSTANCE = ChatProcessor.getInstance();
    }
}
