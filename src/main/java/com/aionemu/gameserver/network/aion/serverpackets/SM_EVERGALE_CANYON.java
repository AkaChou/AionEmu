package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 永风峡谷（Evergale Canyon）状态包：按动作类型写入战场/匹配相关数据。
 * Evergale Canyon status packet: battlefield/match data by action type.
 */
public class SM_EVERGALE_CANYON extends AionServerPacket {
	private int action;

	/**
	 * 按动作类型构造状态包。
	 * Creates a status packet for the given action type.
	 *
	 * @param id 动作类型 / action type
	 */
	public SM_EVERGALE_CANYON(int id) {
		this.action = id;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(action);
		switch (action) {
		case 0: // 12 Bytes
			writeB("00 00 01 00 00 00 00 00 00 00 04 00 A4 72 A3 59 00 00 00 00 6E A3 11 4A 07 00 00 00 4A 00 00 00 01 00 00 00 25 00 00 00 48 00 75 00 72 00 72 00 79 00 70 00 61 00 69 00 6E 00 65 00 2D 00 54 00 68 00 6F 00 72 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 A4 72 A3 59 00 00 00 00 A9 FA 1F 4A 0B 00 00 00 48 00 00 00 01 00 00 00 25 00 00 00 49 00 63 00 61 00 72 00 75 00 75 00 73 00 2D 00 54 00 68 00 6F 00 72 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 A4 72 A3 59 00 00 00 00 07 FB 1F 4A 0A 00 00 00 48 00 00 00 01 00 00 00 25 00 00 00 44 00 61 00 63 00 69 00 61 00 6E 00 2D 00 54 00 68 00 6F 00 72 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 13 73 A3 59 00 00 00 00 F2 BA 19 50 04 00 00 00 42 00 00 00 01 00 00 00 28 00 00 00 52 00 6E 00 67 00 2D 00 55 00 72 00 74 00 65 00 6D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
			break;
		case 1: // 12 bytes
			writeB("00 00 01 00 00 00 00 00 00 00 00 00");
			break;
		case 2: // 36 bytes
			writeB("000001000000000000000100"); // 12
			writeD(2); // battleCount
			writeD(20); // maxBattle
			writeD(2); // battleCount
			writeD(20); // maxBattle
			writeD(18); // playerWaiting
			writeD(2); // BattleQuick
			break;
		case 4: // 141 bytes
			writeB("00 00 01 00 00 00 00 00 00 00 02 00 77 69 A3 59 00 00 00 00 F2 BA 19 50 4B 00 6F 00 72 00 74 00 61 00 6E 00 61 00 20 00 64 00 75 00 20 00 47 00 68 00 65 00 74 00 74 00 6F 00 00 00 01 77 69 A3 59 00 00 00 00 F2 BA 19 50 04 00 00 00 42 00 00 00 01 00 00 00 28 00 00 00 52 00 6E 00 67 00 2D 00 55 00 72 00 74 00 65 00 6D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
			// writeB("00 00 01 00 00 00 00 00 00 00 02 00 42 70 A3 59 00 00 00 00 F2 BA 19
			// 50 59 00 6F 00 6C 00 6F 00 00 00 01 42 70 A3 59 00 00 00 00 F2 BA 19 50 04 00
			// 00 00 42 00 00 00 01 00 00 00 28 00 00 00 52 00 6E 00 67 00 2D 00 55 00 72 00
			// 74 00 65 00 6D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
			// 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
			break;
		}
	}
}
