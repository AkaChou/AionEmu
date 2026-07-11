package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.player.BlockedPlayer;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.SocialService;

/**
 * 将玩家从黑名单移除的客户端包。
 * Client packet to remove a player from the block list.
 *
 * @author Ben
 */
@Slf4j
public class CM_BLOCK_DEL extends AionClientPacket {

	private String targetName;

	/**
	 * @param opcode
	 */
	public CM_BLOCK_DEL(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		targetName = readS();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();

		BlockedPlayer target = activePlayer.getBlockList().getBlockedPlayer(targetName);
		if (target == null) {
			sendPacket(SM_SYSTEM_MESSAGE.STR_BUDDYLIST_NOT_IN_LIST);
		} else {
			if (!SocialService.deleteBlockedUser(activePlayer, target.getObjId())) {
				log.debug("Could not unblock " + targetName + " from " + activePlayer.getName()
						+ " blocklist. Check database setup.");
			}
		}
	}
}
