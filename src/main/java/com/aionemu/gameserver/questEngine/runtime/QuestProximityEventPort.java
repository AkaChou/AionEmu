package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** 权威 NPC 邻近任务事件的生产边界。 / Production boundary for authoritative NPC proximity quest events. */
public interface QuestProximityEventPort {
	QuestEvent.AtDistance atDistance(QuestEnv env, int expectedNpcId);
}
