package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** Boundary for logout recovery and cancellation of resources owned by typed quests. */
public interface QuestRecoveryEventPort {
	QuestEvent.LogOut logOut(QuestEnv env);
	void recover(QuestEnv env);
}
