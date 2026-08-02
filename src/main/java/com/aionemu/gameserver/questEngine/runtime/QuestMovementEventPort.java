package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** Boundary for server-accepted flight-ring and wind-stream actions. */
public interface QuestMovementEventPort {
	QuestEvent.PassFlyingRing passFlyingRing(QuestEnv env, String ring);
	QuestEvent.EnterWindStream enterWindStream(QuestEnv env, int teleportId);
}
