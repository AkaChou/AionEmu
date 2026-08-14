package com.aionemu.gameserver.services.drop;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.DropConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.DropNpc;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.InRoll;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.model.team2.common.legacy.LootRuleType;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_LOOT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOT_ITEMLIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOT_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOT_STATUS.Status;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.RespawnService;
import com.aionemu.gameserver.services.item.ItemInfoService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 掉落服务，处理掉落列表展示、拾取与队伍分配。
 * Drop service handling loot list display, pickup, and group distribution.
 *
 * @author ATracer, xTz
 */
@Slf4j
public class DropService {

	private static volatile ObjectProvider<DropService> instanceProvider;

	/**
	 * 获取单例实例。
	 * Returns the singleton instance.
	 *
	 * service instance
	 */
	public static DropService getInstance() {
		ObjectProvider<DropService> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<DropService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 调度尸体进入自由拾取状态。
	 * Schedules free-for-all looting on a corpse.
	 *
	 * NPC unique object id
	 */
	public void scheduleFreeForAll(final int npcUniqueId) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				DropNpc dropNpc = dropRegistrationService().getDropRegistrationMap().get(npcUniqueId);
				if (dropNpc != null) {
					dropRegistrationService().getDropRegistrationMap().get(npcUniqueId).startFreeForAll();
					VisibleObject npc = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(npcUniqueId);
					if (npc != null && npc.isSpawned()) {
						if (npc instanceof Npc freeForAllNpc
								&& (freeForAllNpc.getRace() == com.aionemu.gameserver.model.Race.ELYOS
										|| freeForAllNpc.getRace() == com.aionemu.gameserver.model.Race.ASMODIANS)) {
							PacketSendUtility.broadcastPacket(npc, new SM_LOOT_STATUS(npcUniqueId, Status.LOOT_ENABLE),
									looter -> freeForAllNpc.getRace() != looter.getRace());
						} else {
							PacketSendUtility.broadcastPacket(npc, new SM_LOOT_STATUS(npcUniqueId, Status.LOOT_ENABLE));
						}
					}
				}
			}
		}, 240000);
	}

	/**
	 * 注销 NPC 掉落登记（重生/消失时）。
	 * Unregisters drop data for an NPC (on respawn/despawn).
	 *
	 * target NPC
	 */
	public void unregisterDrop(Npc npc) {
		Integer npcObjId = npc.getObjectId();
		Map<Integer, DropNpc> dropRegmap = dropRegistrationService().getDropRegistrationMap();
		dropRegistrationService().getCurrentDropMap().remove(npcObjId);

		if (dropRegmap.containsKey(npcObjId)) {
			dropRegmap.remove(npcObjId);
		}
	}

	/**
	 * 玩家点击尸体时请求掉落列表。
	 * Requests the drop list when a player clicks a corpse.
	 *
	 * requesting player
	 * NPC object id
	 */
	public void requestDropList(Player player, int npcId) {
		DropNpc dropNpc = dropRegistrationService().getDropRegistrationMap().get(npcId);
		if (player == null || dropNpc == null) {
			return;
		}
		if (player.isLooting()) {
			closeDropList(player, player.getLootingNpcOid());
		}
		if (!dropNpc.isAllowedToLoot(player)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_LOOT_NO_RIGHT);
			return;
		}
		if (dropNpc.isBeingLooted()) {
			if (!dropNpc.getLootingPlayer().isOnline()) {
				log.warn(I18n.get("log.6b1e392aa106", dropNpc.getLootingPlayer(),
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(npcId)));
			} else {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_LOOT_FAIL_ONLOOTING);
				return;
			}
		}
		// 负重过重。 / Overburdened.
		if (player.getInventory().isFull()) {
			// 你负重过重，无法再拾取物品。 / You are too overburdened to pick up any more items.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_TOO_HEAVY);
			return;
		}
		dropNpc.setLootingPlayer(player);
		VisibleObject visObj = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(npcId);
		if (visObj instanceof Npc) {
			Npc npc = ((Npc) visObj);
			ScheduledFuture<?> decayTask = (ScheduledFuture<?>) npc.getController().cancelTask(TaskId.DECAY);
			if (decayTask != null) {
				long reamingDecayTime = decayTask.getDelay(TimeUnit.MILLISECONDS);
				dropNpc.setRemainingDecayTime(reamingDecayTime);
			}
		}

		Set<DropItem> dropItems = dropRegistrationService().getCurrentDropMap().get(npcId);

		if (dropItems == null) {
			dropItems = Collections.emptySet();
		}

		PacketSendUtility.sendPacket(player, new SM_LOOT_ITEMLIST(dropNpc, dropItems, player));
		PacketSendUtility.sendPacket(player, new SM_LOOT_STATUS(npcId, Status.OPEN_DROP_LIST));
		player.unsetState(CreatureState.ACTIVE);
		player.setState(CreatureState.LOOTING);
		player.setLootingNpcOid(npcId);
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.START_LOOT, 0, npcId), true);
	}

	/**
	 * 关闭掉落列表并释放尸体占用。
	 * Closes the drop list and releases corpse looting lock.
	 *
	 * acting player
	 * NPC object id
	 */
	public void closeDropList(Player player, int npcId) {
		final DropNpc dropNpc = dropRegistrationService().getDropRegistrationMap().get(npcId);
		player.unsetState(CreatureState.LOOTING);
		player.setState(CreatureState.ACTIVE);
		player.setLootingNpcOid(0);

		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.END_LOOT, 0, npcId), true);

		if (dropNpc == null || dropNpc.getLootingPlayer() != player) {
			return;
		}
		Set<DropItem> dropItems = dropRegistrationService().getCurrentDropMap().get(npcId);
		dropNpc.setLootingPlayer(null);

		Npc npc = (Npc) com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(npcId);
		if (npc != null) {
			if (dropItems == null || dropItems.isEmpty()) {
				npc.getController().onDelete();
				return;
			}

			Future<?> decayTask = RespawnService.scheduleDecayTask(npc, dropNpc.getRemainingDecayTime());
			npc.getController().addTask(TaskId.DECAY, decayTask);

			LootGroupRules lootGrouRules = dropNpc.getLootGroupRules();
			if (lootGrouRules != null && dropNpc.getInRangePlayers().size() > 1
					&& dropNpc.getAllowedLooters().size() == 1) {
				LootRuleType lrt = lootGrouRules.getLootRule();
				if (lrt != LootRuleType.FREEFORALL) {
					for (Player member : dropNpc.getInRangePlayers()) {
						if (member != null) {
							dropNpc.setAllowedLooter(member);
						}
					}
					for (DropItem dropItem : dropItems) {
						if (!dropItem.getDropTemplate().isEachMember()) {
							dropItem.getPlayerObjIds().clear();
						}
					}
				}
			}
			PacketSendUtility.broadcastPacket(npc, new SM_LOOT_STATUS(npcId, Status.LOOT_ENABLE), dropNpc::isAllowedToLoot);
		}
	}

	/**
	 * 判断物品是否可直接发放，或需进入掷骰/竞价流程。
	 * Checks whether an item can be given directly or needs roll/bid.
	 *
	 * requesting player
	 *
	 * @param requestedItem 目标掉落物 / requested drop item
	 * @param requestedItem
	 * @return 可直接发放时为 true / true if direct distribution is allowed
	 */
	public boolean canDistribute(Player player, DropItem requestedItem) {
		int npcId = requestedItem.getNpcObj();
		final DropNpc dropNpc = dropRegistrationService().getDropRegistrationMap().get(npcId);
		if (dropNpc == null) {
			return false;
		}
		int itemId = requestedItem.getDropTemplate().getItemId();
		ItemQuality quality = ItemInfoService.getQuality(itemId);
		LootGroupRules lootGrouRules = dropNpc.getLootGroupRules();
		if (lootGrouRules == null) {
			return true;
		}

		if (itemId != 182400001) {
			if (dropNpc.getInRangePlayers().size() > 1) {
				dropNpc.setDistributionId(lootGrouRules.getAutodistribution().getId());
				dropNpc.setDistributionType(lootGrouRules.getQualityRule(quality));
			} else {
				dropNpc.setDistributionId(0);
			}
			if (dropNpc.getDistributionId() > 1 && dropNpc.getDistributionType()) {
				boolean containDropItem = lootGrouRules.containDropItem(requestedItem);
				if (lootGrouRules.getItemsToBeDistributed().isEmpty() || containDropItem) {
					dropNpc.setCurrentIndex(requestedItem.getIndex());
					for (Player member : dropNpc.getInRangePlayers()) {
						Player finalPlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(member.getObjectId());
						if (finalPlayer != null && finalPlayer.isOnline()) {
							dropNpc.addPlayerStatus(finalPlayer);
							finalPlayer.setPlayerMode(PlayerMode.IN_ROLL,
									new InRoll(npcId, itemId, requestedItem.getIndex(), dropNpc.getDistributionId()));
							PacketSendUtility.sendPacket(finalPlayer, new SM_GROUP_LOOT(dropNpc.getLootingTeamId(),
									0, itemId, npcId, dropNpc.getDistributionId(), 1, requestedItem.getIndex()));
							log.info(I18n.get("log.fb98ac5f31e8"));
						}
					}

					lootGrouRules.setPlayersInRoll(dropNpc.getInRangePlayers(),
							dropNpc.getDistributionId() == 2 ? 17000 : 32000, requestedItem.getIndex(), npcId);

					if (!containDropItem) {
						lootGrouRules.addItemToBeDistributed(requestedItem);
						log.info(I18n.get("log.3593d03e4458", requestedItem));
					}
					return false;
				} else {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LOOT_ALREADY_DISTRIBUTING_ITEM(
							new DescriptionId(ItemInfoService.getNameId(itemId))));
					if (!containDropItem) {
						lootGrouRules.addItemToBeDistributed(requestedItem);
					}
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 判断是否允许自动拾取该掉落物。
	 * Checks whether auto-loot is allowed for the drop item.
	 *
	 * requesting player
	 *
	 * @param requestedItem 目标掉落物 / requested drop item
	 * @param requestedItem
	 * @return 可自动拾取时为 true / true if auto-loot is allowed
	 */
	public boolean canAutoLoot(Player player, DropItem requestedItem) {
		int npcId = requestedItem.getNpcObj();
		final DropNpc dropNpc = dropRegistrationService().getDropRegistrationMap().get(npcId);
		if (dropNpc == null) {
			return false;
		}
		LootGroupRules lootGroupRules = dropNpc.getLootGroupRules();
		if (lootGroupRules == null) {
			return true;
		}

		int itemId = requestedItem.getDropTemplate().getItemId();
		ItemQuality quality = ItemInfoService.getQuality(itemId);
		if (itemId == 182400001) {
			return true;
		}
		int distId = lootGroupRules.getAutodistribution().getId();
		if (dropNpc.getInRangePlayers().size() <= 1) {
			distId = 0;
			dropNpc.setDistributionId(distId);
		}

		if ((distId > 1) && (lootGroupRules.getQualityRule(quality))) {
			boolean anyOnline = false;
			for (Player member : dropNpc.getInRangePlayers()) {
				Player finalPlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(member.getObjectId());
				if ((finalPlayer != null) && (finalPlayer.isOnline())) {
					anyOnline = true;
					break;
				}
			}
			return !anyOnline;
		}
		return true;
	}

	/**
	 * 请求拾取指定索引的掉落物（非自动拾取）。
	 * Requests pickup of a drop item by index (non-auto loot).
	 *
	 * requesting player
	 * NPC object id
	 * drop index
	 */
	public void requestDropItem(Player player, int npcId, int itemIndex) {
		requestDropItem(player, npcId, itemIndex, false);
	}

	/**
	 * 请求拾取指定索引的掉落物。
	 * Requests pickup of a drop item by index.
	 *
	 * requesting player
	 * NPC object id
	 * drop index
	 * @param autoLoot 是否自动拾取 / whether auto-loot
	 */
	public void requestDropItem(Player player, int npcId, int itemIndex, boolean autoLoot) {
		Set<DropItem> dropItems = dropRegistrationService().getCurrentDropMap().get(npcId);
		DropNpc dropNpc = dropRegistrationService().getDropRegistrationMap().get(npcId);
		DropItem requestedItem = null;
		// 掉落未登记 / drop was unregistered
		if (dropItems == null || dropNpc == null) {
			return;
		}

		synchronized (dropItems) {
			for (DropItem dropItem : dropItems)
				if (dropItem.getIndex() == itemIndex) {
					requestedItem = dropItem;
					break;
				}
		}

		if (requestedItem == null) {
			return;
		}

		// 修复漏洞 / fix exploit
		if (!requestedItem.isDistributeItem() && !dropNpc.isAllowedToLoot(player)) {
			return;
		}

		int itemId = requestedItem.getDropTemplate().getItemId();
		ItemTemplate item = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (requestedItem.getDropTemplate().getItemTemplate().hasLimitOne()) {
			if (player.getInventory().getFirstItemByItemId(itemId) != null
					|| player.getStorage(StorageType.REGULAR_WAREHOUSE.getId()).getFirstItemByItemId(itemId) != null) {
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_CAN_NOT_GET_LORE_ITEM((new DescriptionId(item.getNameId()))));
				return;
			}
		}

		long currentDropItemCount = requestedItem.getCount();
		ItemQuality quality = ItemInfoService.getQuality(itemId);
		LootGroupRules lootGrouRules = dropNpc.getLootGroupRules();
		if (lootGrouRules != null && !requestedItem.isDistributeItem() && !requestedItem.isFreeForAll()) {
			if (lootGrouRules.containDropItem(requestedItem)) {
				if (!autoLoot) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1390219));
				}
				return;
			}
			if (autoLoot && !canAutoLoot(player, requestedItem)) {
				return;
			}
			requestedItem.setNpcObj(npcId);
			if (!canDistribute(player, requestedItem)) {
				return;
			}
		}

		if (itemId == 182400001) {
			if (lootGrouRules == null) {
				currentDropItemCount = ItemService.addItem(player, itemId, currentDropItemCount,
						ItemService.DEFAULT_UPDATE_PREDICATE);
			} else {
				List<Player> entitledPlayers = dropNpc.getInRangePlayers().stream()
						.filter(member -> member.isOnline() && !member.getLifeStats().isAlreadyDead() && !member.isMentor()
								&& MathUtil.isIn3dRange(member, player, GroupConfig.GROUP_MAX_DISTANCE))
						.toList();
				currentDropItemCount = distributeEqually(itemId, currentDropItemCount, entitledPlayers);
			}
		} else if (lootGrouRules == null && !requestedItem.isItemWonNotCollected()
				&& dropNpc.getDistributionId() == 0) {
			currentDropItemCount = ItemService.addItem(player, itemId, currentDropItemCount,
					ItemService.DEFAULT_UPDATE_PREDICATE);
			uniqueDropAnnounce(player, requestedItem);
		} else if (!requestedItem.isDistributeItem()) {
			if (lootGrouRules != null) {
				if (lootGrouRules.isMisc(quality)) {
					Collection<Player> members = dropNpc.getInRangePlayers();

					if (members.size() > lootGrouRules.getNrMisc()) {
						lootGrouRules.setNrMisc(lootGrouRules.getNrMisc() + 1);
					} else {
						lootGrouRules.setNrMisc(1);
					}

					int i = 0;
					for (Player p : members) {
						i++;
						if (i == lootGrouRules.getNrMisc()) {
							requestedItem.setWinningPlayer(p);
							break;
						}
					}
				} else {
					requestedItem.setWinningPlayer(player);
				}
			} else if (requestedItem.getWinningPlayer() == null) {
				requestedItem.setWinningPlayer(player);
			}

			if (requestedItem.getWinningPlayer() != null) {
				currentDropItemCount = ItemService.addItem(requestedItem.getWinningPlayer(), itemId,
						currentDropItemCount, new TempTradeDropPredicate(dropNpc));
				winningNormalActions(player, npcId, requestedItem);
				uniqueDropAnnounce(player, requestedItem);
			}
		}

		// 将物品分配给正确玩家并相应发送消息。 / handles distribution of item to correct player and messages accordingly
		else if (!autoLoot && requestedItem.isDistributeItem()) {
			if (player != requestedItem.getWinningPlayer() && requestedItem.isItemWonNotCollected()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LOOT_ANOTHER_OWNER_ITEM);
				return;
			} else if (!ItemService.canAddItem(requestedItem.getWinningPlayer(), itemId, currentDropItemCount)) {
				PacketSendUtility.sendPacket(requestedItem.getWinningPlayer(),
						SM_SYSTEM_MESSAGE.STR_MSG_DICE_INVEN_ERROR);
				requestedItem.isItemWonNotCollected(true);
				return;
			}
			if (dropNpc.getDistributionId() == 3 && requestedItem.getHighestValue() > 0
					&& !requestedItem.getWinningPlayer().getInventory().tryDecreaseKinah(requestedItem.getHighestValue())) {
				requestedItem.isItemWonNotCollected(true);
				return;
			}

			long requestedCount = currentDropItemCount;
			currentDropItemCount = ItemService.addItem(requestedItem.getWinningPlayer(), itemId, currentDropItemCount,
					new TempTradeDropPredicate(dropNpc));
			if (currentDropItemCount != 0) {
				long grantedCount = requestedCount - currentDropItemCount;
				if (grantedCount > 0) {
					requestedItem.getWinningPlayer().getInventory().decreaseByItemId(itemId, grantedCount);
				}
				if (dropNpc.getDistributionId() == 3 && requestedItem.getHighestValue() > 0) {
					requestedItem.getWinningPlayer().getInventory().increaseKinah(requestedItem.getHighestValue());
				}
				requestedItem.isItemWonNotCollected(true);
				return;
			}

			switch (dropNpc.getDistributionId()) {
			case 2:
				winningRollActions(requestedItem.getWinningPlayer(), itemId, npcId);
				break;
			case 3:
				winningBidActions(requestedItem.getWinningPlayer(), npcId, requestedItem.getHighestValue());
				break;
			}

			uniqueDropAnnounce(player, requestedItem);
		}

		if (currentDropItemCount <= 0) {
			synchronized (dropItems) {
				dropItems.remove(requestedItem);
			}
		} else {
			requestedItem.setCount(currentDropItemCount);
		}
		if (autoLoot) {
			if (dropItems.isEmpty()) {
				Npc npc = (Npc) com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(npcId);
				if (npc != null) {
					npc.getController().onDelete();
				}
			}
		} else {
			resendDropList(dropNpc.getLootingPlayer(), npcId, dropNpc, dropItems);
		}
	}

	private static long distributeEqually(int itemId, long count, List<Player> players) {
		if (players.isEmpty()) {
			return count;
		}
		long countPerPlayer = count / players.size();
		for (int i = players.size() - 1; i >= 0; i--) {
			long share = i == 0 ? count : countPerPlayer;
			long remaining = ItemService.addItem(players.get(i), itemId, share, ItemService.DEFAULT_UPDATE_PREDICATE);
			count = count - share + remaining;
		}
		return count;
	}

	private void resendDropList(Player player, int npcId, DropNpc dropNpc, Set<DropItem> dropItems) {
		Npc npc = (Npc) com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(npcId);
		if (dropItems.size() != 0) {
			if (player != null) {
				PacketSendUtility.sendPacket(player, new SM_LOOT_ITEMLIST(dropNpc, dropItems, player));
			}
		} else {
			if (player != null) {
				PacketSendUtility.sendPacket(player, new SM_LOOT_STATUS(npcId, Status.CLOSE_DROP_LIST));
				player.unsetState(CreatureState.LOOTING);
				player.setState(CreatureState.ACTIVE);
				PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.END_LOOT, 0, npcId), true);
			}
			if (npc != null) {
				npc.getController().onDelete();
			}
		}
	}

	/**
	 * @param player 掷骰中奖时消息通知的玩家 / messages when item gained via ROLLED
	 */
	private void winningRollActions(Player player, int itemId, int npcId) {
		PacketSendUtility.sendPacket(player,
				SM_SYSTEM_MESSAGE.STR_MSG_LOOT_GET_ITEM_ME(new DescriptionId(ItemInfoService.getNameId(itemId))));

		if (player.isInGroup2() || player.isInAlliance2()) {
			for (Player member : dropRegistrationService().getDropRegistrationMap().get(npcId)
					.getInRangePlayers()) {
				if (member != null && !player.equals(member) && member.isOnline()) {
					PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_MSG_LOOT_GET_ITEM_OTHER(player.getName(),
							new DescriptionId(ItemInfoService.getNameId(itemId))));
				}
			}
		}
	}

	/**
	 * @param player 竞价中奖时消息/移除并分享基纳的玩家 / messages/removes and shares kinah when item gained via BID
	 */
	private void winningBidActions(Player player, int npcId, long highestValue) {
		DropNpc dropNpc = dropRegistrationService().getDropRegistrationMap().get(npcId);

		if (highestValue > 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_PAY_ACCOUNT_ME(highestValue));
		}

		if (player.isInGroup2() || player.isInAlliance2())

			for (Player member : dropNpc.getInRangePlayers())

				if (member != null && !player.equals(member) && member.isOnline()) {
					PacketSendUtility.sendPacket(member,
							SM_SYSTEM_MESSAGE.STR_MSG_PAY_ACCOUNT_OTHER(player.getName(), highestValue));
					long distributeKinah = highestValue / (dropNpc.getInRangePlayers().size() - 1);
					member.getInventory().increaseKinah(distributeKinah);
					PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_MSG_PAY_DISTRIBUTE(highestValue,
							dropNpc.getInRangePlayers().size() - 1, distributeKinah));
				}
	}

	private void winningNormalActions(Player player, int npcId, DropItem requestedItem) {
		DropNpc dropNpc = dropRegistrationService().getDropRegistrationMap().get(npcId);
		if (player == null || dropNpc == null) {
			return;
		}
		int itemId = requestedItem.getDropTemplate().getItemId();
		if (player.isInGroup2() || player.isInAlliance2()) {
			for (Player member : dropNpc.getInRangePlayers()) {
				if (member != null && !requestedItem.getWinningPlayer().equals(member) && member.isOnline()) {
					PacketSendUtility.sendPacket(member,
							SM_SYSTEM_MESSAGE.STR_MSG_GET_ITEM_PARTYNOTICE(requestedItem.getWinningPlayer().getName(),
									new DescriptionId(ItemInfoService.getNameId(itemId))));
				}
			}
		}
	}

	/**
	 * 玩家看见尸体时同步掉落可用状态。
	 * Syncs loot-available status when a player sees a corpse.
	 *
	 * observing player
	 * corpse NPC
	 */
	public void see(final Player player, Npc owner) {
		final int id = owner.getObjectId();
		final DropNpc dropNpc = dropRegistrationService().getDropRegistrationMap().get(id);

		if (dropNpc == null) {
			return;
		}
		if (dropNpc.isAllowedToLoot(player)) {
			PacketSendUtility.sendPacket(player, new SM_LOOT_STATUS(id, Status.LOOT_ENABLE));
		}
	}

	private void uniqueDropAnnounce(final Player player, final DropItem requestedItem) {
		if (DropConfig.ENABLE_UNIQUE_DROP_ANNOUNCE && !player.getInventory()
				.isFull(requestedItem.getDropTemplate().getItemTemplate().getExtraInventoryId())) {
			final ItemTemplate itemTemplate = ItemInfoService
					.getItemTemplate(requestedItem.getDropTemplate().getItemId());
			if (itemTemplate.getItemQuality() == ItemQuality.RARE || itemTemplate.getItemQuality() == ItemQuality.LEGEND
					|| itemTemplate.getItemQuality() == ItemQuality.UNIQUE
					|| itemTemplate.getItemQuality() == ItemQuality.EPIC
					|| itemTemplate.getItemQuality() == ItemQuality.MYTHIC) {
				final String lastGetName = requestedItem.getWinningPlayer() != null
						? requestedItem.getWinningPlayer().getName()
						: player.getName();
				final int pObjectId = player.getObjectId();
				final int pRaceId = player.getRace().getRaceId();
				final int pMapId = player.getWorldId();
				final int pInstance = player.isInInstance() ? player.getInstanceId() : 0;
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player other) {
						int oObjectId = other.getObjectId();
						int oRaceId = other.getRace().getRaceId();
						int oMapId = other.getWorldId();
						int oInstance = other.isInInstance() ? other.getInstanceId() : 0;
						if (oObjectId != pObjectId && other.isSpawned() && oRaceId == pRaceId && oMapId == pMapId
								&& oInstance == pInstance) {
							PacketSendUtility.sendPacket(other, new SM_SYSTEM_MESSAGE(1390001, lastGetName,
									"[item: " + requestedItem.getDropTemplate().getItemId() + "]"));
						}
					}
				});
			}
		}
	}

	private DropRegistrationService dropRegistrationService() {
		return GameWorldServices.dropRegistrationService();
	}

	private static final class TempTradeDropPredicate extends ItemUpdatePredicate {

		private final DropNpc dropNpc;

		private TempTradeDropPredicate(DropNpc dropNpc) {
			this.dropNpc = dropNpc;
		}

		@Override
		public boolean changeItem(Item input) {
			if (dropNpc.getAllowedLooters().size() > 1) {
				ItemTemplate template = input.getItemTemplate();
				if (template.getTempExchangeTime() != 0) {
					input.setTemporaryExchangeTime(
							(int) (System.currentTimeMillis() / 1000) + (template.getTempExchangeTime() * 60));
					GameTaskManagerServices.temporaryTradeTimeTask().addTask(input, dropNpc.getAllowedLooters());
				}
				return true;
			}
			return false;
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final DropService instance = new DropService();
	}
}
