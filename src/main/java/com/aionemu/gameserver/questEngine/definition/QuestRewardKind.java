package com.aionemu.gameserver.questEngine.definition;

import java.util.Locale;

/**
 * 正式 typed runtime 可执行的封闭奖励类型。
 * Closed reward kinds executable by the typed runtime.
 */
public enum QuestRewardKind {
	ITEM,
	SELECTABLE_ITEM,
	RANDOM,
	GOLD,
	KINAH,
	EXP,
	EXP_BOOST,
	AP,
	GP,
	DP,
	CP,
	ABYSS_OP,
	TITLE,
	AURA_OF_GROWTH,
	EXTEND_INVENTORY,
	EXTEND_STIGMA;

	public static QuestRewardKind fromWire(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("reward kind must not be blank");
		}
		try {
			return valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("unknown quest reward kind: " + value, e);
		}
	}

	public boolean isCurrency() {
        switch (this) {
            case GOLD:
            case KINAH:
            case AP:
            case GP:
            case DP:
            case CP:
            case ABYSS_OP:
                return true;
            default:
                return false;
        }
	}
}
