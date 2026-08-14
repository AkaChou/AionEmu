package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestPvpInstanceFacts;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** 从真实副本奖励扇出传入的玩家构建结算事实。 / Builds settlement facts from the player passed by a real instance reward fanout. */
public final class PlayerQuestPvpInstanceEventPort implements QuestPvpInstanceEventPort {
	@Override public QuestEvent.DredgionReward dredgionReward(QuestEnv env) {
		return new QuestEvent.DredgionReward(facts(env, "DREDGION"));
	}
	@Override public QuestEvent.KamarReward kamarReward(QuestEnv env) {
		return new QuestEvent.KamarReward(facts(env, "KAMAR"));
	}
	@Override public QuestEvent.OphidanReward ophidanReward(QuestEnv env) {
		return new QuestEvent.OphidanReward(facts(env, "OPHIDAN"));
	}
	@Override public QuestEvent.BastionReward bastionReward(QuestEnv env) {
		return new QuestEvent.BastionReward(facts(env, "BASTION"));
	}

	private static QuestPvpInstanceFacts facts(QuestEnv env, String kind) {
		if (env == null || env.getPlayer() == null) throw new IllegalArgumentException("instance reward player is required");
		Player player = env.getPlayer();
		if (player.getPosition() == null || !player.isSpawned()
				|| player.getWorldId() <= 0 || player.getInstanceId() <= 0) {
			throw new IllegalStateException("instance reward player world/instance is unavailable");
		}
		return QuestPvpInstanceFacts.settled(player.getObjectId(), kind, player.getWorldId(), player.getInstanceId());
	}
}
