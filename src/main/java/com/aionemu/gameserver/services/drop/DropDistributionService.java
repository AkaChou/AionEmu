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
package com.aionemu.gameserver.services.drop;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.DropNpc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_LOOT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author xTz
 */
@Slf4j
public class DropDistributionService {

	private static volatile ObjectProvider<DropDistributionService> instanceProvider;

	public static DropDistributionService getInstance() {
		ObjectProvider<DropDistributionService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	public static void setInstanceProvider(ObjectProvider<DropDistributionService> provider) {
		instanceProvider = provider;
	}

	/**
	 * @param player from CM_GROUP_LOOT to handle rolls
	 */
	public void handleRoll(Player player, int roll, int itemId, int npcId, int index) {
		DropNpc dropNpc = GameWorldServices.dropRegistrationService().getDropRegistrationMap().get(npcId);
		if (player == null || dropNpc == null) {
			return;
		}
		synchronized (dropNpc) {
			DropItem requestedItem = findRequestedItem(player, dropNpc,
					GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npcId), 2, itemId, index);
			if (requestedItem == null) {
				return;
			}
			int luck = 0;
			if (roll == 0) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DICE_GIVEUP_ME);
			} else {
				luck = Rnd.get(1, dropNpc.getMaxRoll());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DICE_RESULT_ME(luck, dropNpc.getMaxRoll()));
			}
			for (Player member : dropNpc.getInRangePlayers()) {
				if (member == null) {
					continue;
				}
				PacketSendUtility.sendPacket(member, new SM_GROUP_LOOT(dropNpc.getLootingTeamId(), member.getObjectId(), itemId, npcId,
						dropNpc.getDistributionId(), luck, index));
				if (!player.equals(member) && member.isOnline()) {
					PacketSendUtility.sendPacket(member, roll == 0 ? SM_SYSTEM_MESSAGE.STR_MSG_DICE_GIVEUP_OTHER(player.getName())
							: SM_SYSTEM_MESSAGE.STR_MSG_DICE_RESULT_OTHER(player.getName(), luck, dropNpc.getMaxRoll()));
				}
			}
			distributeLoot(player, luck, itemId, requestedItem, dropNpc);
		}
	}

	/**
	 * @param player from CM_GROUP_LOOT to handle bids
	 */
	public void handleBid(Player player, long bid, int itemId, int npcId, int index) {
		DropNpc dropNpc = GameWorldServices.dropRegistrationService().getDropRegistrationMap().get(npcId);
		if (player == null || dropNpc == null) {
			return;
		}
		synchronized (dropNpc) {
			DropItem requestedItem = findRequestedItem(player, dropNpc,
					GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npcId), 3, itemId, index);
			if (requestedItem == null) {
				return;
			}
			if ((bid > 0 && player.getInventory().getKinah() < bid) || bid < 0 || bid > 999999999) {
				bid = 0;
			}
			PacketSendUtility.sendPacket(player,
					bid > 0 ? SM_SYSTEM_MESSAGE.STR_MSG_PAY_RESULT_ME : SM_SYSTEM_MESSAGE.STR_MSG_PAY_GIVEUP_ME);
			for (Player member : dropNpc.getInRangePlayers()) {
				if (member == null) {
					continue;
				}
				PacketSendUtility.sendPacket(member, new SM_GROUP_LOOT(dropNpc.getLootingTeamId(), member.getObjectId(), itemId, npcId,
						dropNpc.getDistributionId(), bid, index));
				if (!player.equals(member) && member.isOnline()) {
					if (bid > 0) {
						PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_MSG_PAY_RESULT_OTHER(player.getName()));
					} else {
						PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_MSG_PAY_GIVEUP_OTHER(player.getName()));
					}
				}
			}
			distributeLoot(player, bid, itemId, requestedItem, dropNpc);
		}
	}

	static DropItem findRequestedItem(Player player, DropNpc dropNpc, Set<DropItem> dropItems, int distributionId,
			int itemId, int index) {
		if (player == null || dropNpc == null || dropItems == null || !dropNpc.containsPlayerStatus(player)
				|| dropNpc.getDistributionId() != distributionId || dropNpc.getCurrentIndex() != index) {
			return null;
		}
		synchronized (dropItems) {
			for (DropItem dropItem : dropItems) {
				if (dropItem.getIndex() == index && dropItem.getDropTemplate().getItemId() == itemId) {
					return dropItem;
				}
			}
		}
		return null;
	}

	/**
	 * @param player all players have Rolled or Bid then Distributes items
	 *               accordingly
	 */
	private void distributeLoot(Player player, long luckyPlayer, int itemId, DropItem requestedItem, DropNpc dropNpc) {
		player.unsetPlayerMode(PlayerMode.IN_ROLL);
		// Removes player from ARRAY once they have rolled or bid
		if (dropNpc.containsPlayerStatus(player)) {
			dropNpc.delPlayerStatus(player);
		}
		if (luckyPlayer > requestedItem.getHighestValue()) {
			requestedItem.setHighestValue(luckyPlayer);
			requestedItem.setWinningPlayer(player);
		}
		if (!dropNpc.getPlayerStatus().isEmpty()) {
			return;
		}
		for (Player member : dropNpc.getInRangePlayers()) {
			if (member == null) {
				continue;
			}
			if (requestedItem.getWinningPlayer() == null) {
				PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_MSG_PAY_ALL_GIVEUP);
			}
			PacketSendUtility.sendPacket(member, new SM_GROUP_LOOT(dropNpc.getLootingTeamId(),
					requestedItem.getWinningPlayer() != null ? requestedItem.getWinningPlayer().getObjectId() : 1,
					itemId, dropNpc.getObjectId(), dropNpc.getDistributionId(), 0xFFFFFFFF, requestedItem.getIndex()));
		}

		LootGroupRules lgr = dropNpc.getLootGroupRules();

		if (lgr != null) {
			lgr.removeItemToBeDistributed(requestedItem);
		}
		// Check if there is a Winning Player registered if not all members must have
		// passed...
		if (requestedItem.getWinningPlayer() == null) {
			requestedItem.isFreeForAll(true);
			if (lgr != null && !lgr.getItemsToBeDistributed().isEmpty()) {
				GameCoreGameplayServices.dropService().canDistribute(player, lgr.getItemsToBeDistributed().getFirst());
			}
			return;
		}
		requestedItem.isDistributeItem(true);
		GameCoreGameplayServices.dropService().requestDropItem(player, dropNpc.getObjectId(), dropNpc.getCurrentIndex());
		if (lgr != null && !lgr.getItemsToBeDistributed().isEmpty()) {
			GameCoreGameplayServices.dropService().canDistribute(player, lgr.getItemsToBeDistributed().getFirst());
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final DropDistributionService instance = new DropDistributionService();
	}
}
