package com.aionemu.gameserver.skillengine.model;

public record ExclusiveAttribute(String name, String tag, int normalPercent, int normalFlat, int skillPercent,
		int skillFlat, int statusImmune) {
}
