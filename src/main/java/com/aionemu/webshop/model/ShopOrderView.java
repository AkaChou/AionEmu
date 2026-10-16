package com.aionemu.webshop.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import lombok.Data;

/**
 * 订单视图：订单头 + 订单行 + 每行的发货状态。
 * Order view: header, lines, and the delivery state of each line.
 */
@Data
public class ShopOrderView {

	private int id;

	private int accountId;

	private String accountName;

	private int playerId;

	private String playerName;

	private BigDecimal totalPrice;

	private Timestamp createdAt;

	private List<ShopOrderLine> lines;

	/**
	 * 每行对应的 web_reward 发货状态；键为 item_id，值为已发货行数。
	 * Delivery state per line derived from web_reward; key is the item id, value is the delivered count.
	 */
	private List<DeliveryState> deliveries;

	/**
	 * 单条 web_reward 记录的发货状态。
	 * Delivery state of a single web_reward row.
	 */
	@Data
	public static class DeliveryState {

		private int itemId;

		private long itemCount;

		private Timestamp added;

		private Timestamp received;

		/** 0 待发 / 1 已发 / 0 pending, 1 delivered. */
		private Integer rewarded;
	}
}
