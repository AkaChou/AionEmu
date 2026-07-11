package com.aionemu.gameserver.network.loginserver.clientpackets;

import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;

/**
 * 登录服同步 MAC 封禁列表。
 * Login server sync of MAC ban list.
 *
 * @author KID
 */
public class CM_MACBAN_LIST extends LsClientPacket {

	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_MACBAN_LIST(int opCode) {
		super(opCode);
	}

	/**
	 * 读取全部 MAC 封禁条目并完成加载回调。
	 * Reads all MAC-ban entries and finishes the load callback.
	 */
	@Override
	protected void readImpl() {
		BannedMacManager bmm = GameServerNetworkServices.bannedMacManager();
		int cnt = readD();
		for (int a = 0; a < cnt; a++) {
			bmm.dbLoad(readS(), readQ(), readS());
		}
		bmm.onEnd();
	}

	/**
	 * 无运行时逻辑（数据已在 readImpl 中处理）。
	 * No runtime logic (data is handled in readImpl).
	 */
	@Override
	protected void runImpl() {
		// ?
	}
}
