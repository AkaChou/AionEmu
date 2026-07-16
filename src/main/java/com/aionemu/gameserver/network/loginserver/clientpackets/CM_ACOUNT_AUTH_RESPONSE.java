package com.aionemu.gameserver.network.loginserver.clientpackets;

import com.aionemu.gameserver.model.account.AccountTime;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;

/**
 * 登录服对游戏服账号鉴权请求的响应包，返回鉴权结果与账号信息。
 * LoginServer reply to a GameServer account-auth request with result and account details.
 *
 * @author -Nemesiss-
 */
public class CM_ACOUNT_AUTH_RESPONSE extends LsClientPacket {

	public CM_ACOUNT_AUTH_RESPONSE(int opCode) {
		super(opCode);
	}

	private int accountId;
	private boolean result;
	private String accountName;
	private AccountTime accountTime;
	private byte accessLevel;
	private byte membership;
	private long toll;
	private long luna;
	private byte vipLevel;
	private long vipExp;
	private long vipExpireTime;

	@Override
	public void readImpl() {
		accountId = readD();
		result = readC() == 1;

		if (result) {
			accountName = readS();
			accountTime = new AccountTime();

			accountTime.setAccumulatedOnlineTime(readQ());
			accountTime.setAccumulatedRestTime(readQ());

			accessLevel = (byte) readC();
			membership = (byte) readC();
			toll = readQ();
			luna = readQ();
			readC(); // return-account flag is not used by the GameServer
			vipLevel = (byte) readC();
			vipExp = readQ();
			vipExpireTime = readQ();
		}
	}

	@Override
	public void runImpl() {
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().accountAuthenticationResponse(
			accountId, accountName, result, accountTime, accessLevel, membership, toll, luna, vipLevel, vipExp,
			vipExpireTime);
	}
}
