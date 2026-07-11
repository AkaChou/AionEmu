package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BLOCK_RESPONSE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.SocialService;
import com.aionemu.gameserver.world.World;

/**
 * 将玩家加入黑名单的客户端包。
 * Client packet to add a player to the block list.
 *
 * @author Ben
 */
@Slf4j
public class CM_BLOCK_ADD extends AionClientPacket {

	private String targetName;
	private String reason;

	public CM_BLOCK_ADD(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		targetName = readS();
		reason = readS();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {

		Player activePlayer = getConnection().getActivePlayer();

		Player targetPlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(targetName);

		// 试图屏蔽自己 / Trying to block self
		if (activePlayer.getName().equalsIgnoreCase(targetName)) {
			sendPacket(new SM_BLOCK_RESPONSE(SM_BLOCK_RESPONSE.CANT_BLOCK_SELF, targetName));
		}

		// 列表已满 / List full
		else if (activePlayer.getBlockList().isFull()) {
			sendPacket(new SM_BLOCK_RESPONSE(SM_BLOCK_RESPONSE.LIST_FULL, targetName));
		}

		// 玩家离线 / Player offline
		else if (targetPlayer == null) {
			sendPacket(new SM_BLOCK_RESPONSE(SM_BLOCK_RESPONSE.TARGET_NOT_FOUND, targetName));
		}

		// 玩家是你的好友 / Player is your friend
		else if (activePlayer.getFriendList().getFriend(targetPlayer.getObjectId()) != null) {
			sendPacket(SM_SYSTEM_MESSAGE.STR_BLOCKLIST_NO_BUDDY);
		}

		// 玩家已被屏蔽 / Player already blocked
		else if (activePlayer.getBlockList().contains(targetPlayer.getObjectId())) {
			sendPacket(SM_SYSTEM_MESSAGE.STR_BLOCKLIST_ALREADY_BLOCKED);
		}

		// 尝试屏蔽玩家 / Try and block player
		else if (!SocialService.addBlockedUser(activePlayer, targetPlayer, reason)) {
			log.error(I18n.get("log.2e06b4e4b5e0", targetPlayer.getName(), activePlayer.getName()));
		}
	}
}
