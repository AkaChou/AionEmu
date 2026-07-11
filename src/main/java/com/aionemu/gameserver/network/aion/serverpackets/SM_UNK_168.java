package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameEventServices;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.EventService;

/**
 * 未知 opcode 0x168 的服务端包，携带角色重入、装饰与活动类型等杂项客户端标志。
 * Server packet for unknown opcode 0x168, carrying misc client flags such as reentry, decor, and event type.
 *
 * @author FrozenKiller
 */
public class SM_UNK_168 extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) { // 59
		writeB(new byte[14]);
		writeC(1);
		writeC(94);
		writeC(1);
		writeC(1);
		writeC(5);
		writeC(15);
		writeC(10);
		writeC(1);
		writeC(1);
		writeC(10);
		writeC(1);
		writeC(2);
		writeC(0);
		writeC(GSConfig.CHARACTER_REENTRY_TIME);
		writeC(EventsConfig.ENABLE_DECOR);
		writeC(GameEventServices.eventService().getEventType().getId()); // 18 Summer Splash V1 / 20 Summer Splash V2
		writeB(new byte[3]);
		writeC(4);
		writeC(1);
		writeB(new byte[5]);
		writeC(1);
		writeC(1);
		writeC(1);
		writeH(0);
		writeC(-128);
		writeC(63);
		writeC(1);
		writeC(19);
		writeB(new byte[3]);
		writeC(1);
		writeC(-86);
		writeC(5);
		writeH(0);
		writeC(1);
		writeC(8);
		writeH(0);
		writeC(-128);
		writeC(63);
		writeB(new byte[10]);
	}
}
