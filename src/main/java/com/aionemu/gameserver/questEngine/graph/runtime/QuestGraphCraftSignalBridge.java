package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.CraftFailedEvent;

/**
 * 将制作服务的失败产品和 CUBE 数量快照转换为服务端权威任务图事件。
 * Converts craft-service failed-product and CUBE-count snapshots into server-authoritative quest-graph events.
 */
public final class QuestGraphCraftSignalBridge {

	/** 禁止实例化纯静态 bridge。 / Prevents instantiation of this static bridge. */
	private QuestGraphCraftSignalBridge() {
	}

	/** 表示制作服务在失败尝试后读取的产品库存快照。 / Represents the product inventory snapshot read by craft service after a failed attempt. */
	public record FailureSnapshot(int playerId, int productItemId, long inventoryCountAfterAttempt) {
		/** 校验玩家、产品物品和非负库存数量。 / Validates player, product item, and nonnegative inventory count. */
		public FailureSnapshot {
			if (playerId <= 0 || productItemId <= 0 || inventoryCountAfterAttempt < 0) {
				throw new IllegalArgumentException("Craft-failure snapshot is invalid");
			}
		}
	}

	/**
	 * 仅在失败产品的制作后 CUBE 数量为零时创建旧语义兼容事件。
	 * Creates a legacy-compatible event only when the failed product has zero post-attempt CUBE count.
	 */
	public static CraftFailedEvent craftFailed(String eventId, long occurredAt, FailureSnapshot snapshot) {
		if (snapshot == null || snapshot.inventoryCountAfterAttempt() != 0) {
			throw new IllegalArgumentException("Craft failure is not eligible while the product remains in inventory");
		}
		return new CraftFailedEvent(eventId, snapshot.playerId(), occurredAt, snapshot.productItemId(),
			snapshot.inventoryCountAfterAttempt());
	}
}
