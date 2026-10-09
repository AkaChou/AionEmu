package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 原版「系统发放」任务的分发器（M5-b3x 定义，P0c 接线）。
 * <p>
 * 类别哨兵（{@code _faction_} / {@code _challengetask_} / {@code _area_}）任务的接取名不是 NPC，
 * 原版由子系统发放而不是 NPC 对话接取，因此这类定义只有一条 {@link QuestEvent.SystemGrant} 边
 * （NONE → START），没有接取路由。旧引擎里「无手势发放」已有先例：{@code RetailAreaEngine} 进入
 * {@code quest_area} 时直接调用 {@code QuestService.startQuest}。
 * <p>
 * 本类把同一语义推广给阵营日常轮换：分配即发放（服务端直接进 START，客户端收到 addQuest）。
 * 只有含 {@code SystemGrant} 边的定义会被发放，其余（XML 驱动或带接取路由的原版定义）行为不变。
 * <p>
 * Dispatcher for retail system-granted quests: definitions that carry a {@code SystemGrant} edge are
 * started directly by the granting subsystem (faction daily rotation today), mirroring the existing
 * {@code RetailAreaEngine} quest-area grant. Definitions without that edge are never touched.
 * @author AionEmu
 */
public final class RetailSystemGrantDispatcher {
	/** {@link QuestEvent.SystemGrant#type()} 的类型串。 / Event-type string of {@code SystemGrant}. */
	private static final String SYSTEM_GRANT_TYPE = new QuestEvent.SystemGrant().type();

	private RetailSystemGrantDispatcher() {
	}

	/**
	 * 判断给定任务的定义是否含系统发放边。
	 * Returns whether the given quest definition carries a system-grant edge.
	 * @param catalog 任务目录 / quest catalog
	 * @param questId 任务 ID / quest id
	 * @return 含 {@code SystemGrant} 边时为 true / true when a {@code SystemGrant} edge exists
	 */
	public static boolean isSystemGranted(QuestCatalog catalog, int questId) {
		if (catalog == null || questId <= 0) {
			return false;
		}
		return catalog.findExecutable(questId)
			.map(compiled -> !compiled.transitionsFor(SYSTEM_GRANT_TYPE).isEmpty())
			.orElse(false);
	}

	/**
	 * 若该任务是系统发放任务且玩家尚未开始，则直接发放。
	 * Grants the quest directly when it is system-granted and the player has not started it yet.
	 * @param player 玩家 / player
	 * @param questId 任务 ID / quest id
	 * @return 实际发放成功时为 true / true when the quest was granted
	 */
	public static boolean grantIfSystemGranted(Player player, int questId) {
		if (player == null || questId <= 0) {
			return false;
		}
		QuestEngine questEngine = GameEngineServices.questEngine();
		if (questEngine == null || !isSystemGranted(questEngine.questCatalog(), questId)) {
			return false;
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state != null && state.getStatus() != QuestStatus.NONE) {
			// 已有进度（START/REWARD/COMPLETE）：不重复发放。
			// Already in progress: never re-grant.
			return false;
		}
		return QuestService.startQuest(new QuestEnv(null, player, questId, 0));
	}
}
