package com.aionemu.gameserver.model.siege;

import lombok.Getter;

/**
 * 要塞类型枚举。
 * Siege Type enumeration.
 */

@Getter
public enum SiegeType {
	/** 要塞。 / Fortress. */
	FORTRESS(0),
	/** 神器。 / Artifact. */
	ARTIFACT(1),
	/** 光之首领突袭。 / Boss Raid Light. */
	BOSSRAID_LIGHT(2),
	/** 暗之首领突袭。 / Boss Raid Dark. */
	BOSSRAID_DARK(3),
	/** 副本。 / Indun. */
	INDUN(4),
	/** 地下通道。 / Underpass. */
	UNDERPASS(5),
	/** 塔。 / Tower. */
	TOWER(6);

	/** 返回类型 ID / Returns the type id */
	private final int typeId;

	SiegeType(int id) {
		this.typeId = id;
	}
}
