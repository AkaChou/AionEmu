package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMovementFacts;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** Captures movement facts only after the client/server movement handshake succeeds. */
public final class PlayerQuestMovementEventPort implements QuestMovementEventPort {
	@Override
	public QuestEvent.PassFlyingRing passFlyingRing(QuestEnv env, String ring) {
		Player player = requirePlayer(env);
		return new QuestEvent.PassFlyingRing(ring, facts(player, ring));
	}

	@Override
	public QuestEvent.EnterWindStream enterWindStream(QuestEnv env, int teleportId) {
		Player player = requirePlayer(env);
		if (teleportId <= 0) throw new IllegalArgumentException("wind-stream teleport id must be positive");
		return new QuestEvent.EnterWindStream(teleportId, facts(player, Integer.toString(teleportId)));
	}

	private static Player requirePlayer(QuestEnv env) {
		if (env == null || env.getPlayer() == null) throw new IllegalArgumentException("movement player is required");
		Player player = env.getPlayer();
		if (player.getPosition() == null || !player.isSpawned()) throw new IllegalStateException("movement player must be spawned");
		if (player.getWorldId() <= 0 || player.getInstanceId() <= 0) throw new IllegalStateException("movement world/instance is unavailable");
		return player;
	}

	private static QuestMovementFacts facts(Player player, String actionId) {
		return new QuestMovementFacts(player.getObjectId(), player.getWorldId(), player.getInstanceId(),
			player.getX(), player.getY(), player.getZ(), player.isSpawned(), player.isFlying(), actionId);
	}
}
