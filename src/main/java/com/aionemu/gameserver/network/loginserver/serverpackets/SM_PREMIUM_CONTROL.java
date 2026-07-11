package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.ingameshop.IGRequest;
import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服发起商城/高级点消费控制的服务端包。
 * Server packet that requests premium/in-game shop cost control on the login server.
 *
 * @author KID
 */
public class SM_PREMIUM_CONTROL extends LsServerPacket {
	private IGRequest request;
	private long cost;

	/**
	 * 构造高级点/商城消费控制包。
	 * Constructs a premium/shop cost control packet.
	 *
	 * in-game shop request
	 * @param cost 消费点数 / cost amount
	 */
	public SM_PREMIUM_CONTROL(IGRequest request, long cost) {
		super(0x0B);
		this.request = request;
		this.cost = cost;
	}

	/**
	 * 写入账号 ID、请求 ID、消费点数与游戏服 ID。
	 * Writes account id, request id, cost and game-server id.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeD(request.accountId);
		writeD(request.requestId);
		writeQ(cost);
		writeC(NetworkConfig.GAMESERVER_ID);
	}
}
