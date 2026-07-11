package com.aionemu.gameserver.model;

/**
 * 请愿状态枚举。
 * Petition Status enumeration.
 *
 * @author zdead
 */
public enum PetitionStatus {
	/** 待处理 / Pending. */
	PENDING(0), IN_PROGRESS(1), REPLIED(2);

	private int element;

	private PetitionStatus(int id) {
		this.element = id;
	}

	/** 返回元素 ID / Returns the element id */
	public int getElementId() {
		return element;
	}
}
