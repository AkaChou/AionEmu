package com.aionemu.gameserver.questEngine.definition;

/** Closed set of best-effort actions allowed after a successful commit. */
public sealed interface AfterCommitAction permits AfterCommitAction.CloseDialog,
		AfterCommitAction.ShowQuestDialog, AfterCommitAction.ShowQuestSelectionDialog,
		AfterCommitAction.TeleportPlayer, AfterCommitAction.PlayMovie,
		AfterCommitAction.SpawnNpc, AfterCommitAction.DespawnNpc, AfterCommitAction.StartFollow,
		AfterCommitAction.StopFollow, AfterCommitAction.AttackTarget, AfterCommitAction.StartWalking,
		AfterCommitAction.BroadcastNpcEmotion, AfterCommitAction.WatchFollowZone,
		AfterCommitAction.StartQuestTimer, AfterCommitAction.StartInvisibleTimer,
		AfterCommitAction.CancelQuestTimer, AfterCommitAction.SyncQuestState,
		AfterCommitAction.RefreshPlayerStats, AfterCommitAction.Morph,
		AfterCommitAction.FlightTeleport, AfterCommitAction.DeleteInteractionNpc {
	record CloseDialog() implements AfterCommitAction {
	}

	/** Sends the committed QuestStatus/quest_vars projection and refreshes quest visibility. */
	record SyncQuestState(QuestStateSyncMode mode) implements AfterCommitAction {
		public SyncQuestState {
			if (mode == null) {
				throw new NullPointerException("mode");
			}
		}
	}

	/** Sends the committed player-stat projection after reward mutations. */
	record RefreshPlayerStats() implements AfterCommitAction {
	}

	/** 打开指定 dialogId 的任务对话页。objectId 由执行上下文的权威交互对象提供。 */
	record ShowQuestDialog(int dialogId) implements AfterCommitAction {
		public ShowQuestDialog {
			if (dialogId < 0) {
				throw new IllegalArgumentException("dialogId must be non-negative");
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
	record PlayMovie(int movieId) implements AfterCommitAction {
		public PlayMovie {
			if (movieId <= 0) {
				throw new IllegalArgumentException("movieId must be positive");
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

	/** Cancels either a visible or invisible timer by its typed identity. */
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

	/** Morphs the player into the ascension form for the given ascension id (SM_ASCENSION_MORPH). */
	record Morph(int ascensionId) implements AfterCommitAction {
		public Morph {
			if (ascensionId <= 0) {
				throw new IllegalArgumentException("ascensionId must be positive");
			}
		}
	}

	/** Starts a flight teleport for the given route (FLIGHT_TELEPORT state + START_FLYTELEPORT emote). */
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
}
