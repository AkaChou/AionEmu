package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestProximityFacts;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

/**
 * Captures NPC proximity facts from the actual AI callback.
 *
 * <p>The port is deliberately fail-closed: a missing target/player, a stale
 * template id, a cross-world/instance callback, or a distance outside the
 * legacy 20-unit contract cannot create a typed event.</p>
 */
public final class PlayerQuestProximityEventPort implements QuestProximityEventPort {
	public static final float DEFAULT_MAXIMUM_DISTANCE = 20f;

	private final BiPredicate<Npc, Player> rangeCheck;
	private final ToIntFunction<com.aionemu.gameserver.model.gameobjects.VisibleObject> instanceResolver;

	public PlayerQuestProximityEventPort() {
		this((npc, player) -> MathUtil.isIn3dRange(npc, player, DEFAULT_MAXIMUM_DISTANCE),
			com.aionemu.gameserver.model.gameobjects.VisibleObject::getInstanceId);
	}

	PlayerQuestProximityEventPort(BiPredicate<Npc, Player> rangeCheck) {
		this(rangeCheck, com.aionemu.gameserver.model.gameobjects.VisibleObject::getInstanceId);
	}

	PlayerQuestProximityEventPort(BiPredicate<Npc, Player> rangeCheck,
		ToIntFunction<com.aionemu.gameserver.model.gameobjects.VisibleObject> instanceResolver) {
		this.rangeCheck = Objects.requireNonNull(rangeCheck, "rangeCheck");
		this.instanceResolver = Objects.requireNonNull(instanceResolver, "instanceResolver");
	}

	@Override
	public QuestEvent.AtDistance atDistance(QuestEnv env, int expectedNpcId) {
		if (expectedNpcId <= 0) {
			throw new IllegalArgumentException("expectedNpcId must be positive");
		}
		if (env == null || !(env.getVisibleObject() instanceof Npc npc) || env.getPlayer() == null) {
			throw new IllegalArgumentException("proximity target and recipient are required");
		}
		Player player = env.getPlayer();
		if (npc.getNpcId() != expectedNpcId) {
			throw new IllegalArgumentException("proximity target template does not match the route");
		}
		if (npc.getPosition() == null || player.getPosition() == null
				|| !npc.isSpawned() || !player.isSpawned()) {
			throw new IllegalStateException("proximity target and recipient must be spawned");
		}
		int targetWorldId = npc.getWorldId();
		int recipientWorldId = player.getWorldId();
		int targetInstanceId = instanceResolver.applyAsInt(npc);
		int recipientInstanceId = instanceResolver.applyAsInt(player);
		if (targetWorldId != recipientWorldId || targetInstanceId != recipientInstanceId) {
			throw new IllegalArgumentException("proximity target and recipient are not in the same world/instance");
		}
		if (!rangeCheck.test(npc, player)) {
			throw new IllegalArgumentException("proximity recipient is outside the 20-unit range");
		}
		double distance = MathUtil.getDistance(npc, player);
		QuestProximityFacts facts = new QuestProximityFacts(player.getObjectId(), npc.getObjectId(),
			expectedNpcId, recipientWorldId, targetWorldId, recipientInstanceId, targetInstanceId,
			distance, DEFAULT_MAXIMUM_DISTANCE);
		return new QuestEvent.AtDistance(expectedNpcId, facts);
	}

}
