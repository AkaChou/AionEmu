package com.aionemu.gameserver.network.loginserver.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;

/**
 * 登录服回复商城点数/月华查询结果。
 * luna query.
 *
 * @author KID
 */
public class CM_PREMIUM_RESPONSE extends LsClientPacket {
	private int requestId;
	private int result;
	private long points;
	private long luna;

	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_PREMIUM_RESPONSE(int opCode) {
		super(opCode);
	}

	/**
	 * 读取请求 ID、结果与剩余点数。
	 * Reads request id, result, and remaining points.
	 */
	@Override
	protected void readImpl() {
		requestId = readD();
		result = readD();
		points = readQ();
		luna = readQ();
	}

	/**
	 * 完成商城请求收尾。
	 * Finishes the in-game shop request.
	 */
	@Override
	protected void runImpl() {
		GameRuntimeServices.inGameShopEn().finishRequest(requestId, result, points, luna);
	}
}
