package com.aionemu.gameserver.model.templates.event;

/**
 * Max 计数 OfDay 模板（静态数据/XML）。
 * XML template.
 */

public class MaxCountOfDay {
	private int thisCount;

	public MaxCountOfDay(int thisCount) {
		this.thisCount = thisCount;
	}

	/** 返回数量 / Returns the this count*/
	public int getThisCount() {
		return thisCount;
	}

	/** 设置数量 / Sets the this count*/
	public void setThisCount(int thisCount) {
		this.thisCount = thisCount;
	}
}
