package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** 房屋所属物品交互的边界。 / Boundary for a house-owned object interaction. */
public interface QuestHousingEventPort {
	QuestEvent.HouseItemUse houseItemUse(QuestEnv env, int itemTemplateId);

	default QuestEvent.HouseItemUse houseItemUse(QuestEnv env, int itemTemplateId, int itemObjectId) {
		return houseItemUse(env, itemTemplateId);
	}
}
