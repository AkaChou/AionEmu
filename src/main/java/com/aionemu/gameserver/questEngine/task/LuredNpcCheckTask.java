package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/** Monitors a combat NPC being pulled to a quest destination without replacing its attack AI. */
final class LuredNpcCheckTask implements Runnable {
	private static final float MAX_PLAYER_DISTANCE = 50;

	private final QuestEnv env;
	private final Npc npc;
	private final float x;
	private final float y;
	private final float z;
	private final float radius;
	private final AtomicBoolean finished = new AtomicBoolean();
	private volatile Future<?> task;

	LuredNpcCheckTask(QuestEnv env, Npc npc, float x, float y, float z, float radius) {
		this.env = Objects.requireNonNull(env, "env");
		this.npc = Objects.requireNonNull(npc, "npc");
		if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
				|| !Float.isFinite(radius) || radius <= 0) {
			throw new IllegalArgumentException("lure destination must be finite and radius must be positive");
		}
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = radius;
	}

	void bind(Future<?> task) {
		this.task = Objects.requireNonNull(task, "task");
	}

	@Override
	public void run() {
		Player player = env.getPlayer();
		if (player == null || player.getLifeStats() == null || player.getLifeStats().isAlreadyDead()
				|| npc.getLifeStats() == null || npc.getLifeStats().isAlreadyDead() || !npc.isSpawned()
				|| player.getWorldId() != npc.getWorldId() || player.getInstanceId() != npc.getInstanceId()) {
			lost(player);
			return;
		}
		if (MathUtil.getDistance(npc.getX(), npc.getY(), npc.getZ(), x, y, z) <= radius) {
			reached(player);
			return;
		}
		if (!MathUtil.isIn3dRange(player, npc, MAX_PLAYER_DISTANCE)) {
			lost(player);
		}
	}

	private void reached(Player player) {
		if (!finished.compareAndSet(false, true)) {
			return;
		}
		cancel(player);
		synchronized (npc) {
			if (!npc.isSpawned() || npc.getLifeStats() == null || npc.getLifeStats().isAlreadyDead()) {
				GameEngineServices.questEngine().onNpcLostTarget(env);
				return;
			}
			npc.getController().scheduleRespawn();
			npc.getController().onDelete();
		}
		GameEngineServices.questEngine().onNpcReachTarget(env);
	}

	private void lost(Player player) {
		if (!finished.compareAndSet(false, true)) {
			return;
		}
		cancel(player);
		GameEngineServices.questEngine().onNpcLostTarget(env);
	}

	private void cancel(Player player) {
		if (player != null && player.getController().getTask(TaskId.QUEST_LURE) == task) {
			player.getController().cancelTask(TaskId.QUEST_LURE);
		} else if (task != null) {
			task.cancel(false);
		}
	}
}
