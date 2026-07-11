package com.aionemu.gameserver.network.aion.serverpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;

import com.aionemu.commons.network.IPRange;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.configs.network.IPConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.ChatService;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;

/**
 * 客户端版本校验应答服务端包（含建角限制、服务器模式、聊天服地址等）。
 * Server packet for client version-check response (char limits, server mode, chat server address, etc.).
 *
 * @author -Nemesiss- CC fix
 * @modified by Novo, cura
 * @author GiGatR00n, NewLives
 */
@Slf4j

public class SM_VERSION_CHECK extends AionServerPacket {

	/**
	 * Aion 客户端版本。
	 * Aion Client version.
	 */
	private int version;
	/**
	 * 可创建角色数量。
	 * Number of characters that can be created.
	 */
	private int characterLimitCount;
	/**
	 * 建角阵营模式相关。
	 * Related to the character creation mode.
	 */
	private final int characterFactionsMode;
	private final int characterCreateMode;

	/**
	 * @param version 客户端版本号 / client version number
	 */
	public SM_VERSION_CHECK(int version) {
		this.version = version;

		if (MembershipConfig.CHARACTER_ADDITIONAL_ENABLE != 10 && MembershipConfig.CHARACTER_ADDITIONAL_COUNT > GSConfig.CHARACTER_LIMIT_COUNT) {
			characterLimitCount = MembershipConfig.CHARACTER_ADDITIONAL_COUNT;
		} else {
			characterLimitCount = GSConfig.CHARACTER_LIMIT_COUNT;
		}
		characterLimitCount *= GameServerNetworkServices.networkController().getServerCount();

		if (GSConfig.CHARACTER_CREATION_MODE < 0 || GSConfig.CHARACTER_CREATION_MODE > 2) {
			characterFactionsMode = 0;
		} else {
			characterFactionsMode = GSConfig.CHARACTER_CREATION_MODE;
		}

		if (GSConfig.CHARACTER_FACTION_LIMITATION_MODE < 0 || GSConfig.CHARACTER_FACTION_LIMITATION_MODE > 3) {
			characterCreateMode = 0;
		} else {
			characterCreateMode = GSConfig.CHARACTER_FACTION_LIMITATION_MODE * 0x04;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		// aion 3.0 = 194 / aion 3.0 = 194
		// aion 3.5 = 196 / aion 3.5 = 196
		// aion 4.0 = 201 / aion 4.0 = 201
		// aion 4.5 = 203 / aion 4.5 = 203
		// aion 4.7 = 204 / aion 4.7 = 204
		// aion 4.7.0.7 = 205 / aion 4.7.0.7 = 205
		// aion 4.7.5.x = 206 / aion 4.7.5.x = 206
		// aion 5.1.x.x = 212 / aion 5.1.x.x = 212
		if (version < 213) {
			// 发送错误的客户端版本 / Send wrong client version
			writeC(0x02);
			return;
		}
		if (version == 213) {
			log.info(I18n.get("log.96cb08bb7c66"));
		} else if (version < 213) {
			log.info(I18n.get("log.6a1b02a86399"));
		}
		
		int utcTimeSeconds = (int) (System.currentTimeMillis() / 1000);
		int offset = DateTimeUtil.getZone().getRules().getOffset(Instant.now()).getTotalSeconds();
		int negativeOffset = -offset;

		writeC(0x00);
		writeC(NetworkConfig.GAMESERVER_ID);
		writeD(180205);
		writeD(171201);
		writeD(0x00);
		writeD(180205);
		writeD(utcTimeSeconds);
		writeC(0x00);
		writeC(GSConfig.SERVER_COUNTRY_CODE);
		int serverMode = (characterLimitCount * 0x10) | characterFactionsMode;
		writeC(serverMode | characterCreateMode);
		writeD(utcTimeSeconds);
		writeD(negativeOffset);
		writeD(40014200);
		writeD(0);
		writeD(68536);
		writeB(new byte[20]);
		for (int i = 0; i < 11; i++) {
			writeD(1000);
		}
		writeH(25600);
		writeH(0);
		writeC(0);
		writeD(1000);
		writeH(1);
		writeC(0);
		{
			byte[] addr = IPConfig.getDefaultAddress();
			for (IPRange range : IPConfig.getRanges()) {
				if (range.isInRange(con.getIP())) {
					addr = range.getAddress();
					break;
				}
			}
			writeB(addr);
			writeH(ChatService.getPort());
		}
	}
}
