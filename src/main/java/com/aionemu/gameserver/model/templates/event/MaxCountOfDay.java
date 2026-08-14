package com.aionemu.gameserver.model.templates.event;

/**
 * 每日最大计数模板（静态数据/XML）。
 * Max Count Of Day Template (static data/XML).
 */

public class MaxCountOfDay {
	private int thisCount;

	public MaxCountOfDay(int thisCount) {
		this.thisCount = thisCount;
	}

	/** 返回当前次数 / Returns the this count */
	public int getThisCount() {
		return thisCount;
	}

	/** 设置当前次数 / Sets the this count */
	public void setThisCount(int thisCount) {
		this.thisCount = thisCount;
	}
}
