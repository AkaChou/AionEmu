package com.aionemu.gameserver.questEngine.handlers;

/**
 * 任务处理器链式调用的结果枚举。
 * Result of a quest-handler chain invocation.
 *
 * <p>UNKNOWN 表示本处理器未处理该事件，允许后续处理器继续；
 * FAILED 表示已处理并给出明确结果。</p>。
 * <p>UNKNOWN means this handler did not process the event and others may continue;
 * FAILED means a definitive outcome was produced.</p>
 *
 * @author Rolandas
 */
public enum HandlerResult {
	/** 未处理，允许其他处理器继续 / Not handled; allow other handlers to process */
	UNKNOWN,
	/** 处理成功 / Handled successfully */
	SUCCESS,
	/** 处理失败 / Handled but failed */
	FAILED;

	/**
	 * 将可空布尔值转换为处理器结果。
	 * Convert a nullable Boolean into a handler result.
	 *
	 * @param value 布尔结果；null 表示未知 / Boolean outcome; {@code null} means unknown
	 * @return Matching {@link HandlerResult}。
	 */
	public static HandlerResult fromBoolean(Boolean value) {
		if (value == null) {
			return HandlerResult.UNKNOWN;
		} else if (value) {
			return HandlerResult.SUCCESS;
		}
		return HandlerResult.FAILED;
	}
}
