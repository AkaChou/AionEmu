package com.aionemu.gameserver.model.gameobjects.player;

/**
 * Revive 类型枚举。
 * Revive Type enumeration.
 */

public enum ReviveType {
	/** 绑定点复活 / Bind Revive */
	BIND_REVIVE(0), REBIRTH_REVIVE(1), ITEM_SELF_REVIVE(2), SKILL_REVIVE(3), KISK_REVIVE(4), INSTANCE_REVIVE(6),
	/** 旋涡复活 / Vortex Revive */
	VORTEX_REVIVE(8), START_POINT_REVIVE(11);

	private int typeId;

	private ReviveType(int typeId) {
		this.typeId = typeId;
	}

	/** 返回 revive type id / Returns the revive type id */
	public int getReviveTypeId() {
		return typeId;
	}

	/** 按 ID 返回 revive type / Returns the revive type by id */
	public static ReviveType getReviveTypeById(int id, Player pl) {
		for (ReviveType rt : values()) {
			if (rt.typeId == id) {
				return rt;
			}
		}
		throw new IllegalArgumentException("Unsupported revive type: " + id);
	}
}
