package com.aionemu.gameserver.model.ingameshop;

/**
 * 游戏内商城购买请求（含赠送与点数同步变体）。
 * In-game shop purchase request (gift and toll-sync variants).
 *
 * @author KID
 */
public class IGRequest {

	public boolean gift = false, sync = false;
	public int playerId;
	public int cost;
	public int requestId, itemObjId;
	public String receiver, message;
	public int accountId;

	public IGRequest(int requestId, int playerId, int itemObjId) {
		this.requestId = requestId;
		this.playerId = playerId;
		this.itemObjId = itemObjId;
	}

	public IGRequest(int requestId, int playerId, String receiver, String message, int itemObjId) {
		this.requestId = requestId;
		this.playerId = playerId;
		this.receiver = receiver;
		this.message = message;
		this.itemObjId = itemObjId;
		gift = true;
	}

	public IGRequest(int requestId, int playerId, int cost, boolean a) {
		this.requestId = requestId;
		this.playerId = playerId;
		this.cost = cost;
		sync = a;
	}
}
