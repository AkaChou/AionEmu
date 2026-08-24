package com.aionemu.gameserver.questEngine.e2e.client;

import java.util.Objects;

/**
 * 从真实服务端包对象读取的不可变协议观察，不依赖序列化 opcode。
 * Immutable protocol observation read from a real server-packet object without depending on serialized opcodes.
 */
public record ServerPacketObservation(Type type, int targetObjectId, int dialogId, int questId,
		int action, int status, int step, int movieId, int movieType, int itemObjectId, int itemId,
		int animationMillis, int animationEnd, String detail) {
	/** 被端到端工具解析的服务端包类型。 / Server packet types parsed by the end-to-end tooling. */
	public enum Type {
		DIALOG_WINDOW,
		QUEST_ACTION,
		PLAY_MOVIE,
		ITEM_USAGE_ANIMATION,
		OTHER
	}

	public ServerPacketObservation {
		type = Objects.requireNonNull(type, "type");
		detail = Objects.requireNonNullElse(detail, "");
	}

	/** 创建对话窗口观察。 / Creates a dialog-window observation. */
	public static ServerPacketObservation dialog(int targetObjectId, int dialogId, int questId) {
		return new ServerPacketObservation(Type.DIALOG_WINDOW, targetObjectId, dialogId, questId,
			0, 0, 0, 0, 0, 0, 0, 0, 0, "");
	}

	/** 创建任务状态包观察。 / Creates a quest-action observation. */
	public static ServerPacketObservation questAction(int questId, int action, int status, int step) {
		return new ServerPacketObservation(Type.QUEST_ACTION, 0, 0, questId,
			action, status, step, 0, 0, 0, 0, 0, 0, "");
	}

	/** 创建电影播放包观察。 / Creates a movie-playback observation. */
	public static ServerPacketObservation movie(int movieId, int movieType) {
		return new ServerPacketObservation(Type.PLAY_MOVIE, 0, 0, 0,
			0, 0, 0, movieId, movieType, 0, 0, 0, 0, "");
	}

	/** 创建物品使用动画观察。 / Creates an item-usage-animation observation. */
	public static ServerPacketObservation itemUsageAnimation(int itemObjectId, int itemId, int animationMillis,
			int animationEnd) {
		return new ServerPacketObservation(Type.ITEM_USAGE_ANIMATION, 0, 0, 0,
			0, 0, 0, 0, 0, itemObjectId, itemId, animationMillis, animationEnd, "");
	}

	/** 创建未建模服务端包观察。 / Creates an observation for an unmodeled server packet. */
	public static ServerPacketObservation other(String packetClass) {
		return new ServerPacketObservation(Type.OTHER, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, packetClass);
	}
}
