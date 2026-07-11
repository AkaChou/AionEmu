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

	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_ACOUNT_AUTH_RESPONSE(int opCode) {
		super(opCode);
	}

	/**
	 * 账号 ID。
	 * Account id.
	 */
	private int accountId;

	/**
	 * 鉴权结果（true=通过）。
	 * Auth result (true = authenticated).
	 */
	private boolean result;

	/**
	 * 账号名（鉴权成功时有效）。
	 * Account name (present when response is ok).
	 */
	private String accountName;
	/**
	 * 账号在线/休息累计时间。
	 * Accumulated online/rest account time.
	 */
	private AccountTime accountTime;
	/**
	 * 权限等级（普通/GM/Admin）。
	 * Access level (regular/gm/admin).
	 */
	private byte accessLevel;
	/**
	 * 会员等级（普通/高级）。
	 * Membership (regular/premium).
	 */
	private byte membership;

	/**
	 * Toll 点数。
	 * Toll points.
	 */
	private long toll;

	private long luna;

	/**
	 * 读取鉴权结果及成功时的账号详情。
	 * Reads auth result and, when successful, account details.
	 */
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
		}
	}

	/**
	 * 将鉴权结果转交 LoginServer 门面，完成客户端登录流程。
	 * Forwards the auth result to the LoginServer facade to finish client login.
	 */
	@Override
	public void runImpl() {
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().accountAuthenticationResponse(accountId, accountName, result, accountTime,
				accessLevel, membership, toll, luna);
	}
}
