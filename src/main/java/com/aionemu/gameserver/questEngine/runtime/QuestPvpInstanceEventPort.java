package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** 每玩家权威副本结算回调的边界。 / Boundary for per-player authoritative instance settlement callbacks. */
public interface QuestPvpInstanceEventPort {
	QuestEvent.DredgionReward dredgionReward(QuestEnv env);
	QuestEvent.KamarReward kamarReward(QuestEnv env);
	QuestEvent.OphidanReward ophidanReward(QuestEnv env);
	QuestEvent.BastionReward bastionReward(QuestEnv env);
}
