/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.network;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.PropertiesUtils;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.SecurityConfig;

/**
 * @author KID
 */
@Slf4j
public class PacketFloodFilter {

	private static volatile ObjectProvider<PacketFloodFilter> instanceProvider;


	public static PacketFloodFilter getInstance() {
		ObjectProvider<PacketFloodFilter> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	public static void setInstanceProvider(ObjectProvider<PacketFloodFilter> provider) {
		instanceProvider = provider;
	}

	private int[] packets;
	private short maxClientRequest = 0x2ff;

	public PacketFloodFilter() {
		if (SecurityConfig.PFF_ENABLE) {
			int cnt = 0;
			packets = new int[maxClientRequest];
			try {
				java.util.Properties props = PropertiesUtils.load(Config.configFile("administration/pff.properties").getPath());
				for (Object key : props.keySet()) {
					String str = (String) key;
					packets[Integer.decode(str)] = Integer.valueOf(props.getProperty(str).trim());
					cnt++;
				}
			} catch (IOException e) {
				log.error("Can't read pff.properties", e);
			}
			log.info("PacketFloodFilter initialized with " + cnt + " packets.");
		} else {
			log.info("PacketFloodFilter disabled.");
		}
	}

	public final int[] getPackets() {
		return this.packets;
	}

	private static final class SingletonHolder {
		private static final PacketFloodFilter instance = new PacketFloodFilter();
	}
}
