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
 * 掉落分配服务，处理队伍掷骰与竞价分配。
 * Drop distribution service handling group roll and bid allocation.
 *
 * @author xTz
 */
@Slf4j
public class DropDistributionService {

	private static volatile ObjectProvider<DropDistributionService> instanceProvider;

	/**
	 * 获取单例实例。
	 * Returns the singleton instance.
	 *
	 * service instance
	 */
	public static DropDistributionService getInstance() {
		ObjectProvider<DropDistributionService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<DropDistributionService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 处理玩家掷骰结果（来自 CM_GROUP_LOOT）。
	 * Handles a player roll result (from CM_GROUP_LOOT).
	 *
	 * rolling player
	 * @param roll 掷骰值，0 表示放弃 / roll value, 0 means pass
	 * item id
	 * npc object id
	 * @param index 掉落索引 / drop index
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
	 * 处理玩家竞价结果（来自 CM_GROUP_LOOT）。
	 * Handles a player bid result (from CM_GROUP_LOOT).
	 *
	 * bidding player
	 * @param bid 出价金额，0 表示放弃 / bid amount, 0 means pass
	 * item id
	 * npc object id
	 * @param index 掉落索引 / drop index
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
		// 玩家掷骰或竞价后从数组移除 / Removes player from ARRAY once they have rolled or bid
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
		// 检查是否登记了获胜玩家；否则所有成员必须 / Check if there is a Winning Player registered if not all members must have
		// 已通过…… / passed...
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
