package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.chest.ChestTemplate;
import com.aionemu.gameserver.model.templates.chest.KeyItem;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * 宝箱 AI：校验钥匙后打开宝箱、注册掉落并展示掉落列表。
 * Chest AI that validates keys, opens the chest, registers drops, and shows the drop list.
 */
@AIName("chest")
public class ChestAI2 extends ActionItemNpcAI2
{
	private ChestTemplate chestTemplate;
	
	/**
	 * 玩家开始与本 NPC 对话/交互。
	 * Player starts dialog/interaction with this NPC.
	 *
	 * 玩家 / player
	 */
	@Override
	protected void handleDialogStart(final Player player) {
		chestTemplate = DataManager.CHEST_DATA.getChestTemplate(getNpcId());
		if (chestTemplate == null) {
			return;
		}
		super.handleDialogStart(player);
	}
	
	/**
	 * 使用交互物完成时的逻辑。
	 * Logic when action-item use finishes.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	protected void handleUseItemFinish(Player player) {
		if (analyzeOpening(player)) {
			if (isAlreadyDead())
				return;
			AI2Actions.dieSilently(this, player);
			Collection<Player> players = new HashSet<Player>();
			if (player.isInGroup2()) {
				for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
					if (MathUtil.isIn3dRange(member, getOwner(), GroupConfig.GROUP_MAX_DISTANCE)) {
						players.add(member);
					}
				}
			} else if (player.isInAlliance2()) {
				for (Player member : player.getPlayerAlliance2().getOnlineMembers()) {
					if (MathUtil.isIn3dRange(member, getOwner(), GroupConfig.GROUP_MAX_DISTANCE)) {
						players.add(member);
					}
				}
			} else {
				players.add(player);
			}
			GameWorldServices.dropRegistrationService().registerDrop(getOwner(), player, maxLevel(players), players);
			GameCoreGameplayServices.dropService().requestDropList(player, getObjectId());
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1111300, player.getObjectId(), 2));
		}
	}

	private int maxLevel(Collection<Player> players) {
		int maxLevel = 0;
		for (Player player : players) {
			if (player.getLevel() > maxLevel) {
				maxLevel = player.getLevel();
			}
		}
		return maxLevel;
	}
	
	private boolean analyzeOpening(final Player player) {
		List<KeyItem> keyItems = chestTemplate.getKeyItem();
		int i = 0;
		for (KeyItem keyItem : keyItems) {
			if (keyItem.getItemId() == 0) {
				return true;
			}
			Item item = player.getInventory().getFirstItemByItemId(keyItem.getItemId());
			if (item != null) {
				if (item.getItemCount() != keyItem.getQuantity()) {
					int _i = 0;
					for (Item findedItem : player.getInventory().getItemsByItemId(keyItem.getItemId())) {
						_i += findedItem.getItemCount();
					} if (_i < keyItem.getQuantity()) {
						return false;
					}
				}
				i++;
				continue;
			} else {
				return false;
			}
		} if (i == keyItems.size()) {
			for (KeyItem keyItem : keyItems) {
				player.getInventory().decreaseByItemId(keyItem.getItemId(), keyItem.getQuantity());
			}
			return true;
		}
		return false;
	}
	
	/**
	 * 玩家结束与本 NPC 对话。
	 * Player finishes dialog with this NPC.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	protected void handleDialogFinish(Player player) {
	}
}
