package com.aionemu.gameserver.ai2;

import lombok.Getter;

/**
 * 常用 AI 注册名称枚举，避免硬编码字符串。
 * Enumeration of common AI registration names to avoid hard-coded strings.
 * @author ATracer
 */
@Getter
public enum AiNames {

	/** 通用 NPC AI / General NPC AI */
	GENERAL_NPC("general"),
	/** 空壳/哑元 NPC AI / Dummy NPC AI */
	DUMMY_NPC("dummy"),
	/** 主动攻击型 NPC AI / Aggressive NPC AI */
	AGGRESSIVE_NPC("aggressive");

	/**
	 * 获取 AI 注册名称字符串。
	 * Returns the AI registration name string.
	 */
	private final String name;

	AiNames(String name) {
		this.name = name;
	}
}
