package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步付费/现金 Buff 状态的服务端包。
 * Server packet synchronizing cash-buff status to the client.
 */
public class SM_CASH_BUFF extends AionServerPacket {
	int type;

	/**
	 * 构造现金 Buff 同步包。
	 * Builds a cash-buff status packet.
	 *
	 * @param type 包类型（1 停止/清空，2 开始 buff 等） / packet type (1 stop/clear, 2 start buff, etc.)
	 */
	public SM_CASH_BUFF(int type) {
		this.type = type;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(type); // 버프 시작
		switch (type) {
		case 1:
			writeH(0);
			break;
		case 2:
			writeH(1); // buff number
			writeC(2); // 버프 시작
			writeH(3000); // 버프 아이디
			writeH(0); // 0x00
			writeD(388306); // temps
			break;
		}
	}
}
