package com.aionemu.gameserver.model;

/**
 * 决斗结果。
 * Duel Result enumeration.
 */

public enum DuelResult {
	/** 决斗你胜利 / Duel You Win*/
	DUEL_YOU_WIN(1300098, (byte) 2),
	/** 决斗你失败 / Duel You Lose */
	DUEL_YOU_LOSE(1300099, (byte) 0),
	/** 决斗超时 / Duel Timeout */
	DUEL_TIMEOUT(1300100, (byte) 1);

	private int msgId;
	private byte resultId;

	private DuelResult(int msgId, byte resultId) {
		this.msgId = msgId;
		this.resultId = resultId;
	}

	/** 返回消息 ID / Returns the msg id */
	public int getMsgId() {
		return msgId;
	}

	/** 返回结果 ID / Returns the result id */
	public byte getResultId() {
		return resultId;
	}
}
