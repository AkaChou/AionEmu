package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步账号属性（GM 标记、账号类型、购买类型与时间）的服务端包。
 * Server packet synchronizing account properties (GM flag, account type, purchase type, and time) to the client.
 */
@NoArgsConstructor
@AllArgsConstructor
public class SM_ACCOUNT_PROPERTIES extends AionServerPacket {
	private boolean isGM;
	private int accountType;
	private int purchaseType;
	private int time;

	/**
	 * 仅设置 GM 标记的精简构造。
	 * Minimal constructor that only sets the GM flag.
	 * @param isGM whether the account is a GM
	 */
	public SM_ACCOUNT_PROPERTIES(boolean isGM) {
		this.isGM = isGM;
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
