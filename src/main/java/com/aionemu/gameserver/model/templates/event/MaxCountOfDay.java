package com.aionemu.gameserver.model.templates.event;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

/**
 * 每日最大计数模板（静态数据/XML）。
 * Max Count Of Day Template (static data/XML).
 */

@AllArgsConstructor
public class MaxCountOfDay {
	/** 返回当前次数 / Returns the this count */
	@Getter
	@Setter
	private int thisCount;
}
