package com.aionemu.gameserver.taskmanager.tasks;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;

import static com.aionemu.gameserver.taskmanager.parallel.ForEach.forEach;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.ForkJoinTask;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.taskmanager.AbstractPeriodicTaskManager;
import com.google.common.base.Predicate;

/**
 * 生物移动推进任务：周期性并行更新移动中生物的目标点与到达事件。
 * Creature movement advancement task: periodically updates moving creatures' destinations and arrival events in parallel.
 */
public class MoveTaskManager extends AbstractPeriodicTaskManager {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<MoveTaskManager> instanceProvider;

	/**
	 * 正在移动的生物（objectId → 生物）。
	 * Creatures currently moving (objectId → creature).
	 */
	private final Map<Integer, MoveRegistration> movingCreatures = new ConcurrentHashMap<>();

	/**
	 * 移动更新周期（毫秒）。
	 * Movement update period in milliseconds.
	 */
	public static final int UPDATE_PERIOD = 100;
	private static final int MID_DISTANCE_UPDATE_PERIOD = 200;
	private static final int FAR_DISTANCE_UPDATE_PERIOD = 500;
	private static final float NEAR_PLAYER_DISTANCE_SQUARED = 30 * 30;
	private static final float MID_PLAYER_DISTANCE_SQUARED = 60 * 60;

	/**
	 * 单次移动步进谓词：推进目标点，到达则移除并触发 AI 事件。
	 * Per-creature move step: advance destination; on arrival remove and fire AI events.
	 */
	private final Predicate<MoveRegistration> CREATURE_MOVE_PREDICATE = new Predicate<MoveRegistration>() {
		@Override
		public boolean apply(MoveRegistration registration) {
			Creature creature = registration.creature();
			int objectId = creature.getObjectId();
			if (movingCreatures.get(objectId) != registration) {
				return true;
			}
			long now = System.currentTimeMillis();
			if (now < registration.nextUpdateAt) {
				return true;
			}
			registration.processing = true;
			try {
				creature.getMoveController().moveToDestination();
				if (movingCreatures.get(objectId) != registration) {
					return true;
				}
				if (creature.getAi2().poll(AIQuestion.DESTINATION_REACHED)) {
					if (movingCreatures.remove(objectId, registration)) {
						creature.getAi2().onGeneralEvent(AIEventType.MOVE_ARRIVED);
						GameMovementLoopServices.zoneUpdateService().add(creature);
					}
				} else {
					creature.getAi2().onGeneralEvent(AIEventType.MOVE_VALIDATE);
				}
			} finally {
				registration.nextUpdateAt = now + movementUpdatePeriod(creature);
				registration.processing = false;
			}
			return true;
		}
	};

	/**
	 * 以 {@link #UPDATE_PERIOD} 周期构造移动任务管理器。
	 * Construct the move task manager with {@link #UPDATE_PERIOD}.
	 */
	public MoveTaskManager() {
		super(UPDATE_PERIOD);
	}

	/**
	 * 将生物加入移动跟踪集合。
	 * Add a creature to the moving set.
	 *
	 * @param creature 生物 / Creature
	 */
	public void addCreature(Creature creature) {
		int objectId = creature.getObjectId();
		while (true) {
			MoveRegistration current = movingCreatures.get(objectId);
			if (current != null && current.creature == creature && !current.processing) {
				current.nextUpdateAt = 0;
				return;
			}
			MoveRegistration replacement = new MoveRegistration(creature);
			if (current == null) {
				if (movingCreatures.putIfAbsent(objectId, replacement) == null) {
					return;
				}
			} else if (movingCreatures.replace(objectId, current, replacement)) {
				return;
			}
		}
	}

	private static int movementUpdatePeriod(Creature creature) {
		if (!(creature instanceof Npc) || creature.getKnownList() == null) {
			return UPDATE_PERIOD;
		}
		return movementUpdatePeriod(GeoDataConfig.GEO_PATH_DISTANCE_TIERS_ENABLE, creature.getAi2().getState(),
				creature.getKnownList().getNearestKnownPlayerDistanceSquared());
	}

	static int movementUpdatePeriod(boolean distanceTiersEnabled, AIState state, float nearestPlayerDistanceSquared) {
		if (!distanceTiersEnabled || state != AIState.IDLE && state != AIState.WALKING
				|| nearestPlayerDistanceSquared <= NEAR_PLAYER_DISTANCE_SQUARED) {
			return UPDATE_PERIOD;
		}
		return nearestPlayerDistanceSquared <= MID_PLAYER_DISTANCE_SQUARED
				? MID_DISTANCE_UPDATE_PERIOD : FAR_DISTANCE_UPDATE_PERIOD;
	}

	/**
	 * 将生物移出移动跟踪集合。
	 * Remove a creature from the moving set.
	 *
	 * @param creature 生物 / Creature
	 */
	public void removeCreature(Creature creature) {
		movingCreatures.remove(creature.getObjectId());
	}

	/**
	 * 快照当前移动生物并在 ForkJoin 池上并行步进。
	 * Snapshot current movers and advance them in parallel on the ForkJoin pool.
	 */
	@Override
	public void run() {
		long now = System.currentTimeMillis();
		final ArrayList<MoveRegistration> copy = new ArrayList<>();
		for (MoveRegistration registration : movingCreatures.values()) {
			if (now >= registration.nextUpdateAt) {
				copy.add(registration);
			}
		}
		ForkJoinTask<MoveRegistration> task = forEach(copy, CREATURE_MOVE_PREDICATE);
		if (task != null) {
			GameThreadPoolServices.threadPoolManager().getForkingPool().invoke(task);
		}
	}

	private static final class MoveRegistration {

		private final Creature creature;
		private volatile boolean processing;
		private volatile long nextUpdateAt;

		private MoveRegistration(Creature creature) {
			this.creature = creature;
		}

		private Creature creature() {
			return creature;
		}
	}

	/**
	 * 获取单例：优先 Spring 提供者，否则静态 holder。
	 * Get the singleton: prefer Spring provider, otherwise the static holder.
	 *
	 * @return 管理器实例 / Manager instance
	 */
	public static MoveTaskManager getInstance() {
		ObjectProvider<MoveTaskManager> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param provider 实例提供者 / Provider
	 */
	public static void setInstanceProvider(ObjectProvider<MoveTaskManager> provider) {
		instanceProvider = provider;
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	private static final class SingletonHolder {

		/**
		 * 默认单例实例。
		 * Default singleton instance.
		 */
		private static final MoveTaskManager INSTANCE = new MoveTaskManager();
	}
}
