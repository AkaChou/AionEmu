package com.aionemu.gameserver.ai2;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * AI2 调试日志工具，仅在 AI 开启日志或移动调试开关时输出。
 * AI2 debug logging helper that writes only when AI logging or move debug is enabled.
 *
 * @author ATracer
 */
@Slf4j
public class AI2Logger {


	/**
	 * 输出 AI 信息日志（受 {@link AbstractAI#isLogging()} 控制）。
	 * Logs an AI info message (gated by {@link AbstractAI#isLogging()}).
	 *
	 * @param ai AI 实例 / AI instance
	 * @param message 日志消息 / log message
	 */
	public static final void info(AbstractAI ai, String message) {
		if (ai.isLogging()) {
			log.info(I18n.get("log.25ad741e4183", ai.getOwner().getObjectId(), message));
		}
	}

	/**
	 * 输出 AI 信息日志（将 {@link AI2} 转为 {@link AbstractAI}）。
	 * Logs an AI info message (casts {@link AI2} to {@link AbstractAI}).
	 *
	 * @param ai AI 实例 / AI instance
	 * @param message 日志消息 / log message
	 */
	public static final void info(AI2 ai, String message) {
		info((AbstractAI) ai, message);
	}

	/**
	 * 输出移动相关调试信息（受 {@link AIConfig#MOVE_DEBUG} 与 AI 日志开关控制）。
	 * Logs movement-related debug info (gated by {@link AIConfig#MOVE_DEBUG} and AI logging).
	 *
	 * @param owner 生物所有者 / creature owner
	 * @param message 日志消息 / log message
	 */
	public static void moveinfo(Creature owner, String message) {
		if (AIConfig.MOVE_DEBUG && owner.getAi2().isLogging()) {
			log.info(I18n.get("log.25ad741e4183", owner.getObjectId(), message));
		}
	}
}
