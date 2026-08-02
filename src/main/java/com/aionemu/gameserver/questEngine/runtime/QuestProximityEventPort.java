package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** Production boundary for authoritative NPC proximity quest events. */
public interface QuestProximityEventPort {
	QuestEvent.AtDistance atDistance(QuestEnv env, int expectedNpcId);
}
