package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestRecoveryFacts;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** 捕获登出事实，并只拥有类型化运行时资源清理。 / Captures logout facts and owns only typed runtime-resource cleanup. */
public final class PlayerQuestRecoveryEventPort implements QuestRecoveryEventPort {
	@Override
	public QuestEvent.LogOut logOut(QuestEnv env) {
		if (env == null || env.getPlayer() == null) throw new IllegalArgumentException("logout player is required");
		Player player = env.getPlayer();
		boolean positioned = player.getPosition() != null;
		QuestRecoveryFacts facts = new QuestRecoveryFacts(player.getObjectId(),
			positioned ? Math.max(0, player.getWorldId()) : 0,
			positioned ? Math.max(0, player.getInstanceId()) : 0,
			positioned && player.isSpawned(), true);
		return new QuestEvent.LogOut(facts);
	}

	@Override
	public void recover(QuestEnv env) {
		if (env == null || env.getPlayer() == null) return;
		QuestRuntimeResources.cleanupPlayer(env.getPlayer().getObjectId());
	}
}
