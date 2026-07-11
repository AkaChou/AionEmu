package com.aionemu.gameserver.model.gameobjects;

/**
 * 住房动作枚举。
 * Housing Action enumeration.
 */

public enum HousingAction {
	/** 未知 / Unk. */
	UNK(-1), ENTER_DECORATION(1), EXIT_DECORATION(2), ADD_ITEM(3), DELETE_ITEM(4), SPAWN_OBJECT(5), MOVE_OBJECT(6),
	/** 消失对象 / Despawn Object*/
	DESPAWN_OBJECT(7), ENTER_RENOVATION(14), EXIT_RENOVATION(15), CHANGE_APPEARANCE(16);

	private int id;

	private HousingAction(int id) {
		this.id = id;
	}

	/** 返回类型 ID / Returns the type id */
	public int getTypeId() {
		return id;
	}

	/** 按 ID 返回 action type / Returns the action type by id */
	public static HousingAction getActionTypeById(int id) {
		for (HousingAction actionType : values()) {
			if (actionType.getTypeId() == id) {
				return actionType;
			}
		}
		return UNK;
	}
}
