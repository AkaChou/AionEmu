package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestAiPerceptionFacts;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToIntFunction;

/** 在仇恨列表回调边界捕获 NPC/接受者事实。 / Captures the NPC/recipient facts at the aggro-list callback boundary. */
public final class PlayerQuestAiPerceptionEventPort implements QuestAiPerceptionEventPort {
	private final BiPredicate<Creature, Npc> hostileSource;
	private final ToDoubleBiFunction<Npc, Player> distance;
	private final ToIntFunction<Npc> aggroRange;

	public PlayerQuestAiPerceptionEventPort() {
		this((source, npc) -> source.isEnemy(npc), MathUtil::getDistance,
			npc -> Math.max(1, npc.getAggroRange()));
	}

	PlayerQuestAiPerceptionEventPort(BiPredicate<Creature, Npc> hostileSource,
			ToDoubleBiFunction<Npc, Player> distance, ToIntFunction<Npc> aggroRange) {
		this.hostileSource = Objects.requireNonNull(hostileSource, "hostileSource");
		this.distance = Objects.requireNonNull(distance, "distance");
		this.aggroRange = Objects.requireNonNull(aggroRange, "aggroRange");
	}

	@Override
	public QuestEvent.AddAggroList addAggroList(QuestEnv env, int expectedNpcId) {
		return addAggroList(env, expectedNpcId, null);
	}

	@Override
	public QuestEvent.AddAggroList addAggroList(QuestEnv env, int expectedNpcId, Creature aggroSource) {
		if (expectedNpcId <= 0) throw new IllegalArgumentException("expectedNpcId must be positive");
		if (env == null || !(env.getVisibleObject() instanceof Npc npc) || env.getPlayer() == null) {
			throw new IllegalArgumentException("AI perception target and recipient are required");
		}
		Player player = env.getPlayer();
		if (npc.getNpcId() != expectedNpcId) throw new IllegalArgumentException("AI perception route does not match NPC");
		if (npc.getPosition() == null || player.getPosition() == null || !npc.isSpawned() || !player.isSpawned()) {
			throw new IllegalStateException("AI perception target and recipient must be spawned");
		}
		if (npc.getWorldId() != player.getWorldId() || npc.getInstanceId() != player.getInstanceId()) {
			throw new IllegalArgumentException("AI perception target and recipient are not in the same world/instance");
		}
		if (aggroSource == null || aggroSource.getObjectId() <= 0 || aggroSource.getPosition() == null
				|| !aggroSource.isSpawned()
				|| !hostileSource.test(aggroSource, npc) || aggroSource.getWorldId() != npc.getWorldId()
				|| aggroSource.getInstanceId() != npc.getInstanceId()) {
			throw new IllegalArgumentException("AI perception aggro source is not authoritative");
		}
		double distance = this.distance.applyAsDouble(npc, player);
		int aggroRange = this.aggroRange.applyAsInt(npc);
		if (aggroRange <= 0) throw new IllegalStateException("AI perception aggro range is unavailable");
		if (distance > aggroRange) throw new IllegalArgumentException("AI perception recipient is outside aggro range");
		QuestAiPerceptionFacts facts = new QuestAiPerceptionFacts(player.getObjectId(), npc.getObjectId(),
			expectedNpcId, aggroSource.getObjectId(), true, true, player.getWorldId(), npc.getWorldId(),
			player.getInstanceId(), npc.getInstanceId(), distance, aggroRange,
			player.getLifeStats() != null && !player.getLifeStats().isAlreadyDead(), true);
		return new QuestEvent.AddAggroList(expectedNpcId, facts);
	}
}
