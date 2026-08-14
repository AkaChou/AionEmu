package com.aionemu.gameserver.skillengine.model;

/**
 * 独占属性记录：普通/技能百分比与固定值，以及状态免疫豁免。
 * Exclusive attribute record: normal/skill percent and flat values, plus status-immune exemption.
 */
public record ExclusiveAttribute(String name, String tag, int normalPercent, int normalFlat, int skillPercent,
		int skillFlat, int statusImmune) {
}
