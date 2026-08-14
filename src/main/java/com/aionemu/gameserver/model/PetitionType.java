package com.aionemu.gameserver.model;

/**
 * 请愿类型枚举。
 * Petition Type enumeration.
 *
 * @author zdead
 */
public enum PetitionType {
	/** 角色卡住 / Character Stuck */
	CHARACTER_STUCK(256),
	/** 角色恢复 / Character Restoration */
	CHARACTER_RESTORATION(512),
	/** 错误 / Bug */
	BUG(768),
	/** 任务 / Quest */
	QUEST(1024),
	/** 不当行为 / Unacceptable Behavior */
	UNACCEPTABLE_BEHAVIOR(1280),
	/** 建议 / Suggestion. */
	SUGGESTION(1536),
	/** 咨询 / Inquiry */
	INQUIRY(65280);

	private int element;

	private PetitionType(int id) {
		this.element = id;
	}

	/** 返回元素 ID / Returns the element id */
	public int getElementId() {
		return element;
	}
}
