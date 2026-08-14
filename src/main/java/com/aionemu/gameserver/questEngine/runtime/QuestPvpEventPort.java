package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestPvpCreditSource;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** 权威 PvP 任务事件的生产边界。 / Production boundary for authoritative PvP quest events. */
public interface QuestPvpEventPort {
	QuestEvent.KillRanked killRanked(QuestEnv env, Player killer, int victimRankId,
		QuestPvpCreditSource creditSource);

	QuestEvent.KillInWorld killInWorld(QuestEnv env, Player killer, int victimRankId, int worldId,
		QuestPvpCreditSource creditSource);
}
