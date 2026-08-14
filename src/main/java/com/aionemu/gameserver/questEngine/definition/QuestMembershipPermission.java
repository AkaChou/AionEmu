package com.aionemu.gameserver.questEngine.definition;

import java.util.Locale;

/**
 * 任务定义可消费的类型化成员权限。
 * Typed membership capabilities that may be consumed by quest definitions.
 *
 * <p>The wire name is intentionally closed: quest XML must not be able to
 * invent a permission whose runtime source has not been captured.</p>
 */
public enum QuestMembershipPermission {
	STIGMA_SLOT_QUEST;

	public static QuestMembershipPermission fromWire(String wire) {
		if (wire == null || wire.isBlank()) {
			throw new IllegalArgumentException("membership permission must not be blank");
		}
		try {
			return valueOf(wire.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException invalid) {
			throw new IllegalArgumentException("unsupported membership permission: " + wire, invalid);
		}
	}
}
