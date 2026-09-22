package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.templates.world.WeatherEntry;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 同步当前地图天气状态的服务端包。
 * Server packet that syncs the current map weather state.
 * @author ATracer
 * @author Kwazar
 * @author Nemesiss :D:D
 */
@AllArgsConstructor
public class SM_WEATHER extends AionServerPacket {

	private final WeatherEntry[] weatherEntries;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(0x00);// 未知 / unk
		writeC(weatherEntries.length);
		for (WeatherEntry entry : weatherEntries) {
			writeC(entry.getCode());
		}
	}
}
