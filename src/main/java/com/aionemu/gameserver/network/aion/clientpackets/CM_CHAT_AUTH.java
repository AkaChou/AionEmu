package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHAT_INIT;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 向聊天服务器发起玩家登录认证的客户端包。
 * Client packet to authenticate the player with the chat server.
 */
public class CM_CHAT_AUTH extends AionClientPacket {
	public CM_CHAT_AUTH(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		@SuppressWarnings("unused")
		int objectId = readD();
		@SuppressWarnings("unused")
		byte[] macAddress = readB(6);
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (GSConfig.ENABLE_CHAT_SERVER) {
			if (!player.isInPrison()) {
				com.aionemu.gameserver.lifecycle.GameServerNetworkServices.chatServer().sendPlayerLoginRequst(player);
			}
		} else {
			PacketSendUtility.sendPacket(player, new SM_CHAT_INIT(new byte[0]));
		}
	}
}
