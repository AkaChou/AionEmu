package com.aionemu.gameserver.questEngine.e2e.client;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.Objects;

/**
 * 无头客户端发往任务入口的不可变请求，保留协议身份和类型化事件以便追踪。
 * Immutable request sent by the headless client to a quest ingress, retaining protocol identity and the typed event
 * for tracing.
 */
public record ClientActionRequest(Kind kind, int questId, int npcId, int objectId, int actionId,
		int itemId, int itemObjectId, QuestEvent event) {
	/** 客户端入口种类。 / Client ingress kind. */
	public enum Kind {
		DIALOG_SELECT,
		USE_OBJECT,
		USE_ITEM,
		WORLD_EVENT
	}

	public ClientActionRequest {
		kind = Objects.requireNonNull(kind, "kind");
		event = Objects.requireNonNull(event, "event");
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		if (npcId < 0 || objectId < 0 || itemId < 0 || itemObjectId < 0) {
			throw new IllegalArgumentException("client object and template ids must be non-negative");
		}
	}

	/** 创建带权威交互 objectId 的 NPC 对话请求。 / Creates an NPC dialog request with an authoritative interaction object id. */
	public static ClientActionRequest dialog(int questId, int npcId, int objectId, int actionId) {
		if (npcId <= 0 || objectId <= 0) {
			throw new IllegalArgumentException("NPC dialog requires positive npcId and objectId");
		}
		return new ClientActionRequest(Kind.DIALOG_SELECT, questId, npcId, objectId, actionId, 0, 0,
			new QuestEvent.TalkToNpc(npcId, actionId, objectId));
	}

	/** 创建交互物使用完成请求。 / Creates an interaction-object use-completion request. */
	public static ClientActionRequest useObject(int questId, int npcId, int objectId) {
		if (npcId <= 0 || objectId <= 0) {
			throw new IllegalArgumentException("object use requires positive npcId and objectId");
		}
		return new ClientActionRequest(Kind.USE_OBJECT, questId, npcId, objectId, -1, 0, 0,
			new QuestEvent.TalkToNpc(npcId, -1, objectId));
	}

	/** 创建物品使用请求。 / Creates an item-use request. */
	public static ClientActionRequest useItem(int questId, int itemId, int itemObjectId) {
		if (itemId <= 0 || itemObjectId <= 0) {
			throw new IllegalArgumentException("item use requires positive itemId and itemObjectId");
		}
		return new ClientActionRequest(Kind.USE_ITEM, questId, 0, 0, 0, itemId, itemObjectId,
			new QuestEvent.UseItem(itemId, itemObjectId));
	}

	/** 创建由内存世界注入的任务事件。 / Creates a quest event emitted by the in-memory world. */
	public static ClientActionRequest world(int questId, QuestEvent event) {
		return new ClientActionRequest(Kind.WORLD_EVENT, questId, 0, 0, 0, 0, 0, event);
	}
}
