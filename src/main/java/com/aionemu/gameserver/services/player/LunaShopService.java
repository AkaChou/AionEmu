package com.aionemu.gameserver.services.player;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.dao.PlayerWardrobeDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerLunaShop;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.luna.LunaConsumeRewardsTemplate;
import com.aionemu.gameserver.model.templates.recipe.LunaComponent;
import com.aionemu.gameserver.model.templates.recipe.LunaComponentElement;
import com.aionemu.gameserver.model.templates.recipe.LunaTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 露娜商店服务，管理露娜点数、每日工艺与特价。
 * Luna shop service managing luna points, daily craft and specials.
 */

@Slf4j

public class LunaShopService {

	static final int MATERIAL_BOX_PRICE = 2;
	static final int WARDROBE_APPEARANCE_PRICE = 12;
	static final int LUNA_INSTANCE_PRICE = 20;
	static final int TREASURE_CHEST_PRICE = 5;

	private static volatile ObjectProvider<LunaShopService> instanceProvider;
	PlayerWardrobeDAO wDAO = DAOManager.getDAO(PlayerWardrobeDAO.class);
	private boolean dailyGenerated = true;
	private boolean specialGenerated = true;
	private boolean reciveBonus = false;
	private List<Integer> DailyCraft = new ArrayList<Integer>();
	private List<Integer> SpecialCraft = new ArrayList<Integer>();
	private List<Integer> armors = new ArrayList<Integer>();
	private List<Integer> pants = new ArrayList<Integer>();
	private List<Integer> shoes = new ArrayList<Integer>();
	private List<Integer> gloves = new ArrayList<Integer>();
	private List<Integer> shoulders = new ArrayList<Integer>();
	private List<Integer> weapons = new ArrayList<Integer>();

	public void init() {
		log.info(I18n.get("log.54a853f1dff1"));
		String daily = "0 0 9 1/1 * ? *";
		String weekly = "0 0 9 ? * WED *";
		if (DailyCraft.size() == 0) {
			generateDailyCraft();
		}
		if (SpecialCraft.size() == 0) {
			generateSpecialCraft();
		}

		GameCronServices.cronService().schedule(new Runnable() {
			public void run() {
				dailyGenerated = false;
				generateDailyCraft();
				resetFreeLuna();
			}
		}, daily);

		GameCronServices.cronService().schedule(new Runnable() {
			public void run() {
				specialGenerated = false;
				generateSpecialCraft();
			}
		}, weekly);
	}

	public void generateSpecialCraft() {
		if (SpecialCraft.size() > 0) {
			SpecialCraft.clear();
		}
		armors.add(10029);
		armors.add(10031);
		armors.add(10033);
		armors.add(10035);
		pants.add(10037);
		pants.add(10039);
		pants.add(10041);
		pants.add(10043);
		shoes.add(10061);
		shoes.add(10063);
		shoes.add(10065);
		shoes.add(10067);
		gloves.add(10053);
		gloves.add(10055);
		gloves.add(10057);
		gloves.add(10059);
		shoulders.add(10045);
		shoulders.add(10047);
		shoulders.add(10049);
		shoulders.add(10051);
		weapons.add(10021);
		weapons.add(10017);
		weapons.add(10025);
		weapons.add(10005);
		weapons.add(10011);
		weapons.add(10023);
		weapons.add(10003);
		weapons.add(10007);
		weapons.add(10019);
		weapons.add(10013);
		weapons.add(10027);
		weapons.add(10009);
		weapons.add(10015);
		weapons.add(10001);
		int rnd = Rnd.get(1, 6);
		switch (rnd) {
		case 1:
			SpecialCraft.addAll(weapons);
			break;
		case 2:
			SpecialCraft.addAll(armors);
			break;
		case 3:
			SpecialCraft.addAll(pants);
			break;
		case 4:
			SpecialCraft.addAll(shoes);
			break;
		case 5:
			SpecialCraft.addAll(gloves);
			break;
		case 6:
			SpecialCraft.addAll(shoulders);
			break;
		}
		if (!specialGenerated) {
			updateSpecialCraft();
		}
	}

	public void resetFreeLuna() {
		DAOManager.getDAO(PlayerLunaShopDAO.class).delete();
		updateFreeLuna();
	}

	public void sendSpecialCraft(Player player) {
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(2, 0, SpecialCraft));
	}

	private void updateSpecialCraft() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(2, 0, SpecialCraft));
			}
		});
	}

	private void updateFreeLuna() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {

			@Override
			public void visit(Player player) {
				PlayerLunaShop pls = new PlayerLunaShop(true, true, true);
				pls.setPersistentState(PersistentState.UPDATE_REQUIRED);
				player.setPlayerLunaShop(pls);
				DAOManager.getDAO(PlayerLunaShopDAO.class).add(player.getObjectId(), pls.isFreeUnderpath(),
						pls.isFreeFactory(), pls.isFreeChest());
			}
		});
	}

	public void generateDailyCraft() {
		if (DailyCraft.size() > 0) {
			dailyGenerated = false;
			DailyCraft.clear();
		}

		List<LunaTemplate> test = DataManager.LUNA_DATA.getLunaTemplatesAny();
		Random rand = new Random();
		for (int i = 0; i < 5; i++) {
			int randomIndex = rand.nextInt(test.size());
			LunaTemplate randomElement = test.get(randomIndex);
			DailyCraft.add(randomElement.getId());
		}

		if (!dailyGenerated) {
			updateDailyCraft();
		}
	}

	public void sendDailyCraft(Player player) {
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(DailyCraft));
	}

	private void updateDailyCraft() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(DailyCraft));
				dailyGenerated = true;
			}
		});
	}

	/**
	 * 露娜点数控制。
	 * Luna point controller.
	 *
	 * @param player 玩家 / player
	 * @param point 点数 / points
	 */
	public void lunaPointController(Player player, int point) {
		player.setLunaAccount(point);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
	}

	/**
	 * Muni 钥匙控制器。
	 * Muni keys controller.
	 *
	 * @param player 玩家 / player
	 * @param keys 钥匙数量 / key count
	 */
	public void muniKeysController(Player player, int keys) {
		player.setMuniKeys(keys);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4));
	}

	/**
	 * 玩家登录时同步状态。
	 * Syncs state when a player logs in.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		if (player.getPlayerLunaShop() == null) {
			PlayerLunaShop pls = new PlayerLunaShop(true, true, true);
			pls.setPersistentState(PersistentState.UPDATE_REQUIRED);
			player.setPlayerLunaShop(pls);
			DAOManager.getDAO(PlayerLunaShopDAO.class).add(player.getObjectId(), pls.isFreeUnderpath(),
					pls.isFreeFactory(), pls.isFreeChest());
		}

		// PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(6));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(7));
		sendSpecialCraft(player);
		sendDailyCraft(player);
		for (int i = 0; i < 9; i++) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(8, i, 0));
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4, player.getMuniKeys()));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(9, 0));

		if (!player.getPlayerLunaShop().isFreeUnderpath()) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 45));
		}
		if (!player.getPlayerLunaShop().isFreeFactory()) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 47));
		}
	}

	/**
	 * 特殊设计处理。
	 * Special design handler.
	 *
	 * @param player 玩家 / player
	 * @param recipeId 配方 ID / recipe id
	 */
	public void specialDesign(Player player, int recipeId) {
		LunaTemplate recipe = DataManager.LUNA_DATA.getLunaTemplateById(recipeId);
		int product_id = recipe.getProductid();
		int quantity = recipe.getQuantity();
		ItemTemplate item = DataManager.ITEM_DATA.getItemTemplate(product_id);
		boolean isSuccess = isSuccess(player, recipeId);
		if (isSuccess) {
			for (LunaComponent lc : recipe.getLunaComponent()) {
				for (LunaComponentElement a : lc.getComponents()) {
					if (!player.getInventory().decreaseByItemId(a.getItemid(), a.getQuantity())) {
						log.warn(I18n.get("log.a2657e1e13e5", player.getObjectId(), recipeId, a.getItemid(), a.getQuantity()));
						PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(2, item, 1));
						PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(3, product_id, quantity, false));
						return;
					}
				}
			}
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(2, item, 0));
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(3, product_id, quantity, true));
			ItemService.addItem(player, product_id, quantity);
		} else {
			for (LunaComponent lc : recipe.getLunaComponent()) {
				for (LunaComponentElement a : lc.getComponents()) {
					if (!player.getInventory().decreaseByItemId(a.getItemid(), a.getQuantity())) {
						log.warn(I18n.get("log.a2657e1e13e5", player.getObjectId(), recipeId, a.getItemid(), a.getQuantity()));
						PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(2, item, 1));
						PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(3, product_id, quantity, false));
						return;
					}
				}
			}
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(2, item, 1));
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(3, product_id, quantity, false));
		}
	}

	/**
	 * craftBox 方法。
	 * craftBox method.
	 *
	 * @param player 玩家 / player
	 */
	public void craftBox(Player player) {
		int itemId = 188055460;
		if (player.getPlayerLunaShop().isFreeChest()) {
			player.getPlayerLunaShop().setFreeChest(false);
		} else if (!spendLuna(player, MATERIAL_BOX_PRICE)) {
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(3, itemId, 1, true));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 1));
		ItemService.addItem(player, itemId, 1); // Luna Material Chest
	}

	private boolean isSuccess(Player player, int recipeId) {
		LunaTemplate recipe = DataManager.LUNA_DATA.getLunaTemplateById(recipeId);
		boolean result = false;
		float random = Rnd.get(1, 100);
		if (recipe.getRate() == 100) {
			result = true;
		} else if (recipe.getRate() < 100) {
			if (random <= recipe.getRate()) {
				result = true;
			} else {
				result = false;
			}
		}
		return result;
	}

	/**
	 * 购买材料。
	 * Buys materials.
	 *
	 * 玩家 / player
	 * itemId
	 * count
	 */
	public void buyMaterials(Player player, int itemId, long count) {
		ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (count <= 0 || itemTemplate == null || itemTemplate.getLunaPrice() < 0) {
			log.warn(I18n.get("log.a3e3447b644d", player.getObjectId(), itemId, count));
			return;
		}
		int lunaPrice = itemTemplate.getLunaPrice();
		long price;
		try {
			price = Math.multiplyExact(count, lunaPrice);
		} catch (ArithmeticException e) {
			log.warn(I18n.get("log.dd3d4bc0be5f", player.getObjectId(),
					itemId, count, lunaPrice));
			return;
		}
		if (!spendLuna(player, price)) {
			return;
		}
		ItemService.addItem(player, itemId, count);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(4, player.getMuniKeys()));
	}

	/**
	 * dorinerkWardrobeLoad 方法。
	 * dorinerkWardrobeLoad method.
	 *
	 * @param player 玩家 / player
	 */
	public void dorinerkWardrobeLoad(Player player) {
		int size = DAOManager.getDAO(PlayerWardrobeDAO.class).getItemSize(player.getObjectId());
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(8, player.getWardrobeSlot(), size));
	}

	/**
	 * dorinerkWardrobeAct 方法。
	 * dorinerkWardrobeAct method.
	 *
	 * 玩家 / player
	 * applySlot
	 * itemObjId
	 */
	public void dorinerkWardrobeAct(Player player, int applySlot, int itemObjId) {
		int itemId = player.getInventory().getItemByObjId(itemObjId).getItemId();
		int itemOnDB = DAOManager.getDAO(PlayerWardrobeDAO.class).getWardrobeItemBySlot(player.getObjectId(),
				applySlot);
		if (itemOnDB != 0) {
			DAOManager.getDAO(PlayerWardrobeDAO.class).delete(player.getObjectId(), itemOnDB);
			player.getWardrobe().addItem(player, itemId, applySlot, 0);
		} else {
			player.getWardrobe().addItem(player, itemId, applySlot, 0);
		}
		player.getInventory().decreaseByObjectId(itemObjId, 1);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(10, 0x00, applySlot, itemId, 1));
	}

	/**
	 * dorinerkWardrobeModifyAppearance 方法。
	 * dorinerkWardrobeModifyAppearance method.
	 *
	 * 玩家 / player
	 * applySlot
	 * itemObjId
	 */
	public void dorinerkWardrobeModifyAppearance(Player player, int applySlot, int itemObjId) {
		int itemId = DAOManager.getDAO(PlayerWardrobeDAO.class).getWardrobeItemBySlot(player.getObjectId(), applySlot);
		int reskinCount = DAOManager.getDAO(PlayerWardrobeDAO.class).getReskinCountBySlot(player.getObjectId(),
				applySlot);
		ItemTemplate it = DataManager.ITEM_DATA.getItemTemplate(itemId);
		Storage inventory = player.getInventory();
		Item keepItem = inventory.getItemByObjId(itemObjId);
		if (reskinCount != 0) {
			if (!spendLuna(player, WARDROBE_APPEARANCE_PRICE)) {
				return;
			}
			DAOManager.getDAO(PlayerWardrobeDAO.class).setReskinCountBySlot(player.getObjectId(), applySlot,
					reskinCount + 1);
			keepItem.setItemSkinTemplate(it);
			if (!keepItem.getItemTemplate().isItemDyePermitted()) {
				keepItem.setItemColor(0);
			}
			keepItem.setLunaReskin(true);
		} else {
			DAOManager.getDAO(PlayerWardrobeDAO.class).setReskinCountBySlot(player.getObjectId(), applySlot,
					reskinCount + 1);
			keepItem.setItemSkinTemplate(it);
			if (!keepItem.getItemTemplate().isItemDyePermitted()) {
				keepItem.setItemColor(0);
			}
			keepItem.setLunaReskin(true);
		}
		ItemPacketService.updateItemAfterInfoChange(player, keepItem, ItemUpdateType.STATS_CHANGE);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
				.STR_CHANGE_ITEM_SKIN_SUCCEED(new DescriptionId(keepItem.getItemTemplate().getNameId())));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(11, applySlot));
	}

	/**
	 * dorinerkWardrobeExtendSlots 方法。
	 * dorinerkWardrobeExtendSlots method.
	 *
	 * @param player 玩家 / player
	 */
	public void dorinerkWardrobeExtendSlots(Player player) {
		int currentSlot = player.getWardrobeSlot();
		int size = DAOManager.getDAO(PlayerWardrobeDAO.class).getItemSize(player.getObjectId());
		if (!spendLuna(player, wardrobePrice(currentSlot + 1))) {
			return;
		}
		player.setWardrobeSlot(currentSlot + 1);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(9, player.getWardrobeSlot(), size));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5, player.getLunaAccount()));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4, player.getMuniKeys()));
	}

	/**
	 * takiAdventure 方法。
	 * takiAdventure method.
	 *
	 * 玩家 / player
	 * indun_id
	 */
	public void takiAdventure(Player player, int indun_id) {
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(14, indun_id));
	}

	/**
	 * takiAdventureTeleport 方法。
	 * takiAdventureTeleport method.
	 *
	 * 玩家 / player
	 * @param indun_unk 副本未知字段 / indun_unk
	 * indun_id
	 */
	public void takiAdventureTeleport(Player player, int indun_unk, int indun_id) {
		if (indun_id == 1) {
			boolean free = player.getPlayerLunaShop().isFreeUnderpath();
			if (!free && !spendLuna(player, LUNA_INSTANCE_PRICE)) {
				return;
			}
			WorldMapInstance contaminatedUnderpath = InstanceService.getNextAvailableInstance(301630000);
			InstanceService.registerPlayerWithInstance(contaminatedUnderpath, player);
			TeleportService2.teleportTo(player, 301630000, contaminatedUnderpath.getInstanceId(), 230f, 169f, 164f,
					(byte) 60);
			if (free) {
				player.getPlayerLunaShop().setLunaShopByObjId(player.getObjectId());
				player.getPlayerLunaShop().setFreeUnderpath(false);
			}
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 45));
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(0, 0));
		}
		if (indun_id == 2) {
			boolean free = player.getPlayerLunaShop().isFreeFactory();
			if (!free && !spendLuna(player, LUNA_INSTANCE_PRICE)) {
				return;
			}
			WorldMapInstance secretMunitionsFactory = InstanceService.getNextAvailableInstance(301640000);
			InstanceService.registerPlayerWithInstance(secretMunitionsFactory, player);
			TeleportService2.teleportTo(player, 301640000, secretMunitionsFactory.getInstanceId(), 400.3279f,
					290.5061f, 198.64015f, (byte) 60);
			if (free) {
				player.getPlayerLunaShop().setLunaShopByObjId(player.getObjectId());
				player.getPlayerLunaShop().setFreeFactory(false);
			}
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 47));
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(0, 0));
		}
	}

	/**
	 * teleport 方法。
	 * teleport method.
	 *
	 * 玩家 / player
	 * action
	 * teleportId
	 */
	public void teleport(Player player, int action, int teleportId) {
		switch (action) {
		case 6:
			PacketSendUtility.sendMessage(player, "teleportId : " + teleportId);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(6));
			break;
		case 7:
			PacketSendUtility.sendMessage(player, "teleportId : " + teleportId);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(7));
			break;
		}
	}

	/**
	 * munirunerksTreasureChamber 方法。
	 * munirunerksTreasureChamber method.
	 *
	 * @param player 玩家 / player
	 */
	public void munirunerksTreasureChamber(final Player player) {
		HashMap<Integer, Long> hm = new HashMap<Integer, Long>();
		hm.put(188054633, (long) 1); // [Event] Special Head Executor Weapon Box
		hm.put(188054634, (long) 1); // [Event] Special Head Executor Armor Box
		hm.put(166030013, (long) 1); // [Event] Tempering Solution
		hm.put(166020003, (long) 1); // [Event] Omega Enchantment Stone
		hm.put(188054122, (long) 1); // Major Stigma Bundle
		hm.put(188055183, (long) 1); // Major Felicitous Socketing Box (Mythic)
		hm.put(188054287, (long) 1); // Greater Stigma Bundle
		hm.put(188054462, (long) 1); // Illusion Godstone Bundle
		hm.put(188052639, (long) 1); // [Event] Heroic Godstone Bundle
		hm.put(169405339, (long) 10); // Pallasite Crystal
		hm.put(164000076, (long) 10); // Greater Running Scroll
		hm.put(164000134, (long) 10); // Greater Awakening Scroll
		hm.put(166000196, (long) 3); // Enchantment Stone
		hm.put(186000242, (long) 2); // Ceramium Medal
		hm.put(186000051, (long) 2); // Major Ancient Crown
		hm.put(188055168, (long) 10); // [Event] Blood Medal Box
		hm.put(188054283, (long) 30); // Blood Mark Box
		hm.put(188054463, (long) 1); // [Event] Fabled Godstone Bundle
		hm.put(188053002, (long) 1); // [Event] Noble Composite Manastone Bundle
		hm.put(188100335, (long) 2000); // 强化石粉末 / Enchantment Stone Dust
		hm.put(164000073, (long) 10); // Greater Courage Scroll
		hm.put(160002497, (long) 1); // Fresh Oily Plucar Dragon Salad
		hm.put(160002499, (long) 1); // Fresh Oily Plucar Dragon Soup

		if (player.getMuniKeys() > 0) {
			player.setMuniKeys(player.getMuniKeys() - 1);
		} else {
			if (!spendLuna(player, TREASURE_CHEST_PRICE)) {
				return;
			}
			player.setLunaConsumePoint(player.getLunaConsumePoint() + TREASURE_CHEST_PRICE);
			switch (player.getLunaConsumePoint()) {
			case 25:
				reciveBonus = true;
				player.setLunaConsumeCount(1);
				break;
			case 50:
				reciveBonus = true;
				player.setLunaConsumeCount(2);
				break;
			case 100:
				reciveBonus = true;
				player.setLunaConsumeCount(3);
				muniKeysController(player, player.getMuniKeys() + 1);
				break;
			case 150:
				reciveBonus = true;
				player.setLunaConsumeCount(4);
				muniKeysController(player, player.getMuniKeys() + 1);
				break;
			case 300:
				reciveBonus = true;
				player.setLunaConsumeCount(5);
				muniKeysController(player, player.getMuniKeys() + 2);
				break;
			case 500:
				reciveBonus = true;
				player.setLunaConsumeCount(6);
				muniKeysController(player, player.getMuniKeys() + 2);
				break;
			case 1000:
				reciveBonus = true;
				player.setLunaConsumeCount(7);
				muniKeysController(player, player.getMuniKeys() + 3);
				break;
			default:
				reciveBonus = false;
				break;
			}
			if (reciveBonus) {
				LunaConsumeRewardsTemplate lt = DataManager.LUNA_CONSUME_REWARDS_DATA
						.getLunaConsumeRewardsId(player.getLunaConsumeCount());
				ItemService.addItem(player, lt.getCreateItemId(), lt.getCreateItemCount());
			}
		}

		final HashMap<Integer, Long> mt = new HashMap<Integer, Long>();
		for (int i = 0; i < 3; i++) {
			Object[] crunchifyKeys = hm.keySet().toArray();
			Object key = crunchifyKeys[new Random().nextInt(crunchifyKeys.length)];
			mt.put((int) key, (long) hm.get(key));
		}
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				for (Map.Entry<Integer, Long> e : mt.entrySet()) {
					ItemService.addItem(player, e.getKey(), e.getValue());
					ItemTemplate t = DataManager.ITEM_DATA.getItemTemplate(e.getKey());
					if (e.getValue() == 1) {
						PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LUNA_REWARD_GOTCHA_ITEM(t.getNameId()));
					} else if (e.getValue() > 1) {
						PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LUNA_REWARD_GOTCHA_ITEM_MULTI(e.getValue(), t.getNameId()));
					}
				}
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(mt));
			}
		}, 1);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4, player.getMuniKeys()));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
		// 花费露娜可获得打开穆尼伦克宝箱的钥匙。 / As you spend Luna, you can earn keys to open Munirunerks Treasure Chest.
		// 若没有钥匙，可花费 3 露娜开箱。 / If you do not have any keys, you can spend 3 Luna to open a chest
		// 立即。 / immediately.
		// 开箱消耗的露娜也会计入你的露娜统计。 / The Luna you spend on opening chests will also count towards your Luna
		// 奖励！ / Rewards!
	}

	/**
	 * 玩家登出时清理状态。
	 * Cleans state when a player logs out.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogout(Player player) {
		PlayerLunaShop pls = player.getPlayerLunaShop();
		pls.setPersistentState(PersistentState.UPDATE_REQUIRED);
		DAOManager.getDAO(PlayerLunaShopDAO.class).store(player);
	}

	static int wardrobePrice(int wardrobeSlot) {
		switch (wardrobeSlot) {
		case 3:
		case 4:
			return 10;
		case 5:
		case 6:
		case 7:
		case 8:
			return 12;
		}
		return -1;
	}

	/**
	 * diceGame 方法。
	 * diceGame method.
	 *
	 * @param player 玩家 / player
	 */
	public void diceGame(Player player) {
		int diceTry = player.getLunaDiceGameTry();
		int price = lunaDicePrice(diceTry);
		if (!spendLuna(player, price)) {
			return;
		}
		int random = Rnd.get(1, 1000);
		if (random >= 100 && random <= 400) {
			player.setLunaDiceGame(1, false);
		} else if (random >= 450 && random <= 749) {
			player.setLunaDiceGame(2, false);
		} else if (random >= 750 && random <= 849) {
			player.setLunaDiceGame(3, false);
		} else if (random >= 850 && random <= 900) {
			player.setLunaDiceGame(4, false);
		} else if (random >= 950 && random <= 1000) {
			player.setLunaDiceGame(5, false);
		}
		player.setLunaDiceGameTry(diceTry + 1);
		player.setLunaConsumePoint(player.getLunaConsumePoint() + price);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, diceTry == 0 ? 72 : 73));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(15));
		if (diceTry > 0) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4));
		}
		log.debug("Luna dice result. playerId={} random={} tries={} consumed={}", player.getObjectId(), random,
				player.getLunaDiceGameTry(), player.getLunaConsumePoint());
	}

	/**
	 * diceGameReward 方法。
	 * diceGameReward method.
	 *
	 * @param player 玩家 / player
	 */
	public void diceGameReward(Player player) {
		if (!canClaimDiceReward(player.getLunaDiceGameTry()) || ItemService.addItem(player, 162001014, 4) != 0) {
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(16, 162001014, 4));
		player.setLunaDiceGame(0, true);
		player.setLunaDiceGameTry(0);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 72));
	}

	static boolean canClaimDiceReward(int diceTry) {
		return diceTry > 0;
	}

	static int lunaDicePrice(int diceTry) {
		return diceTry >= 0 && diceTry <= 10 ? 3 : -1;
	}

	static boolean canSpendLuna(long balance, long price) {
		return price >= 0 && balance >= price;
	}

	private boolean spendLuna(Player player, long price) {
		synchronized (player.getClientConnection().getAccount()) {
			long balance = player.getLunaAccount();
			if (!canSpendLuna(balance, price)) {
				if (price < 0) {
					log.warn(I18n.get("log.962d3a2eb9ed", player.getObjectId(), price));
				} else {
					PacketSendUtility.sendMessage(player, "Not enough Luna.");
					PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0));
				}
				return false;
			}
			if (price == 0) {
				return true;
			}
			long remaining = balance - price;
			player.setLunaAccount(remaining);
			if (player.getLunaAccount() != remaining) {
				return false;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0));
		return true;
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static LunaShopService getInstance() {
		ObjectProvider<LunaShopService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<LunaShopService> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final LunaShopService INSTANCE = new LunaShopService();
	}
}
