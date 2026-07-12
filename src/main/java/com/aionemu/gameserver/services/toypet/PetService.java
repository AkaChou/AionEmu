package com.aionemu.gameserver.services.toypet;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collection;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerPetsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.pets.PetBuff;
import com.aionemu.gameserver.model.team2.common.legacy.LootRuleType;
import com.aionemu.gameserver.model.templates.item.ItemUseLimits;
import com.aionemu.gameserver.model.templates.item.actions.AbstractItemAction;
import com.aionemu.gameserver.model.templates.item.actions.ItemActions;
import com.aionemu.gameserver.model.templates.pet.FoodType;
import com.aionemu.gameserver.model.templates.pet.PetBonusAttr;
import com.aionemu.gameserver.model.templates.pet.PetFeedResult;
import com.aionemu.gameserver.model.templates.pet.PetFlavour;
import com.aionemu.gameserver.model.templates.pet.PetFunction;
import com.aionemu.gameserver.model.templates.pet.PetFunctionType;
import com.aionemu.gameserver.model.templates.pet.PetTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 宠物服务，处理登录同步、喂食、增益、拾取与自动出售等功能。
 * Pet service handling login sync, feeding, buffs, looting and auto-sell features.
 */
@Slf4j
public class PetService {

	private static volatile ObjectProvider<PetService> instanceProvider;

	private PetBuff PetBuff;
	private boolean autoSeel = false;
	private boolean autoBuff = false;

	/**
	 * 返回服务单例；优先通过 Spring 提供者获取。
	 * Returns service singleton; prefers Spring provider when available.
	 *
	 * Service instance
	 */
	public static final PetService getInstance() {
		ObjectProvider<PetService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	public PetService() {
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject Spring instance provider.
	 *
	 * Provider
	 */
	public static void setInstanceProvider(ObjectProvider<PetService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 重命名当前召唤中的宠物。
	 * Rename the currently summoned pet.
	 *
	 * 玩家 / Player
	 * New name
	 */
	public void renamePet(Player player, String name) {
		Pet pet = player.getPet();
		if (pet != null) {
			pet.getCommonData().setName(name);
			DAOManager.getDAO(PlayerPetsDAO.class).updatePetName(pet.getCommonData());
			PacketSendUtility.broadcastPacket(player, new SM_PET(10, pet), true);
		}
	}

	/**
	 * 玩家登录时同步宠物列表到客户端。
	 * Sync pet list to client on player login.
	 *
	 * @param player 玩家 / Player
	 */
	public void onPlayerLogin(Player player) {
		Collection<PetCommonData> playerPets = player.getPetList().getPets();
		if (playerPets != null && playerPets.size() > 0) {
			PacketSendUtility.sendPacket(player, new SM_PET(0, playerPets));
		}
	}

	/**
	 * 开始用物品喂养宠物。
	 * Start feeding the pet with an inventory item.
	 *
	 * Item object id
	 * @param count 喂养数量 / Feed count
	 * Action type
	 * 玩家 / Player
	 */
	public void removeObject(int objectId, int count, int action, Player player) {
		Item item = player.getInventory().getItemByObjId(objectId);
		if (item == null || player.getPet() == null || count > item.getItemCount()) {
			return;
		}
		Pet pet = player.getPet();
		pet.getCommonData().setCancelFeed(false);
		PacketSendUtility.sendPacket(player, new SM_PET(1, action, item.getObjectId(), count, pet));
		PacketSendUtility.sendPacket(player,
				new SM_EMOTION(player, EmotionType.START_FEEDING, 0, player.getObjectId()));
		schedule(pet, player, item, count, action);
	}

	/**
	 * 延迟调度下一次喂养判定。
	 * Schedule the next feed check after a short delay.
	 */
	private void schedule(final Pet pet, final Player player, final Item item, final int count, final int action) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!pet.getCommonData().getCancelFeed()) {
					checkFeeding(pet, player, item, count, action);
				}
			}
		}, 2500);
	}

	/**
	 * 执行单次喂养逻辑并处理奖励/继续喂养。
	 * Perform one feeding step and handle reward or continue feeding.
	 */
	private void checkFeeding(Pet pet, Player player, Item item, int count, int action) {
		PetCommonData commonData = pet.getCommonData();
		PetFeedProgress progress = commonData.getFeedProgress();
		if (!commonData.getCancelFeed()) {
			PetFunction func = pet.getPetTemplate().getPetFunction(PetFunctionType.FOOD);
			PetFlavour flavour = DataManager.PET_FEED_DATA.getFlavourById(func.getId());
			FoodType foodType = flavour.getFoodType(item.getItemId());
			PetFeedResult reward = null;
			if (flavour.isLovedFood(foodType, item.getItemId()) && progress.getLovedFoodRemaining() == 0) {
				foodType = null;
			}
			if (foodType != null) {
				player.getInventory().decreaseItemCount(item, 1, ItemUpdateType.DEC_PET_FOOD);
				reward = flavour.processFeedResult(progress, foodType, item.getItemTemplate().getLevel(),
						player.getCommonData().getLevel(), player.getRates().getPetFeedingRate());
				if (progress.getHungryLevel() == PetHungryLevel.FULL && reward != null) {
					PacketSendUtility.sendPacket(player, new SM_PET(2, action, item.getObjectId(), 0, pet));
				} else {
					PacketSendUtility.sendPacket(player, new SM_PET(2, action, item.getObjectId(), --count, pet));
				}
			} else {
				PacketSendUtility.sendPacket(player, new SM_PET(5, action, 0, 0, pet));
				PacketSendUtility.sendPacket(player,
						new SM_EMOTION(player, EmotionType.END_FEEDING, 0, player.getObjectId()));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
						.STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR(pet.getName(), item.getItemTemplate().getNameId()));
				return;
			}
			if (progress.getHungryLevel() == PetHungryLevel.FULL && reward != null) {
				PacketSendUtility.sendPacket(player, new SM_PET(6, action, reward.getItem(), 0, pet));
				PacketSendUtility.sendPacket(player, new SM_PET(5, action, 0, 0, pet));
				PacketSendUtility.sendPacket(player,
						new SM_EMOTION(player, EmotionType.END_FEEDING, 0, player.getObjectId()));
				PacketSendUtility.sendPacket(player, new SM_PET(7, action, 0, 0, pet));
				ItemService.addItem(player, reward.getItem(), 1);
				commonData.setReFoodTime(flavour.getCooldDown() * 60000);
				commonData.setCurentTime(System.currentTimeMillis());
				DAOManager.getDAO(PlayerPetsDAO.class).setTime(player, pet.getPetId(), System.currentTimeMillis());
				progress.reset();
			} else if (count > 0) {
				schedule(pet, player, item, count, action);
			} else {
				PacketSendUtility.sendPacket(player, new SM_PET(5, action, 0, 0, pet));
				PacketSendUtility.sendPacket(player,
						new SM_EMOTION(player, EmotionType.END_FEEDING, 0, player.getObjectId()));
			}
		}
	}

	/**
	 * 调整宠物增益背包中卷轴槽位。
	 * Relocate doping bag scroll slots for the pet.
	 *
	 * 玩家 / Player
	 * Source slot
	 * Destination slot
	 */
	public void relocateDoping(Player player, int targetSlot, int destinationSlot) {
		Pet pet = player.getPet();
		if (pet == null || pet.getCommonData().getDopingBag() == null) {
			return;
		}
		int[] scrollBag = pet.getCommonData().getDopingBag().getScrollsUsed();
		int targetItem = scrollBag[targetSlot - 2];
		if (destinationSlot - 2 > scrollBag.length - 1) {
			pet.getCommonData().getDopingBag().setItem(targetItem, destinationSlot);
			PacketSendUtility.sendPacket(player, new SM_PET(0, targetItem, destinationSlot));
			pet.getCommonData().getDopingBag().setItem(0, targetSlot);
			PacketSendUtility.sendPacket(player, new SM_PET(0, 0, targetSlot));
		} else {
			pet.getCommonData().getDopingBag().setItem(scrollBag[destinationSlot - 2], targetSlot);
			PacketSendUtility.sendPacket(player, new SM_PET(0, scrollBag[destinationSlot - 2], targetSlot));
			pet.getCommonData().getDopingBag().setItem(targetItem, destinationSlot);
			PacketSendUtility.sendPacket(player, new SM_PET(0, targetItem, destinationSlot));
		}
	}

	/**
	 * 使用或配置宠物增益背包中的物品。
	 * Use or configure items in the pet doping bag.
	 *
	 * 玩家 / Player
	 * Action type
	 * Item template id
	 * Slot
	 */
	public void useDoping(final Player player, int action, int itemId, int slot) {
		Pet pet = player.getPet();
		if (pet == null || pet.getCommonData().getDopingBag() == null) {
			return;
		}
		if (action < 2) {
			pet.getCommonData().getDopingBag().setItem(itemId, slot);
			action = 0;
		} else if (action == 3) {
			List<Item> items = player.getInventory().getItemsByItemId(itemId);
			for (;;) {
				Item useItem = items.get(0);
				ItemActions itemActions = useItem.getItemTemplate().getActions();
				ItemUseLimits limit = new ItemUseLimits();
				int useDelay = player.getItemCooldown(useItem.getItemTemplate()) / 3;
				if (useDelay < 3000) {
					useDelay = 3000;
				}
				limit.setDelayId(useItem.getItemTemplate().getUseLimits().getDelayId());
				limit.setDelayTime(useDelay);
				if (player.isItemUseDisabled(limit)) {
					final int useAction = action;
					final int useItemId = itemId;
					final int useSlot = slot;
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							PacketSendUtility.sendPacket(player, new SM_PET(useAction, useItemId, useSlot));
						}
					}, useDelay);
					return;
				}
				if (!RestrictionsManager.canUseItem(player, useItem) || player.isProtectionActive()) {
					player.addItemCoolDown(limit.getDelayId(), System.currentTimeMillis() + useDelay, useDelay / 1000);
					break;
				}
				player.getController().cancelCurrentSkill();
				for (AbstractItemAction itemAction : itemActions.getItemActions()) {
					if (itemAction.canAct(player, useItem, null)) {
						itemAction.act(player, useItem, null);
					}
				}
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_PET(action, itemId, slot));
		itemId = pet.getCommonData().getDopingBag().getFoodItem();
		long totalDopes = player.getInventory().getItemCountByItemId(itemId);
		itemId = pet.getCommonData().getDopingBag().getDrinkItem();
		totalDopes += player.getInventory().getItemCountByItemId(itemId);
		int[] scrollBag = pet.getCommonData().getDopingBag().getScrollsUsed();
		for (int i = 0; i < scrollBag.length; i++) {
			if (scrollBag[i] != 0) {
				totalDopes += player.getInventory().getItemCountByItemId(scrollBag[i]);
			}
		}
		if (totalDopes == 0) {
			pet.getCommonData().setIsBuffing(false);
			PacketSendUtility.sendPacket(player, new SM_PET(1, false));
		}
	}

	/**
	 * 开启或关闭宠物自动拾取。
	 * Enable or disable pet auto-loot.
	 *
	 * 玩家 / Player
	 * Whether to activate
	 */
	public void activateLoot(final Player player, final boolean activate) {
		if (player.getPet() == null) {
			return;
		}
		if (activate) {
			if (player.isInTeam()) {
				LootRuleType lootType = player.getLootGroupRules().getLootRule();
				if (lootType == LootRuleType.FREEFORALL) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LOOTING_PET_MESSAGE03);
					return;
				}
			}
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LOOTING_PET_MESSAGE01);
		}
		player.getPet().getCommonData().setIsLooting(activate);
		PacketSendUtility.sendPacket(player, new SM_PET(activate));
	}

	/**
	 * 开启或关闭宠物欢呼增益（消耗奥德樱桃）。
	 * Enable or disable pet cheer buff (consumes Aether Cherry).
	 *
	 * 玩家 / Player
	 * Whether to activate
	 */
	public void activateBuff(final Player player, final boolean activate) {
		if (player.getPet() == null) {
			return;
		}
		Pet pet = player.getPet();
		PetTemplate petTemp = DataManager.PET_DATA.getPetTemplate(pet.getPetId());
		PetBonusAttr petBuff = DataManager.PET_BUFF_DATA
				.getPetBonusattr(petTemp.getPetFunction(PetFunctionType.CHEER).getId());

		if (activate && player.getInventory().getItemCountByItemId(182007162) < petBuff.getFoodCount()) {// Aether
																											// 樱桃 / Cherry
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_BUFF_PET_USE_STOP_MESSAGE_03);
			return;
		}
		if (activate) {
			autoBuff = true;
			PetBuff = new PetBuff(petBuff.getBuffId());
			PetBuff.applyEffect(player, 300000);
			player.getInventory().decreaseByItemId(182007162, petBuff.getFoodCount());
		} else {
			autoBuff = false;
			PetBuff.endEffect(player);
		}
	}

	/**
	 * 开启或关闭宠物自动出售。
	 * Enable or disable pet auto-sell.
	 *
	 * 玩家 / Player
	 * Whether to activate
	 */
	public void activeAutoSell(final Player player, final boolean activate) {
		if (player.getPet() == null) {
			return;
		}
		if (activate) {
			player.getPet().getCommonData().setIsSelling(activate);
		}
	}

	/**
	 * 玩家登出时关闭自动增益与自动出售。
	 * Disable auto buff and auto sell on player logout.
	 *
	 * @param player 玩家 / Player
	 */
	public void onPlayerLogout(Player player) {
		if (autoBuff) {
			activateBuff(player, false);
		}
		if (autoSeel) {
			activeAutoSell(player, false);
		}
	}

	/**
	 * 强制关闭当前宠物欢呼增益。
	 * Force-switch off the current pet cheer buff.
	 *
	 * @param player 玩家 / Player
	 */
	public void switchOffBuff(final Player player) {
		Pet pet = player.getPet();
		if (pet != null) {
			if (player.getPet().getPetTemplate().getPetFunction(PetFunctionType.CHEER) != null) {
				if (autoBuff) {
					autoBuff = false;
					PetBuff.endEffect(player);
				}
			}
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final PetService instance = new PetService();
	}
}
