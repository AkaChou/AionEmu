package com.aionemu.gameserver.model.gameobjects;

import java.util.HashMap;
import java.util.Map;

/**
 * 宠物动作枚举。
 * Pet Action enumeration.
 *
 * @author ATracer
 */
public enum PetAction {
	/** 收养 / Adopt. */
	ADOPT(1), SURRENDER(2), SPAWN(3), DISMISS(4), TALK_WITH_MERCHANT(6), TALK_WITH_MINDER(7), FOOD(9), RENAME(10),
	/** 心情 / Mood. */
	MOOD(12), UNKNOWN(255);

	private static Map<Integer, PetAction> petActions;

	static {
		petActions = new HashMap<Integer, PetAction>();
		for (PetAction action : values()) {
			petActions.put(action.getActionId(), action);
		}
	}

	private int actionId;

	private PetAction(int actionId) {
		this.actionId = actionId;
	}

	/** 返回动作 ID / Returns the action id */
	public int getActionId() {
		return actionId;
	}

	/** 按 ID 返回动作 / Returns the action by id */
	public static PetAction getActionById(int actionId) {
		PetAction action = petActions.get(actionId);
		return action != null ? action : UNKNOWN;
	}
}
