package com.aionemu.gameserver.taskmanager.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.ai.RetailSensoryAreaEngine;
import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.taskmanager.AbstractFIFOPeriodicTaskManager;
import com.aionemu.gameserver.world.knownlist.VisitorWithOwner;

/**
 * 移动通知任务：对已知列表中的 NPC 广播生物移动事件，并统计广播次数。
 * Movement-notify task: broadcasts creature-moved events to known NPCs and tracks broadcast counts.
 */
public class MovementNotifyTask extends AbstractFIFOPeriodicTaskManager<Creature> {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
     * -- SETTER --
     *  注入 Spring 实例提供者。
     *  Inject the Spring instance provider.
     */
	@Setter
    private static volatile ObjectProvider<MovementNotifyTask> instanceProvider;

	/**
	 * 各地图移动广播峰值统计（[最大次数, Npc 模板 Id]）。
	 * Per-map movement-broadcast peak stats ([max count, Npc template id]).
	 */
	private static final Map<Integer, int[]> moveBroadcastCounts = new HashMap<>();

	/**
	 * 广播统计表是否已按世界地图模板初始化。
	 * Whether broadcast stats were initialized from world-map templates.
	 */
	private static boolean moveBroadcastCountsInitialized;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static MovementNotifyTask getInstance() {
		ObjectProvider<MovementNotifyTask> provider = instanceProvider;
		MovementNotifyTask provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("MovementNotifyTask 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

    /**
	 * 向已知 NPC 投递移动事件的访问器。
	 * Visitor that delivers move events to known NPCs.
	 */
	private final MoveNotifier MOVE_NOTIFIER = new MoveNotifier();

	/**
	 * 以 500ms 周期构造移动通知任务。
	 * Construct the movement-notify task with a 500ms period.
	 */
	public MovementNotifyTask() {
		super(500);
		ensureMoveBroadcastCountsInitialized();
	}

	/**
	 * 对存活生物的已知 NPC 广播 {@link AIEventType#CREATURE_MOVED}（部分地图限流）。
	 * Broadcast {@link AIEventType#CREATURE_MOVED} to known NPCs of a living creature (rate-limited on some maps).
	 * @param creature 移动中的生物 / Moving creature
	 */
	@Override
	protected void callTask(Creature creature) {
		if (creature.getLifeStats().isAlreadyDead()) {
			return;
		}
		int limit = creature.getWorldId() == 400010000 || // Reshanta.
				creature.getWorldId() == 400020000 || // 贝洛斯 / Belus.
				creature.getWorldId() == 400030000 || // Transidium Annex.
				creature.getWorldId() == 400040000 || // Aspida.
				creature.getWorldId() == 400050000 || // Atanatos.
				creature.getWorldId() == 400060000 ? 200 : Integer.MAX_VALUE; // Disillon.
		int iterations = creature.getKnownList().doOnAllNpcsWithOwner(MOVE_NOTIFIER, limit);
		if (creature instanceof Player player) {
			RetailSensoryAreaEngine.onPlayerMoved(player);
		}
		if (!(creature instanceof Player)) {
			int[] maxCounts = moveBroadcastCounts(creature.getWorldId());
			synchronized (maxCounts) {
				if (iterations > maxCounts[0]) {
					maxCounts[0] = iterations;
					maxCounts[1] = creature.getObjectTemplate().getTemplateId();
				}
			}
		}
	}

	/**
	 * 导出各地图移动广播峰值诊断行。
	 * Dump diagnostic lines of per-map movement-broadcast peaks.
	 * @return 诊断文本行 / Diagnostic text lines
	 */
	public String[] dumpBroadcastStats() {
		ensureMoveBroadcastCountsInitialized();
		List<String> lines = new ArrayList<>();
		lines.add("------- Movement broadcast counts -------");
		for (Entry<Integer, int[]> entry : moveBroadcastCounts.entrySet()) {
			lines.add(
					"WorldId=" + entry.getKey() + ": " + entry.getValue()[0] + " (NpcId " + entry.getValue()[1] + ")");
		}
		lines.add("-----------------------------------------");
		return lines.toArray(new String[0]);
	}

	/**
	 * 耗时统计方法名。
	 * Method name for runtime stats.
	 * @return 方法名 / Method name
	 */
	@Override
	protected String getCalledMethodName() {
		return "notifyOnMove()";
	}

	/**
	 * 获取或创建指定地图的广播统计槽位。
	 * Get or create the broadcast-stats slot for a map.
	 * @param worldId 世界地图 ID / World map id
	 * @return 统计数组 / Stats array
	 */
	private static int[] moveBroadcastCounts(int worldId) {
		synchronized (moveBroadcastCounts) {
			ensureMoveBroadcastCountsInitialized();
			return moveBroadcastCounts.computeIfAbsent(worldId, key -> new int[2]);
		}
	}

	/**
	 * 按世界地图模板惰性初始化广播统计表。
	 * Lazily initialize broadcast stats from world-map templates.
	 */
	private static void ensureMoveBroadcastCountsInitialized() {
		synchronized (moveBroadcastCounts) {
			if (moveBroadcastCountsInitialized || DataManager.WORLD_MAPS_DATA == null) {
				return;
			}
			for (WorldMapTemplate template : DataManager.WORLD_MAPS_DATA) {
				moveBroadcastCounts.putIfAbsent(template.getMapId(), new int[2]);
			}
			moveBroadcastCountsInitialized = true;
		}
	}

	/**
	 * 已知列表访问器：向存活 NPC 的 AI 投递生物移动事件。
	 * Known-list visitor: deliver creature-moved events to living NPC AIs.
	 */
	private class MoveNotifier implements VisitorWithOwner<Npc, VisibleObject> {

		/**
		 * 访问一个 NPC：若仍存活则触发 {@link AIEventType#CREATURE_MOVED}。
		 * Visit one NPC: if still alive, fire {@link AIEventType#CREATURE_MOVED}.
		 * @param object 目标 NPC / Target NPC
		 * @param owner  移动源对象 / Moving owner object
		 */
		@Override
		public void visit(Npc object, VisibleObject owner) {
			if (object.getAi2().getState() == AIState.DIED || object.getLifeStats().isAlreadyDead()) {
				if (object.getAi2().isLogging()) {
					AI2Logger.moveinfo(object, "WARN: NPC died but still in knownlist");
				}
				return;
			}
			object.getAi2().onCreatureEvent(AIEventType.CREATURE_MOVED, (Creature) owner);
		}
	}
}
