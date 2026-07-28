package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventKey;

/**
 * 定义不持有游戏对象引用的不可变任务图事件。
 * Defines an immutable quest graph event that holds no game-object references.
 */
public sealed interface QuestGraphEvent permits QuestGraphEvent.DialogEvent, QuestGraphEvent.KillEvent, QuestGraphEvent.AttackEvent,
	QuestGraphEvent.PlayerDeathEvent, QuestGraphEvent.KillInWorldEvent {

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
			case DIALOG -> RoutingPolicy.EXCLUSIVE;
			case KILL, ATTACK, PLAYER_DEATH, KILL_IN_WORLD -> RoutingPolicy.BROADCAST;
		};
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
}
