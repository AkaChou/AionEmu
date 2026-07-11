package com.aionemu.gameserver.network.loginserver.serverpackets;

import java.util.List;

import com.aionemu.commons.network.IPRange;
import com.aionemu.gameserver.configs.network.IPConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服注册自身的认证服务端包。
 * Authentication packet that the game server sends to the login server for registration.
 *
 * @author -Nemesiss-
 */
public class SM_GS_AUTH extends LsServerPacket {

	/**
	 * 构造游戏服认证注册包。
	 * Constructs a game-server auth registration packet.
	 */
	public SM_GS_AUTH() {
		super(0x00);
	}

	/**
	 * 写入游戏服 ID、默认地址、IP 区间、端口、最大在线与登录密码。
	 * Writes game-server id, default address, IP ranges, port, max online players and login password.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeC(NetworkConfig.GAMESERVER_ID);
		writeC(IPConfig.getDefaultAddress().length);
		writeB(IPConfig.getDefaultAddress());
		List<IPRange> ranges = IPConfig.getRanges();
		int size = ranges.size();
		writeD(size);
		for (int i = 0; i < size; i++) {
			IPRange ipRange = ranges.get(i);
			byte[] min = ipRange.getMinAsByteArray();
			byte[] max = ipRange.getMaxAsByteArray();
			writeC(min.length);
			writeB(min);
			writeC(max.length);
			writeB(max);
			writeC(ipRange.getAddress().length);
			writeB(ipRange.getAddress());
		}
		writeH(NetworkConfig.GAME_PORT);
		writeD(NetworkConfig.MAX_ONLINE_PLAYERS);
		writeS(NetworkConfig.LOGIN_PASSWORD);
	}
}
