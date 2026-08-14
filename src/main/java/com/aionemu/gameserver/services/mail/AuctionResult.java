package com.aionemu.gameserver.services.mail;

/**
 * 拍卖结果枚举，标识拍卖邮件结算结果。
 * Auction result enum identifying auction mail settlement outcomes.
 */
public enum AuctionResult {
	/** 竞拍失败 / Failed bid. */
	FAILED_BID(0),
	/** 竞拍取消 / Canceled bid. */
	CANCELED_BID(1),
	/** 出售失败 / Failed sale. */
	FAILED_SALE(2),
	/** 出售成功 / Successful sale. */
	SUCCESS_SALE(3),
	/** 竞拍成功 / Won bid. */
	WIN_BID(4),
	/** 宽限期开始 / Grace start. */
	GRACE_START(5),
	/** 宽限期失败 / Grace fail. */
	GRACE_FAIL(6),
	/** 宽限期成功 / Grace success. */
	GRACE_SUCCESS(7);

	private int value;

	private AuctionResult(int value) {
		this.value = value;
	}

	/**
	 * getId 方法。
	 * getId method.
	 * result
	 */
	public int getId() {
		return this.value;
	}

	/**
	 * getResultFromId 方法。
	 * getResultFromId method.
	 *
	 * resultId
	 * result
	 */
	public static AuctionResult getResultFromId(int resultId) {
		for (AuctionResult result : AuctionResult.values()) {
			if (result.getId() == resultId) {
				return result;
			}
		}
		return null;
	}
}