package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;

import java.util.List;
import java.util.Objects;

/**
 * 提交成功后允许执行的尽力而为动作的封闭集合。
 * Closed set of best-effort actions allowed after a successful commit.
 */
public sealed interface AfterCommitAction permits AfterCommitAction.CloseDialog,
		AfterCommitAction.ShowQuestDialog, AfterCommitAction.ShowQuestSelectionDialog,
		AfterCommitAction.ShowDialogWindow,
		AfterCommitAction.TeleportPlayer, AfterCommitAction.PlayMovie,
		AfterCommitAction.SpawnNpc, AfterCommitAction.SpawnNpcRandom, AfterCommitAction.DespawnNpc, AfterCommitAction.StartFollow,
		AfterCommitAction.StopFollow, AfterCommitAction.AttackTarget, AfterCommitAction.AttackNpcTemplate, AfterCommitAction.StartWalking,
		AfterCommitAction.StartFollowCurrentTargetToPoint, AfterCommitAction.StartFollowCurrentTargetToNpc,
		AfterCommitAction.BroadcastNpcEmotion, AfterCommitAction.BroadcastInteractionNpcEmotion,
		AfterCommitAction.WatchFollowZone, AfterCommitAction.WatchFollowCoordinate,
		AfterCommitAction.WatchLuredNpcCoordinate,
		AfterCommitAction.StartQuestTimer, AfterCommitAction.StartInvisibleTimer,
		AfterCommitAction.CancelQuestTimer, AfterCommitAction.SyncQuestState,
		AfterCommitAction.RefreshPlayerStats, AfterCommitAction.Morph,
		AfterCommitAction.SetPlayerClass,
		AfterCommitAction.ApplyEffect, AfterCommitAction.RemoveEffect,
		AfterCommitAction.SendSystemMessage, AfterCommitAction.SendSystemMessagePacket,
		AfterCommitAction.FlightTeleport, AfterCommitAction.PlayerEmotion,
		AfterCommitAction.StartNpcFactionQuest, AfterCommitAction.CompleteNpcFactionQuest,
		AfterCommitAction.AbortNpcFactionQuest,
		AfterCommitAction.AddNpcAggro, AfterCommitAction.DeleteInteractionNpc,
		AfterCommitAction.DeleteWorldNpcs,
		AfterCommitAction.BroadcastZoneMissionEnd, AfterCommitAction.ScheduleEventQuestRefresh,
		AfterCommitAction.PlayMovieRandom {
	record CloseDialog() implements AfterCommitAction {
	}

	/**
	 * 发送已提交的 QuestStatus/quest_vars 投影并刷新任务可见性。
	 * Sends the committed QuestStatus/quest_vars projection and refreshes quest visibility.
	 */
	record SyncQuestState(QuestStateSyncMode mode) implements AfterCommitAction {
		public SyncQuestState {
			if (mode == null) {
				throw new NullPointerException("mode");
			}
		}
	}

	/**
	 * 奖励变更后发送已提交的玩家属性投影。
	 * Sends the committed player-stat projection after reward mutations.
	 */
	record RefreshPlayerStats() implements AfterCommitAction {
	}

	/** 打开指定 dialogId 的任务对话页。objectId 由执行上下文的权威交互对象提供。 */
	record ShowQuestDialog(int dialogId) implements AfterCommitAction {
		public ShowQuestDialog {
			if (dialogId <= 0) {
				throw new IllegalArgumentException("dialogId must be positive");
			}
		}
	}

	/** 打开不携带 questId 的任务选择页。objectId 由执行上下文的权威交互对象提供。 */
	record ShowQuestSelectionDialog(int dialogId) implements AfterCommitAction {
		public ShowQuestSelectionDialog {
			if (dialogId <= 0) {
				throw new IllegalArgumentException("dialogId must be positive");
			}
		}
	}

	/**
	 * 发送不附带任务 id 的原始 SM_DIALOG_WINDOW 页面。
	 * Sends a raw SM_DIALOG_WINDOW page without attaching the quest id.
	 */
	record ShowDialogWindow(int dialogId) implements AfterCommitAction {
		public ShowDialogWindow {
			if (dialogId <= 0) {
				throw new IllegalArgumentException("dialogId must be positive");
			}
		}
	}

	/**
	 * 传送到指定世界坐标。instanceId 由传送端口的策略决定 (同世界复用玩家实例,
	 * 否则用默认实例 1)；不允许默认实例猜测 (禁止把 templateId/玩家 target 当目标)。
	 * 只在任务事务 commit 成功后执行；玩家离线时 best-effort 跳过；失败记录审计。
	 */
	record TeleportPlayer(QuestInstanceTarget instanceTarget, int worldId, float x, float y, float z, byte heading)
			implements AfterCommitAction {
		public TeleportPlayer(int worldId, float x, float y, float z, byte heading) {
			this(QuestInstanceTarget.currentOrDefault(), worldId, x, y, z, heading);
		}

		public TeleportPlayer {
			if (instanceTarget == null) {
				throw new NullPointerException("instanceTarget");
			}
			if (worldId <= 0) {
				throw new IllegalArgumentException("worldId must be positive");
			}
			if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("teleport coordinates must be finite");
			}
		}
	}

	/**
	 * 播放过场影片 (SM_PLAY_MOVIE)。影片结束由权威客户端 {@code MovieEnd(movieId)} 事件回调,
	 * 禁止用本地 sleep/估算 timer 代替。只在任务事务 commit 成功后执行; 玩家离线 best-effort 跳过。
	 */
	record PlayMovie(int movieId, QuestMovieType type) implements AfterCommitAction {
		public PlayMovie(int movieId) {
			this(movieId, QuestMovieType.CUTSCENE);
		}

		public PlayMovie {
			if (movieId <= 0) {
				throw new IllegalArgumentException("movieId must be positive");
			}
			if (type == null) {
				throw new NullPointerException("type");
			}
		}
	}

	/**
	 * 从多个影片中等概率随机播放一个 (50% 各半的随机影片分支,如活动任务奖励影片)。
	 * 影片结束仍由权威客户端 {@code MovieEnd(movieId)} 事件回调。
	 * Plays one uniformly selected movie after commit.
	 */
	record PlayMovieRandom(List<Integer> movieIds) implements AfterCommitAction {
		public PlayMovieRandom {
			if (movieIds == null || movieIds.size() < 2) {
				throw new IllegalArgumentException("movieIds must contain at least two entries");
			}
			movieIds = List.copyOf(movieIds);
			if (movieIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("movieIds must be positive");
			}
		}
	}

	/**
	 * 按 slot 在指定世界生成一次性任务 NPC。slot 是任务内编译期常量,despawn 通过
	 * 它引用本事务 spawn 的权威 handle;禁止把 handle/实体状态编码进 quest_vars。
	 * instanceId 由端口策略决定 (目标世界等于玩家当前世界时复用玩家实例,否则默认实例)。
	 * 幂等:同一 slot 已存在 handle 时跳过,不无限刷怪。
	 */
	record SpawnNpc(String slot, int templateId, QuestSpawnLocation location)
			implements AfterCommitAction {
		public SpawnNpc(String slot, int worldId, int templateId, float x, float y, float z, byte heading) {
			this(slot, templateId, new QuestSpawnLocation.Fixed(worldId,
				QuestInstanceTarget.currentOrDefault(), x, y, z, heading));
		}

		public SpawnNpc {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
			if (templateId <= 0) {
				throw new IllegalArgumentException("templateId must be positive");
			}
			if (location == null) {
				throw new NullPointerException("location");
			}
		}
	}

	/**
	 * 等概率生成一个权威变体；可选替换 slot 句柄。
	 * Spawns one uniformly selected authoritative variant; optionally replaces the slot handle.
	 */
	record SpawnNpcRandom(String slot, List<QuestSpawnVariant> variants, boolean replaceExisting)
			implements AfterCommitAction {
		public SpawnNpcRandom {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
			if (variants == null || variants.isEmpty()) {
				throw new IllegalArgumentException("variants must not be empty");
			}
			variants = List.copyOf(variants);
			if (variants.stream().anyMatch(Objects::isNull)) {
				throw new IllegalArgumentException("variants must not contain null");
			}
		}
	}

	/** 按 slot 反引用并删除该任务 spawn 的权威 NPC;绝不凭 templateId 删任意同类。 */
	record DespawnNpc(String slot) implements AfterCommitAction {
		public DespawnNpc {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
		}
	}

	/** 让 slot 的权威 NPC 跟随玩家 (护送)。slot 必须已由本任务 SpawnNpc 注册。 */
	record StartFollow(String slot) implements AfterCommitAction {
		public StartFollow {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
		}
	}

	/** 让本次交互的地图常驻 NPC 跟随玩家到指定坐标。 */
	record StartFollowCurrentTargetToPoint(float x, float y, float z) implements AfterCommitAction {
		public StartFollowCurrentTargetToPoint {
			if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("follow destination coordinates must be finite");
			}
		}
	}

	/** 让本次交互的地图常驻 NPC 跟随玩家到当前世界中的指定 NPC。 */
	record StartFollowCurrentTargetToNpc(int npcId) implements AfterCommitAction {
		public StartFollowCurrentTargetToNpc {
			if (npcId <= 0) {
				throw new IllegalArgumentException("npcId must be positive");
			}
		}
	}

	/** 停止 slot 的权威 NPC 跟随玩家。slot 必须已由本任务 SpawnNpc 注册。 */
	record StopFollow(String slot) implements AfterCommitAction {
		public StopFollow {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
		}
	}

	/** 让 slot 的权威 NPC 攻击玩家当前 target。target 为空时 best-effort 跳过。 */
	record AttackTarget(String slot) implements AfterCommitAction {
		public AttackTarget {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
		}
	}

	/**
	 * 让任务拥有的 NPC 攻击玩家当前世界中指定模板的 NPC。
	 * Lets a task-owned NPC attack a named NPC template in the player's current world instance.
	 */
	record AttackNpcTemplate(String slot, int templateId) implements AfterCommitAction {
		public AttackNpcTemplate {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
			if (templateId <= 0) {
				throw new IllegalArgumentException("templateId must be positive");
			}
		}
	}

	/** 让 slot 的权威 NPC 开始巡逻行走。slot 必须已由本任务 SpawnNpc 注册。 */
	record StartWalking(String slot) implements AfterCommitAction {
		public StartWalking {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
		}
	}

	record BroadcastNpcEmotion(String slot, QuestNpcEmotion emotion) implements AfterCommitAction {
		public BroadcastNpcEmotion {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
			if (emotion == null) {
				throw new NullPointerException("emotion");
			}
		}
	}

	/**
	 * 从当前交互使用的权威 NPC 广播一个表情。
	 * Broadcasts an emote from the authoritative NPC used by the current interaction.
	 */
	record BroadcastInteractionNpcEmotion(QuestNpcEmotion emotion) implements AfterCommitAction {
		public BroadcastInteractionNpcEmotion {
			if (emotion != QuestNpcEmotion.NO && emotion != QuestNpcEmotion.PANIC) {
				throw new IllegalArgumentException("interaction NPC emotion must be NO or PANIC");
			}
		}
	}

	record WatchFollowZone(String slot, String zone) implements AfterCommitAction {
		public WatchFollowZone {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
			if (zone == null || zone.isBlank()) {
				throw new IllegalArgumentException("zone must not be blank");
			}
		}
	}

	/** 监视 slot 的权威 NPC 到达指定坐标（半径由运行时坐标检查器定义）。 */
	record WatchFollowCoordinate(String slot, float x, float y, float z) implements AfterCommitAction {
		public WatchFollowCoordinate {
			if (slot == null || slot.isBlank()) {
				throw new IllegalArgumentException("slot must not be blank");
			}
			if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("follow destination coordinates must be finite");
			}
		}
	}

	/**
	 * 监视本次攻击的常驻 NPC 被玩家以仇恨移动诱导到指定坐标，不改变其战斗 AI。
	 * Watches the attacked resident NPC being lured to a coordinate by combat aggro without changing its AI.
	 */
	record WatchLuredNpcCoordinate(float x, float y, float z, float radius,
		QuestLureCompletion completion) implements AfterCommitAction {
		public WatchLuredNpcCoordinate(float x, float y, float z, float radius) {
			this(x, y, z, radius, QuestLureCompletion.DELETE);
		}

		public WatchLuredNpcCoordinate {
			Objects.requireNonNull(completion, "completion");
			if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
					|| !Float.isFinite(radius) || radius <= 0) {
				throw new IllegalArgumentException("lure destination must be finite and radius must be positive");
			}
		}
	}

	/** 启动玩家可见的任务计时器;超时由权威客户端/引擎 {@code QuestTimerEnd} 回调。 */
	record StartQuestTimer(int seconds, QuestTimerPolicy policy) implements AfterCommitAction {
		public StartQuestTimer(int seconds) {
			this(seconds, QuestTimerPolicy.visible());
		}

		public StartQuestTimer {
			if (seconds <= 0) {
				throw new IllegalArgumentException("seconds must be positive");
			}
			if (policy == null) {
				throw new NullPointerException("policy");
			}
		}
	}

	/** 启动不可见任务计时器;超时由引擎 {@code InvisibleTimerEnd} 回调。 */
	record StartInvisibleTimer(int seconds, QuestTimerPolicy policy) implements AfterCommitAction {
		public StartInvisibleTimer(int seconds) {
			this(seconds, QuestTimerPolicy.invisible());
		}

		public StartInvisibleTimer {
			if (seconds <= 0) {
				throw new IllegalArgumentException("seconds must be positive");
			}
			if (policy == null) {
				throw new NullPointerException("policy");
			}
		}
	}

	/**
	 * 按其类型化标识取消可见或隐形定时器。
	 * Cancels either a visible or invisible timer by its typed identity.
	 */
	record CancelQuestTimer(QuestTimerPolicy.Identity identity) implements AfterCommitAction {
		public CancelQuestTimer() {
			this(QuestTimerPolicy.visible().identity());
		}

		public CancelQuestTimer {
			if (identity == null) {
				throw new NullPointerException("identity");
			}
		}
	}

	/**
	 * 同步升华变身状态（0 = 普通，1 = 变身）。
	 * Synchronizes the ascension morph state (0 = normal, 1 = morphed).
	 */
	record Morph(int ascensionId) implements AfterCommitAction {
		public Morph {
			if (ascensionId != 0 && ascensionId != 1) {
				throw new IllegalArgumentException("ascension morph state must be 0 or 1");
			}
		}
	}

	/**
	 * Changes the player's concrete advanced class and refreshes the player
	 * projection through the same class-change service used by legacy quests.
	 */
	record SetPlayerClass(PlayerClass playerClass) implements AfterCommitAction {
		public SetPlayerClass {
			if (playerClass == null || playerClass == PlayerClass.ALL || playerClass.isStartingClass()) {
				throw new IllegalArgumentException("playerClass must be a concrete advanced class");
			}
		}
	}

	/**
	 * 任务状态提交后启动 NPC 阵营任务生命周期。
	 * Starts the NPC-faction quest lifecycle after the quest state commit.
	 */
	record StartNpcFactionQuest(int npcFactionId) implements AfterCommitAction {
		public StartNpcFactionQuest {
			if (npcFactionId <= 0) {
				throw new IllegalArgumentException("npcFactionId must be positive");
			}
		}
	}

	/**
	 * 奖励与任务状态提交后标记 NPC 阵营任务完成。
	 * Marks the NPC-faction quest complete after rewards and quest state commit.
	 */
	record CompleteNpcFactionQuest(int npcFactionId) implements AfterCommitAction {
		public CompleteNpcFactionQuest {
			if (npcFactionId <= 0) {
				throw new IllegalArgumentException("npcFactionId must be positive");
			}
		}
	}

	/**
	 * 显式放弃转换后中止 NPC 阵营任务。
	 * Aborts the NPC-faction quest after an explicit abandon transition.
	 */
	record AbortNpcFactionQuest(int npcFactionId) implements AfterCommitAction {
		public AbortNpcFactionQuest {
			if (npcFactionId <= 0) {
				throw new IllegalArgumentException("npcFactionId must be positive");
			}
		}
	}

	/**
	 * 状态事务提交后对任务所有者施加一个技能效果。
	 * Applies a skill effect to the quest owner after the state transaction commits.
	 */
	record ApplyEffect(int skillId, int durationMillis) implements AfterCommitAction {
		public ApplyEffect {
			if (skillId <= 0) {
				throw new IllegalArgumentException("skillId must be positive");
			}
			if (durationMillis < 0) {
				throw new IllegalArgumentException("durationMillis must be non-negative");
			}
		}
	}

	/**
	 * 从任务所有者身上移除由技能/效果 id 标识的效果。
	 * Removes the effect identified by its skill/effect id from the quest owner.
	 */
	record RemoveEffect(int effectId) implements AfterCommitAction {
		public RemoveEffect {
			if (effectId <= 0) {
				throw new IllegalArgumentException("effectId must be positive");
			}
		}
	}

	/**
	 * 任务状态事务提交后发送一条建模的系统消息。
	 * Sends a modeled system message after the quest state transaction commits.
	 */
	record SendSystemMessage(QuestSystemMessage message) implements AfterCommitAction {
		public SendSystemMessage {
			if (message == null) {
				throw new NullPointerException("message");
			}
		}
	}

	/** 提交后发送显式建模的扩展系统消息包。Sends an explicitly modeled packet after commit. */
	record SendSystemMessagePacket(QuestSystemMessagePacket message) implements AfterCommitAction {
		public SendSystemMessagePacket {
			if (message == null) {
				throw new NullPointerException("message");
			}
		}
	}

	/**
	 * 存在权威交互目标时，以该目标触发玩家侧表情动作。
	 * Emits a player-side emotion using the authoritative interaction target when present.
	 */
	record PlayerEmotion(QuestPlayerEmotion emotion) implements AfterCommitAction {
		public PlayerEmotion {
			if (emotion == null) {
				throw new NullPointerException("emotion");
			}
		}
	}

	/**
	 * 为指定模板的附近 NPC 增加任务 owner 的仇恨值。
	 * Adds quest-owner aggro to nearby NPCs of the requested template.
	 */
	record AddNpcAggro(int npcTemplateId, int damage) implements AfterCommitAction {
		public AddNpcAggro {
			if (npcTemplateId <= 0 || damage < 0) {
				throw new IllegalArgumentException("npcTemplateId must be positive and damage non-negative");
			}
		}
	}

	/**
	 * 为给定路由启动飞行传送（FLIGHT_TELEPORT 状态 + START_FLYTELEPORT 表情）。
	 * Starts a flight teleport for the given route (FLIGHT_TELEPORT state + START_FLYTELEPORT emote).
	 */
	record FlightTeleport(int flightTeleportId) implements AfterCommitAction {
		public FlightTeleport {
			if (flightTeleportId <= 0) {
				throw new IllegalArgumentException("flightTeleportId must be positive");
			}
		}
	}

	/**
	 * Deletes the interaction NPC of this commit (the authoritative
	 * interactionObjectId) and optionally schedules its respawn. Unlike
	 * DespawnNpc, this addresses a world static NPC rather than a task-spawned one.
	 */
	record DeleteInteractionNpc(boolean scheduleRespawn) implements AfterCommitAction {
	}

	/**
	 * 删除玩家权威世界地图实例中当前存在的每个 NPC。
	 * Deletes every NPC currently present in the player's authoritative world-map instance.
	 */
	record DeleteWorldNpcs() implements AfterCommitAction {
	}

	/** 对若干目标任务广播 zone-mission-end 事件, 触发其启动/推进。 */
	record BroadcastZoneMissionEnd(int[] questIds) implements AfterCommitAction {
		public BroadcastZoneMissionEnd {
			if (questIds == null || questIds.length == 0) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			for (int id : questIds) {
				if (id <= 0) {
					throw new IllegalArgumentException("questIds must be positive");
				}
			}
		}
	}

	/**
	 * After a session-scoped delay, dispatches an internal refresh event to the listed typed owners.
	 * Each target owner re-reads live inventory/state and owns its own start/restart rules.
	 */
	record ScheduleEventQuestRefresh(int seconds, int[] questIds) implements AfterCommitAction {
		public ScheduleEventQuestRefresh {
			if (seconds <= 0) {
				throw new IllegalArgumentException("seconds must be positive");
			}
			if (questIds == null || questIds.length == 0) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			questIds = questIds.clone();
			for (int id : questIds) {
				if (id <= 0) {
					throw new IllegalArgumentException("questIds must be positive");
				}
			}
		}

		@Override
		public int[] questIds() {
			return questIds.clone();
		}
	}
}
