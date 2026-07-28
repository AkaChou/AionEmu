package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventKey;

/**
 * 定义不持有游戏对象引用的不可变任务图事件。
 * Defines an immutable quest graph event that holds no game-object references.
 */
public sealed interface QuestGraphEvent permits QuestGraphEvent.DialogEvent, QuestGraphEvent.KillEvent, QuestGraphEvent.AttackEvent,
	QuestGraphEvent.PlayerDeathEvent, QuestGraphEvent.KillInWorldEvent, QuestGraphEvent.ItemUseEvent,
	QuestGraphEvent.ItemObtainedEvent, QuestGraphEvent.ItemEquippedEvent, QuestGraphEvent.HouseItemUseEvent,
	QuestGraphEvent.WorldEnteredEvent, QuestGraphEvent.ZoneEnteredEvent, QuestGraphEvent.ZoneLeftEvent,
	QuestGraphEvent.ZoneMissionEndedEvent, QuestGraphEvent.LevelUpEvent, QuestGraphEvent.PlayerLogoutEvent,
	QuestGraphEvent.QuestTimerEndedEvent, QuestGraphEvent.MovieEndedEvent, QuestGraphEvent.NpcProximityEvent,
	QuestGraphEvent.EscortReachedTargetEvent, QuestGraphEvent.EscortLostTargetEvent,
	QuestGraphEvent.RankedPlayerKillEvent, QuestGraphEvent.DredgionSettledEvent, QuestGraphEvent.CraftFailedEvent,
	QuestGraphEvent.NpcAggroListedEvent, QuestGraphEvent.WindstreamEnteredEvent, QuestGraphEvent.FlyingRingPassedEvent,
	QuestGraphEvent.SkillUsedEvent {

	/**
	 * 定义由事件类型固定的候选传播策略。
	 * Defines candidate propagation policies fixed by event type.
	 */
	enum RoutingPolicy {
		EXCLUSIVE,
		BROADCAST
	}

	/** 返回稳定事件标识。 / Returns the stable event identifier. */
	String eventId();

	/** 返回触发玩家标识。 / Returns the triggering player identifier. */
	int playerId();

	/** 返回事件发生的 Unix 毫秒时间。 / Returns the event occurrence time in Unix milliseconds. */
	long occurredAt();

	/** 返回强类型事件种类。 / Returns the typed event kind. */
	EventType type();

	/** 返回用于预编译索引的目标标识。 / Returns the target identifier used by the precompiled index. */
	int targetId();

	/**
	 * 返回由事件类型固定的路由策略。
	 * Returns the routing policy fixed by event type.
	 */
	default RoutingPolicy routingPolicy() {
		return switch (type()) {
				case DIALOG, ITEM_USE, MOVIE_ENDED, CRAFT_FAILED -> RoutingPolicy.EXCLUSIVE;
				case KILL, ATTACK, PLAYER_DEATH, KILL_IN_WORLD, ITEM_OBTAINED, ITEM_EQUIPPED, HOUSE_ITEM_USE,
					WORLD_ENTERED, ZONE_ENTERED, ZONE_LEFT, ZONE_MISSION_ENDED, LEVEL_UP, PLAYER_LOGOUT,
					QUEST_TIMER_ENDED, NPC_PROXIMITY, ESCORT_REACHED_TARGET, ESCORT_LOST_TARGET -> RoutingPolicy.BROADCAST;
				case RANKED_PLAYER_KILL, DREDGION_SETTLED, NPC_AGGRO_LISTED -> RoutingPolicy.BROADCAST;
				case WINDSTREAM_ENTERED, FLYING_RING_PASSED, SKILL_USED -> RoutingPolicy.BROADCAST;
		};
	}

	/** 区分当前两个服务端 skill-use 分发入口。 / Distinguishes the two current server skill-use dispatch entry points. */
	enum SkillUseSource {
		/** PlayerController 完成限制检查并接受使用请求。 / PlayerController accepted the use request after restriction checks. */
		CONTROLLER_ACCEPTED,
		/** Skill 执行完必要 action 后发出执行信号。 / Skill emitted an execution signal after required actions. */
		SKILL_ACTIONS_APPLIED
	}

	/**
	 * 返回与 compiler 事件索引一致的查找键。
	 * Returns the lookup key used by the compiler event index.
	 */
	default EventKey eventKey() {
		return new EventKey(type(), targetId());
	}

	/**
	 * 表示玩家与 NPC 发生的对话事件。
	 * Represents a dialog event between a player and an NPC.
	 */
	record DialogEvent(String eventId, int playerId, long occurredAt, int npcId, String dialog) implements QuestGraphEvent {
		/**
		 * 校验对话事件标识、玩家、时间、NPC 和对话动作。
		 * Validates dialog event identity, player, time, NPC, and dialog action.
		 */
		public DialogEvent {
			validateCommon(eventId, playerId, occurredAt);
			if (npcId <= 0) {
				throw new IllegalArgumentException("Dialog NPC id must be positive");
			}
			dialog = requireText(dialog, "dialog action");
		}

		/** 返回 DIALOG 类型。 / Returns the DIALOG type. */
		@Override
		public EventType type() {
			return EventType.DIALOG;
		}

		/** 返回对话 NPC 标识。 / Returns the dialog NPC identifier. */
		@Override
		public int targetId() {
			return npcId;
		}
	}

	/**
	 * 表示玩家获得指定 NPC 击杀 credit 的事件。
	 * Represents an event granting a player kill credit for a specific NPC.
	 */
	record KillEvent(String eventId, int playerId, long occurredAt, int npcId) implements QuestGraphEvent {
		/**
		 * 校验击杀事件标识、玩家、时间和 NPC。
		 * Validates kill event identity, player, time, and NPC.
		 */
		public KillEvent {
			validateCommon(eventId, playerId, occurredAt);
			if (npcId <= 0) {
				throw new IllegalArgumentException("Killed NPC id must be positive");
			}
		}

		/** 返回 KILL 类型。 / Returns the KILL type. */
		@Override
		public EventType type() {
			return EventType.KILL;
		}

		/** 返回被击杀 NPC 标识。 / Returns the killed NPC identifier. */
		@Override
		public int targetId() {
			return npcId;
		}
	}

	/** 表示服务器观察到的 NPC 受攻击快照。 / Represents a server-observed NPC attack snapshot. */
	record AttackEvent(String eventId, int playerId, long occurredAt, int npcId, long currentHp, long maximumHp) implements QuestGraphEvent {
		/** 校验攻击者、NPC 和服务端生命值快照。 / Validates attacker, NPC, and server-side health snapshot. */
		public AttackEvent {
			validateCommon(eventId, playerId, occurredAt);
			if (npcId <= 0 || currentHp < 0 || maximumHp <= 0 || currentHp > maximumHp) {
				throw new IllegalArgumentException("Attack event NPC/health snapshot is invalid");
			}
		}

		/** 返回 ATTACK 类型。 / Returns the ATTACK type. */
		@Override
		public EventType type() {
			return EventType.ATTACK;
		}

		/** 返回受攻击 NPC 标识。 / Returns the attacked NPC identifier. */
		@Override
		public int targetId() {
			return npcId;
		}
	}

	/** 表示服务器观察到的当前玩家死亡。 / Represents a server-observed death of the current player. */
	record PlayerDeathEvent(String eventId, int playerId, long occurredAt) implements QuestGraphEvent {
		/** 校验玩家死亡事件公共字段。 / Validates common player-death fields. */
		public PlayerDeathEvent {
			validateCommon(eventId, playerId, occurredAt);
		}

		/** 返回 PLAYER_DEATH 类型。 / Returns the PLAYER_DEATH type. */
		@Override
		public EventType type() {
			return EventType.PLAYER_DEATH;
		}

		/** 玩家死亡使用固定全局路由键 0。 / Player death uses the fixed global route key zero. */
		@Override
		public int targetId() {
			return 0;
		}
	}

	/** 表示服务器确认的指定世界玩家击杀快照。 / Represents a server-confirmed player-kill snapshot in a world. */
	record KillInWorldEvent(String eventId, int playerId, long occurredAt, int worldId, int victimPlayerId, int victimLevel)
		implements QuestGraphEvent {
		/** 校验世界、受害者身份和等级。 / Validates world, victim identity, and level. */
		public KillInWorldEvent {
			validateCommon(eventId, playerId, occurredAt);
			if (worldId < 0 || victimPlayerId <= 0 || victimPlayerId == playerId || victimLevel <= 0) {
				throw new IllegalArgumentException("Kill-in-world event snapshot is invalid");
			}
		}

		/** 返回 KILL_IN_WORLD 类型。 / Returns the KILL_IN_WORLD type. */
		@Override
		public EventType type() {
			return EventType.KILL_IN_WORLD;
		}

		/** 返回世界路由键。 / Returns the world route key. */
		@Override
		public int targetId() {
			return worldId;
		}
	}

	/** 表示服务器确认的普通物品使用快照。 / Represents a server-confirmed regular item-use snapshot. */
	record ItemUseEvent(String eventId, int playerId, long occurredAt, int itemId, int itemObjectId) implements QuestGraphEvent {
		/** 校验物品模板和实例标识。 / Validates item template and object identifiers. */
		public ItemUseEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateItemId(itemId);
			if (itemObjectId <= 0) {
				throw new IllegalArgumentException("Used item object id must be positive");
			}
		}

		/** 返回 ITEM_USE 类型。 / Returns the ITEM_USE type. */
		@Override
		public EventType type() {
			return EventType.ITEM_USE;
		}

		/** 返回物品模板路由键。 / Returns the item-template route key. */
		@Override
		public int targetId() {
			return itemId;
		}
	}

	/** 表示服务器确认的获得物品事件。 / Represents a server-confirmed item-obtained event. */
	record ItemObtainedEvent(String eventId, int playerId, long occurredAt, int itemId) implements QuestGraphEvent {
		/** 校验物品模板标识。 / Validates the item template identifier. */
		public ItemObtainedEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateItemId(itemId);
		}

		/** 返回 ITEM_OBTAINED 类型。 / Returns the ITEM_OBTAINED type. */
		@Override
		public EventType type() {
			return EventType.ITEM_OBTAINED;
		}

		/** 返回物品模板路由键。 / Returns the item-template route key. */
		@Override
		public int targetId() {
			return itemId;
		}
	}

	/** 表示服务器确认的装备物品事件。 / Represents a server-confirmed item-equipped event. */
	record ItemEquippedEvent(String eventId, int playerId, long occurredAt, int itemId) implements QuestGraphEvent {
		/** 校验物品模板标识。 / Validates the item template identifier. */
		public ItemEquippedEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateItemId(itemId);
		}

		/** 返回 ITEM_EQUIPPED 类型。 / Returns the ITEM_EQUIPPED type. */
		@Override
		public EventType type() {
			return EventType.ITEM_EQUIPPED;
		}

		/** 返回物品模板路由键。 / Returns the item-template route key. */
		@Override
		public int targetId() {
			return itemId;
		}
	}

	/** 表示服务器确认的房屋物品使用事件。 / Represents a server-confirmed house-item-use event. */
	record HouseItemUseEvent(String eventId, int playerId, long occurredAt, int itemId) implements QuestGraphEvent {
		/** 校验房屋物品模板标识。 / Validates the house-item template identifier. */
		public HouseItemUseEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateItemId(itemId);
		}

		/** 返回 HOUSE_ITEM_USE 类型。 / Returns the HOUSE_ITEM_USE type. */
		@Override
		public EventType type() {
			return EventType.HOUSE_ITEM_USE;
		}

		/** 返回物品模板路由键。 / Returns the item-template route key. */
		@Override
		public int targetId() {
			return itemId;
		}
	}

	/** 表示服务器确认的玩家进入世界位置快照。 / Represents a server-confirmed player world-entry location snapshot. */
	record WorldEnteredEvent(String eventId, int playerId, long occurredAt, int worldId, int instanceId, float x, float y, float z)
		implements QuestGraphEvent {
		/** 校验世界、实例和坐标快照。 / Validates world, instance, and coordinate snapshots. */
		public WorldEnteredEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateLocation(worldId, instanceId, x, y, z);
		}

		/** 返回 WORLD_ENTERED 类型。 / Returns the WORLD_ENTERED type. */
		@Override
		public EventType type() {
			return EventType.WORLD_ENTERED;
		}

		/** 进入世界为全局广播，使用固定路由键 0。 / World entry is globally broadcast using route key zero. */
		@Override
		public int targetId() {
			return 0;
		}
	}

	/** 表示服务器确认的玩家进入命名区域快照。 / Represents a server-confirmed player entry into a named zone. */
	record ZoneEnteredEvent(String eventId, int playerId, long occurredAt, String zoneName, int worldId, int instanceId,
			float x, float y, float z) implements QuestGraphEvent {
		/** 校验区域、世界、实例和坐标快照。 / Validates zone, world, instance, and coordinate snapshots. */
		public ZoneEnteredEvent {
			validateCommon(eventId, playerId, occurredAt);
			zoneName = validateZoneName(zoneName);
			validateLocation(worldId, instanceId, x, y, z);
		}

		/** 返回 ZONE_ENTERED 类型。 / Returns the ZONE_ENTERED type. */
		@Override
		public EventType type() {
			return EventType.ZONE_ENTERED;
		}

		/** 返回区域名称的预索引键；router 仍执行完整名称匹配。 / Returns the zone pre-index key; the router still matches the full name. */
		@Override
		public int targetId() {
			return zoneName.hashCode();
		}
	}

	/** 表示服务器确认的玩家离开命名区域快照。 / Represents a server-confirmed player departure from a named zone. */
	record ZoneLeftEvent(String eventId, int playerId, long occurredAt, String zoneName, int worldId, int instanceId)
		implements QuestGraphEvent {
		/** 校验区域、世界和实例快照。 / Validates zone, world, and instance snapshots. */
		public ZoneLeftEvent {
			validateCommon(eventId, playerId, occurredAt);
			zoneName = validateZoneName(zoneName);
			validateWorldInstance(worldId, instanceId);
		}

		/** 返回 ZONE_LEFT 类型。 / Returns the ZONE_LEFT type. */
		@Override
		public EventType type() {
			return EventType.ZONE_LEFT;
		}

		/** 返回区域名称的预索引键；router 仍执行完整名称匹配。 / Returns the zone pre-index key; the router still matches the full name. */
		@Override
		public int targetId() {
			return zoneName.hashCode();
		}
	}

	/** 表示服务器内部向指定任务 owner 投递的区域任务结束事件。 / Represents a server-internal zone-mission-end event for a quest owner. */
	record ZoneMissionEndedEvent(String eventId, int playerId, long occurredAt, int questId) implements QuestGraphEvent {
		/** 校验目标任务 owner。 / Validates the target quest owner. */
		public ZoneMissionEndedEvent {
			validateCommon(eventId, playerId, occurredAt);
			if (questId <= 0) {
				throw new IllegalArgumentException("Zone mission target quest id must be positive");
			}
		}

		/** 返回 ZONE_MISSION_ENDED 类型。 / Returns the ZONE_MISSION_ENDED type. */
		@Override
		public EventType type() {
			return EventType.ZONE_MISSION_ENDED;
		}

		/** 返回显式目标任务 owner 路由键。 / Returns the explicit target quest-owner route key. */
		@Override
		public int targetId() {
			return questId;
		}
	}

	/** 表示由服务端等级写入触发的升级快照。 / Represents a level-up snapshot triggered by a server-side level write. */
	record LevelUpEvent(String eventId, int playerId, long occurredAt, int level) implements QuestGraphEvent {
		/** 校验服务端新等级快照。 / Validates the server-side new-level snapshot. */
		public LevelUpEvent {
			validateCommon(eventId, playerId, occurredAt);
			if (level <= 0 || level > 255) {
				throw new IllegalArgumentException("Level-up event level is invalid");
			}
		}

		/** 返回 LEVEL_UP 类型。 / Returns the LEVEL_UP type. */
		@Override
		public EventType type() {
			return EventType.LEVEL_UP;
		}

		/** 升级事件使用固定全局路由键 0。 / Level-up uses the fixed global route key zero. */
		@Override
		public int targetId() {
			return 0;
		}
	}

	/** 表示玩家离开当前服务端会话时的位置快照。 / Represents the location snapshot when a player leaves the current server session. */
	record PlayerLogoutEvent(String eventId, int playerId, long occurredAt, int worldId, int instanceId) implements QuestGraphEvent {
		/** 校验登出时的服务端世界与实例快照。 / Validates the server world and instance snapshot at logout. */
		public PlayerLogoutEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateWorldInstance(worldId, instanceId);
		}

		/** 返回 PLAYER_LOGOUT 类型。 / Returns the PLAYER_LOGOUT type. */
		@Override
		public EventType type() {
			return EventType.PLAYER_LOGOUT;
		}

		/** 登出事件使用固定全局路由键 0。 / Player logout uses the fixed global route key zero. */
		@Override
		public int targetId() {
			return 0;
		}
	}

	/** 表示当前任务 owner 的命名绝对 deadline 已由服务端确认到期。 / Represents server-confirmed expiry of a named absolute deadline for one quest owner. */
	record QuestTimerEndedEvent(String eventId, int playerId, long occurredAt, int questId, String timer, long deadlineAt)
			implements QuestGraphEvent {
		/** 校验 owner、计时器名称和绝对 deadline。 / Validates the owner, timer name, and absolute deadline. */
		public QuestTimerEndedEvent {
			validateCommon(eventId, playerId, occurredAt);
			if (questId <= 0 || deadlineAt <= 0 || occurredAt < deadlineAt) {
				throw new IllegalArgumentException("Quest-timer-ended owner or deadline is invalid");
			}
			timer = requireIdentifier(timer, "timer name");
		}

		/** 返回 QUEST_TIMER_ENDED 类型。 / Returns the QUEST_TIMER_ENDED type. */
		@Override
		public EventType type() {
			return EventType.QUEST_TIMER_ENDED;
		}

		/** 返回显式目标任务 owner。 / Returns the explicit target quest owner. */
		@Override
		public int targetId() {
			return questId;
		}
	}

	/** 表示已消费服务端播放凭据的影片结束事件。 / Represents a movie completion backed by consumed server playback authority. */
	record MovieEndedEvent(String eventId, int playerId, long occurredAt, int movieId, long playbackId, long startedAt)
			implements QuestGraphEvent {
		/** 校验影片协议标识和服务端播放凭据。 / Validates the movie protocol id and server playback authority. */
		public MovieEndedEvent {
			validateCommon(eventId, playerId, occurredAt);
			if (movieId <= 0 || movieId > 0xFFFF || playbackId <= 0 || startedAt <= 0 || occurredAt < startedAt) {
				throw new IllegalArgumentException("Movie-ended authority snapshot is invalid");
			}
		}

		/** 返回 MOVIE_ENDED 类型。 / Returns the MOVIE_ENDED type. */
		@Override
		public EventType type() {
			return EventType.MOVIE_ENDED;
		}

		/** 返回影片路由标识。 / Returns the movie route identifier. */
		@Override
		public int targetId() {
			return movieId;
		}
	}

	/** 表示服务端确认玩家进入 NPC 固定感知半径的快照。 / Represents a server-confirmed player entry into an NPC's fixed proximity radius. */
	record NpcProximityEvent(String eventId, int playerId, long occurredAt, int npcId, int npcObjectId, int worldId,
			int instanceId, float distance) implements QuestGraphEvent {
		/** 校验 NPC 身份、所在实例和服务端距离。 / Validates NPC identity, instance, and server-measured distance. */
		public NpcProximityEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateNpcSnapshot(npcId, npcObjectId, worldId, instanceId);
			if (!Float.isFinite(distance) || distance < 0 || distance >= 20) {
				throw new IllegalArgumentException("NPC proximity distance must be within the fixed server radius");
			}
		}

		/** 返回 NPC_PROXIMITY 类型。 / Returns the NPC_PROXIMITY type. */
		@Override
		public EventType type() {
			return EventType.NPC_PROXIMITY;
		}

		/** 返回邻近 NPC 模板路由键。 / Returns the proximity NPC template route key. */
		@Override
		public int targetId() {
			return npcId;
		}
	}

	/** 表示护送 NPC 已由服务端检查器确认到达当前任务目标。 / Represents server-confirmed escort arrival for the current quest owner. */
	record EscortReachedTargetEvent(String eventId, int playerId, long occurredAt, int questId, int npcId, int npcObjectId,
			int worldId, int instanceId) implements QuestGraphEvent {
		/** 校验任务 owner 与护送 NPC 快照。 / Validates the quest owner and escort NPC snapshot. */
		public EscortReachedTargetEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateQuestNpcSnapshot(questId, npcId, npcObjectId, worldId, instanceId);
		}

		/** 返回 ESCORT_REACHED_TARGET 类型。 / Returns the ESCORT_REACHED_TARGET type. */
		@Override
		public EventType type() {
			return EventType.ESCORT_REACHED_TARGET;
		}

		/** 返回显式目标任务 owner。 / Returns the explicit target quest owner. */
		@Override
		public int targetId() {
			return questId;
		}
	}

	/** 表示护送 NPC 已由服务端检查器判定丢失当前任务目标。 / Represents server-confirmed escort target loss for the current quest owner. */
	record EscortLostTargetEvent(String eventId, int playerId, long occurredAt, int questId, int npcId, int npcObjectId,
			int worldId, int instanceId) implements QuestGraphEvent {
		/** 校验任务 owner 与护送 NPC 快照。 / Validates the quest owner and escort NPC snapshot. */
		public EscortLostTargetEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateQuestNpcSnapshot(questId, npcId, npcObjectId, worldId, instanceId);
		}

		/** 返回 ESCORT_LOST_TARGET 类型。 / Returns the ESCORT_LOST_TARGET type. */
		@Override
		public EventType type() {
			return EventType.ESCORT_LOST_TARGET;
		}

		/** 返回显式目标任务 owner。 / Returns the explicit target quest owner. */
		@Override
		public int targetId() {
			return questId;
		}
	}

	/** 表示服务端确认并分配给当前 credit recipient 的军衔玩家击杀。 / Represents a server-confirmed ranked-player kill assigned to the current credit recipient. */
	record RankedPlayerKillEvent(String eventId, int playerId, long occurredAt, int killerPlayerId, int victimPlayerId,
			int victimRankId, int worldId, int instanceId, float creditDistance, boolean recipientAlive) implements QuestGraphEvent {
		/** 校验参与者、军衔、实例与 credit 资格快照。 / Validates participants, rank, instance, and credit-eligibility snapshot. */
		public RankedPlayerKillEvent {
			validateCommon(eventId, playerId, occurredAt);
			if (killerPlayerId <= 0 || victimPlayerId <= 0 || killerPlayerId == victimPlayerId || playerId == victimPlayerId
					|| victimRankId <= 0 || victimRankId > 18 || !Float.isFinite(creditDistance) || creditDistance < 0 || !recipientAlive) {
				throw new IllegalArgumentException("Ranked-player-kill credit snapshot is invalid");
			}
			validateWorldInstance(worldId, instanceId);
		}

		/** 返回 RANKED_PLAYER_KILL 类型。 / Returns the RANKED_PLAYER_KILL type. */
		@Override
		public EventType type() {
			return EventType.RANKED_PLAYER_KILL;
		}

		/** 返回受害者实际军衔 ID，用于最低军衔范围路由。 / Returns the victim rank id used for minimum-rank range routing. */
		@Override
		public int targetId() {
			return victimRankId;
		}
	}

	/** 表示当前成员已由服务端 Dredgion run 完成结算。 / Represents server-confirmed Dredgion-run settlement for the current member. */
	record DredgionSettledEvent(String eventId, int playerId, long occurredAt, int worldId, int instanceId)
			implements QuestGraphEvent {
		/** 校验结算成员与 instance run 快照。 / Validates the settled member and instance-run snapshot. */
		public DredgionSettledEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateWorldInstance(worldId, instanceId);
		}

		/** 返回 DREDGION_SETTLED 类型。 / Returns the DREDGION_SETTLED type. */
		@Override
		public EventType type() {
			return EventType.DREDGION_SETTLED;
		}

		/** Dredgion settlement 使用固定全局路由键 0。 / Dredgion settlement uses the fixed global route key zero. */
		@Override
		public int targetId() {
			return 0;
		}
	}

	/** 表示服务端确认的制作失败，且失败产品在玩家 CUBE 中仍为零。 / Represents a server-confirmed craft failure whose failed product remains absent from the player's CUBE. */
	record CraftFailedEvent(String eventId, int playerId, long occurredAt, int itemId, long inventoryCountAfterAttempt)
			implements QuestGraphEvent {
		/** 校验失败产品及制作后库存快照。 / Validates the failed product and post-attempt inventory snapshot. */
		public CraftFailedEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateItemId(itemId);
			if (inventoryCountAfterAttempt != 0) {
				throw new IllegalArgumentException("Craft-failed inventory count must be zero");
			}
		}

		/** 返回 CRAFT_FAILED 类型。 / Returns the CRAFT_FAILED type. */
		@Override
		public EventType type() {
			return EventType.CRAFT_FAILED;
		}

		/** 返回失败产品物品模板标识。 / Returns the failed product item-template identifier. */
		@Override
		public int targetId() {
			return itemId;
		}
	}

	/** 表示服务端 NPC 仇恨列表变化向半径内玩家产生的感知信号。 / Represents server NPC aggro-list perception delivered to a player within the fixed radius. */
	record NpcAggroListedEvent(String eventId, int playerId, long occurredAt, int aggroPlayerId, int npcId, int npcObjectId,
			int worldId, int instanceId, float recipientDistance, boolean recipientKnownToNpc) implements QuestGraphEvent {
		/** 校验仇恨来源玩家、NPC 快照和接收者距离。 / Validates aggro-source player, NPC snapshot, and recipient distance. */
		public NpcAggroListedEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateNpcSnapshot(npcId, npcObjectId, worldId, instanceId);
			if (aggroPlayerId <= 0 || !Float.isFinite(recipientDistance) || recipientDistance < 0 || recipientDistance >= 50
					|| !recipientKnownToNpc) {
				throw new IllegalArgumentException("NPC aggro-list recipient snapshot is invalid");
			}
		}

		/** 返回 NPC_AGGRO_LISTED 类型。 / Returns the NPC_AGGRO_LISTED type. */
		@Override
		public EventType type() {
			return EventType.NPC_AGGRO_LISTED;
		}

		/** 返回产生仇恨信号的 NPC 模板标识。 / Returns the NPC template identifier producing the aggro signal. */
		@Override
		public int targetId() {
			return npcId;
		}
	}

	/** 表示服务端完整验证后进入风道的 movement 事件。 / Represents windstream entry after complete server validation. */
	record WindstreamEnteredEvent(String eventId, int playerId, long occurredAt, int worldId, int instanceId, int teleportId,
			int routeId, int distance, boolean routePositionValidated, boolean pendingPathMatched, boolean flightStateEligible)
			implements QuestGraphEvent {
		/** 校验 route 归一化、实例和全部服务端 authority 证明。 / Validates route normalization, instance, and all server-authority proofs. */
		public WindstreamEnteredEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateWorldInstance(worldId, instanceId);
			int normalizedRouteId = teleportId >= 1000 ? teleportId / 1000 : teleportId;
			if (teleportId <= 0 || routeId <= 0 || routeId >= 1000 || routeId != normalizedRouteId || distance < 0
					|| !routePositionValidated || !pendingPathMatched || !flightStateEligible) {
				throw new IllegalArgumentException("Windstream-entry authority snapshot is invalid");
			}
		}

		/** 返回 WINDSTREAM_ENTERED 类型。 / Returns the WINDSTREAM_ENTERED type. */
		@Override
		public EventType type() {
			return EventType.WINDSTREAM_ENTERED;
		}

		/** 返回风道所在世界作为复合路由键的整数部分。 / Returns the windstream world as the integer part of the composite route key. */
		@Override
		public int targetId() {
			return worldId;
		}
	}

	/** 表示服务端平面相交与半径检查通过后的飞行环事件。 / Represents a flying-ring event after server plane and radius checks pass. */
	record FlyingRingPassedEvent(String eventId, int playerId, long occurredAt, int worldId, int instanceId, String ringName,
			float radius, float centerDistance, boolean planeIntersected, boolean intersectionPointAvailable)
			implements QuestGraphEvent {
		/** 校验 ring 静态名称、实例、平面相交和严格半径。 / Validates the static ring name, instance, plane intersection, and strict radius. */
		public FlyingRingPassedEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateWorldInstance(worldId, instanceId);
			ringName = validateMovementName(ringName);
			if (!Float.isFinite(radius) || radius <= 0 || !Float.isFinite(centerDistance) || centerDistance < 0
					|| centerDistance >= radius || !planeIntersected) {
				throw new IllegalArgumentException("Flying-ring authority snapshot is invalid");
			}
		}

		/** 返回 FLYING_RING_PASSED 类型。 / Returns the FLYING_RING_PASSED type. */
		@Override
		public EventType type() {
			return EventType.FLYING_RING_PASSED;
		}

		/** 返回飞行环所在世界作为复合路由键的整数部分。 / Returns the flying-ring world as the integer part of the composite route key. */
		@Override
		public int targetId() {
			return worldId;
		}
	}

	/** 表示只由技能服务签发并带稳定 use ID 的技能使用信号。 / Represents a skill-use signal issued only by the skill service with a stable use id. */
	record SkillUsedEvent(String eventId, int playerId, long occurredAt, long serverUseId, int skillId, int skillLevel,
			int targetObjectId, int worldId, int instanceId, SkillUseSource source, boolean serverExecutionAccepted)
			implements QuestGraphEvent {
		/** 校验技能、对象、世界及服务端执行 authority。 / Validates skill, object, world, and server-execution authority. */
		public SkillUsedEvent {
			validateCommon(eventId, playerId, occurredAt);
			validateWorldInstance(worldId, instanceId);
			if (serverUseId <= 0 || skillId <= 0 || skillLevel <= 0 || targetObjectId < 0 || source == null
					|| !serverExecutionAccepted) {
				throw new IllegalArgumentException("Skill-use authority snapshot is invalid");
			}
		}

		/** 返回 SKILL_USED 类型。 / Returns the SKILL_USED type. */
		@Override
		public EventType type() {
			return EventType.SKILL_USED;
		}

		/** 返回技能模板标识作为广播路由键。 / Returns the skill-template identifier as the broadcast route key. */
		@Override
		public int targetId() {
			return skillId;
		}
	}

	/**
	 * 校验所有事件共享的字段。
	 * Validates fields shared by all events.
	 */
	private static void validateCommon(String eventId, int playerId, long occurredAt) {
		requireText(eventId, "event id");
		if (playerId <= 0 || occurredAt <= 0) {
			throw new IllegalArgumentException("Event player id and occurrence time must be positive");
		}
	}

	/** 校验事件中的物品模板标识。 / Validates an event item-template identifier. */
	private static void validateItemId(int itemId) {
		if (itemId <= 0) {
			throw new IllegalArgumentException("Event item id must be positive");
		}
	}

	/** 校验规范化区域名称并返回原值。 / Validates and returns a canonical zone name. */
	private static String validateZoneName(String zoneName) {
		if (zoneName == null || zoneName.isEmpty() || zoneName.length() > 192
				|| !(zoneName.charAt(0) == '_' || zoneName.charAt(0) >= 'A' && zoneName.charAt(0) <= 'Z')
				|| !zoneName.chars().allMatch(character -> character == '_' || character == '.' || character == '-'
					|| character >= 'A' && character <= 'Z' || character >= '0' && character <= '9')) {
			throw new IllegalArgumentException("Event zone name must be canonical uppercase text");
		}
		return zoneName;
	}

	/** 校验 movement 静态名称并返回原值。 / Validates and returns a canonical movement static name. */
	private static String validateMovementName(String value) {
		if (value == null || value.isEmpty() || value.length() > 192
				|| !(value.charAt(0) == '_' || value.charAt(0) >= 'A' && value.charAt(0) <= 'Z')
				|| !value.chars().allMatch(character -> character == '_' || character == '.' || character == '-'
					|| character >= 'A' && character <= 'Z' || character >= '0' && character <= '9')) {
			throw new IllegalArgumentException("Event movement name must be canonical uppercase text");
		}
		return value;
	}

	/** 校验世界、实例和有限坐标。 / Validates world, instance, and finite coordinates. */
	private static void validateLocation(int worldId, int instanceId, float x, float y, float z) {
		validateWorldInstance(worldId, instanceId);
		if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
			throw new IllegalArgumentException("Event coordinates must be finite");
		}
	}

	/** 校验服务端世界与实例标识。 / Validates server world and instance identifiers. */
	private static void validateWorldInstance(int worldId, int instanceId) {
		if (worldId <= 0 || instanceId <= 0) {
			throw new IllegalArgumentException("Event world and instance ids must be positive");
		}
	}

	/** 校验 NPC 模板、对象及所在实例标识。 / Validates NPC template, object, and containing-instance identifiers. */
	private static void validateNpcSnapshot(int npcId, int npcObjectId, int worldId, int instanceId) {
		if (npcId <= 0 || npcObjectId <= 0) {
			throw new IllegalArgumentException("NPC event template and object ids must be positive");
		}
		validateWorldInstance(worldId, instanceId);
	}

	/** 校验任务 owner 与 NPC 快照。 / Validates a quest owner together with an NPC snapshot. */
	private static void validateQuestNpcSnapshot(int questId, int npcId, int npcObjectId, int worldId, int instanceId) {
		if (questId <= 0) {
			throw new IllegalArgumentException("Escort event target quest id must be positive");
		}
		validateNpcSnapshot(npcId, npcObjectId, worldId, instanceId);
	}

	/**
	 * 返回非空文本，否则拒绝事件。
	 * Returns non-blank text or rejects the event.
	 */
	private static String requireText(String value, String label) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(label + " is missing");
		}
		return value;
	}

	/** 校验与 XSD questGraphIdentifier 一致的有限标识符。 / Validates a bounded identifier matching the XSD questGraphIdentifier. */
	private static String requireIdentifier(String value, String label) {
		requireText(value, label);
		if (value.length() > 128 || !isAsciiLetter(value.charAt(0))
				|| !value.chars().allMatch(character -> isAsciiLetter(character) || character >= '0' && character <= '9' || character == '_' || character == '.'
					|| character == '-')) {
			throw new IllegalArgumentException(label + " is not a canonical identifier");
		}
		return value;
	}

	/** 判断字符是否为 XSD 标识符允许的 ASCII 字母。 / Returns whether a character is an ASCII letter allowed by the XSD identifier. */
	private static boolean isAsciiLetter(int character) {
		return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z';
	}
}
