package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步账号属性（GM 标记、账号类型、购买类型与时间）的服务端包。
 * Server packet synchronizing account properties (GM flag, account type, purchase type, and time) to the client.
 */
public class SM_ACCOUNT_PROPERTIES extends AionServerPacket {
	public SM_ACCOUNT_PROPERTIES() {
	}

	private boolean isGM;
	private int accountType;
	private int purchaseType;
	private int time;

	/**
	 * 仅设置 GM 标记的精简构造。
	 * Minimal constructor that only sets the GM flag.
	 *
	 * whether the account is a GM
	 */
	public SM_ACCOUNT_PROPERTIES(boolean isGM) {
		this.isGM = isGM;
	}

	/**
	 * 使用完整账号属性构造同步包。
	 * Creates a sync packet with full account property fields.
	 *
	 * whether the account is a GM
	 * account type
	 * purchase type
	 * @param time 相关时间戳 / related timestamp
	 */
	public SM_ACCOUNT_PROPERTIES(boolean isGM, int accountType, int purchaseType, int time) {
		this.isGM = isGM;
		this.accountType = accountType;
		this.purchaseType = purchaseType;
		this.time = time;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(this.isGM ? 3 : 0);
		writeH(0);
		writeD(0);
		writeD(0);
		writeD(this.isGM ? 32768 : 0);
		writeD(0);
		writeC(0);
		writeD(31);
		writeD(0);
		writeD(purchaseType); // Purchase Type.
		writeD(accountType); // Account Type.
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(time);
		writeB(new byte[32]);
	}
}
