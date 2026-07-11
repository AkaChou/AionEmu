package com.aionemu.gameserver.taskmanager.tasks;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;

import static com.aionemu.gameserver.taskmanager.parallel.ForEach.forEach;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.ForkJoinTask;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.model.gameobjects.Creature;
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
	private final Map<Integer, Creature> movingCreatures = new ConcurrentHashMap<Integer, Creature>();

	/**
	 * 移动更新周期（毫秒）。
	 * Movement update period in milliseconds.
	 */
	public static final int UPDATE_PERIOD = 100;

	/**
	 * 单次移动步进谓词：推进目标点，到达则移除并触发 AI 事件。
	 * Per-creature move step: advance destination; on arrival remove and fire AI events.
	 */
	private final Predicate<Creature> CREATURE_MOVE_PREDICATE = new Predicate<Creature>() {
		@Override
		public boolean apply(Creature creature) {
			creature.getMoveController().moveToDestination();
			if (creature.getAi2().poll(AIQuestion.DESTINATION_REACHED)) {
				movingCreatures.remove(creature.getObjectId());
				creature.getAi2().onGeneralEvent(AIEventType.MOVE_ARRIVED);
				GameMovementLoopServices.zoneUpdateService().add(creature);
			} else {
				creature.getAi2().onGeneralEvent(AIEventType.MOVE_VALIDATE);
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
	 * Creature
	 */
	public void addCreature(Creature creature) {
		movingCreatures.put(creature.getObjectId(), creature);
	}

	/**
	 * 将生物移出移动跟踪集合。
	 * Remove a creature from the moving set.
	 *
	 * Creature
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
		final ArrayList<Creature> copy = new ArrayList<Creature>(movingCreatures.values());
		ForkJoinTask<Creature> task = forEach(copy, CREATURE_MOVE_PREDICATE);
		if (task != null) {
			GameThreadPoolServices.threadPoolManager().getForkingPool().invoke(task);
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
	 * Provider
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
