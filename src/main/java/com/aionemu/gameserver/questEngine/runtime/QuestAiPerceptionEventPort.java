package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.model.gameobjects.Creature;

/** 权威 NPC 仇恨列表回调的边界。 / Boundary for the authoritative NPC aggro-list callback. */
public interface QuestAiPerceptionEventPort {
	QuestEvent.AddAggroList addAggroList(QuestEnv env, int expectedNpcId);

	default QuestEvent.AddAggroList addAggroList(QuestEnv env, int expectedNpcId, Creature aggroSource) {
		return addAggroList(env, expectedNpcId);
	}
}
