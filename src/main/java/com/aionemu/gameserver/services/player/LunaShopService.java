package com.aionemu.gameserver.services.player;


import com.aionemu.boot.i18n.I18n;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.services.CronService;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerLunaShop;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.luna.LunaConsumeRewardsTemplate;
import com.aionemu.gameserver.model.templates.luna_dice.LunaDiceItem;
import com.aionemu.gameserver.model.templates.recipe.LunaComponent;
import com.aionemu.gameserver.model.templates.recipe.LunaComponentElement;
import com.aionemu.gameserver.model.templates.recipe.LunaTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.persistence.dao.PlayerLunaShopDao;
import com.aionemu.gameserver.persistence.dao.PlayerWardrobeDao;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 露娜商店服务，管理露娜点数、每日工艺与特价。
 * Luna shop service managing luna points, daily craft and specials.
 */

@Slf4j

public class LunaShopService {

	static final int MATERIAL_BOX_PRICE = 2;
	static final int WARDROBE_APPEARANCE_PRICE = 12;
	static final int LUNA_INSTANCE_PRICE = 20;
	/** 无钥匙时露娜直开宝物盒的价格。 / Price to open a treasure chest with Luna when out of keys. */
	static final int TREASURE_CHEST_PRICE = 5;
	/** 露娜骰子每轮价格；真端无每日次数上限，露娜余额即闸门（User::StartRollDice 仅有格子段位区间检查）。 */
	static final int LUNA_DICE_PRICE = 3;
	/** 黄金骰子每轮价格（实机客户端「黄金骰子 35」按钮显示口径）。 */
	static final int LUNA_GOLD_DICE_PRICE = 35;
	/** 掷满该次数且本局无掷空，进入黄金骰子阶段。 */
	static final int DICE_GOLD_STAGE_TRIES = 5;

	private static volatile ObjectProvider<LunaShopService> instanceProvider;
	private static volatile LunaShopService resolvedInstance;
	private final PlayerLunaShopDao playerLunaShopDAO;
	private final PlayerWardrobeDao playerWardrobeDAO;
	private boolean dailyGenerated = true;
	private boolean specialGenerated = true;
	private final List<Integer> DailyCraft = new ArrayList<>();
	private final List<Integer> SpecialCraft = new ArrayList<>();
	private final List<Integer> armors = new ArrayList<>();
	private final List<Integer> pants = new ArrayList<>();
	private final List<Integer> shoes = new ArrayList<>();
	private final List<Integer> gloves = new ArrayList<>();
	private final List<Integer> shoulders = new ArrayList<>();
	private final List<Integer> weapons = new ArrayList<>();

	/**
	 * 使用指定的 DAO 构造露娜商店服务。
	 * Constructs the Luna shop service with the given DAOs.
	 *
	 * @param playerLunaShopDAO 露娜商店数据访问对象 / Luna shop DAO
	 * @param playerWardrobeDAO 衣柜数据访问对象 / wardrobe DAO
	 */
	public LunaShopService(@NonNull PlayerLunaShopDao playerLunaShopDAO, @NonNull PlayerWardrobeDao playerWardrobeDAO) {
		this.playerLunaShopDAO = playerLunaShopDAO;
		this.playerWardrobeDAO = playerWardrobeDAO;
	}

	public void init() {
		log.info(I18n.get("log.54a853f1dff1"));
		String daily = "0 0 9 1/1 * ? *";
		String weekly = "0 0 9 ? * WED *";
		if (DailyCraft.isEmpty()) {
			generateDailyCraft();
		}
		if (SpecialCraft.isEmpty()) {
			generateSpecialCraft();
		}

		CronService.requireCurrent().schedule(() -> {
			dailyGenerated = false;
			generateDailyCraft();
			resetFreeLuna();
			resetDailyConsumeAchievements();
		}, daily);

		CronService.requireCurrent().schedule(() -> {
			specialGenerated = false;
			generateSpecialCraft();
		}, weekly);
	}

	public void generateSpecialCraft() {
		if (!SpecialCraft.isEmpty()) {
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
		playerLunaShopDAO.delete();
		updateFreeLuna();
	}

	/**
	 * 每天 9 点重置全体在线玩家的露娜消耗档位成就：消耗点与已领基线清零，钥匙保留（面板口径“每天上午 9 点，成就初始化”）。
	 * Resets the daily Luna consume ladder for all online players at 09:00 — points and claimed baseline reset, keys kept.
	 */
	public void resetDailyConsumeAchievements() {
		World.getInstance().doOnAllPlayers(player -> {
			player.setLunaConsumePoint(0);
			player.setLunaConsumeCount(0);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		});
	}

	public void sendSpecialCraft(Player player) {
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, SpecialCraft));
	}

	private void updateSpecialCraft() {
		World.getInstance().doOnAllPlayers(player -> PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, SpecialCraft)));
	}

	private void updateFreeLuna() {
		World.getInstance().doOnAllPlayers(player -> {
			PlayerLunaShop pls = new PlayerLunaShop(true, true, true);
			pls.setPersistentState(PersistentState.UPDATE_REQUIRED);
			player.setPlayerLunaShop(pls);
			playerLunaShopDAO.add(player.getObjectId(), pls.isFreeUnderpath(), pls.isFreeFactory(), pls.isFreeChest());
		});
	}

	public void generateDailyCraft() {
		if (!DailyCraft.isEmpty()) {
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
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, DailyCraft));
	}

	private void updateDailyCraft() {
		World.getInstance().doOnAllPlayers(player -> {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, DailyCraft));
			dailyGenerated = true;
		});
	}

	/**
	 * 露娜点数控制。
	 * Luna point controller.
	 * @param player 玩家 / player
	 * @param point 点数 / points
	 */
	public void lunaPointController(Player player, int point) {
		player.setLunaAccount(point);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0));
	}

	/**
	 * Muni 钥匙控制器。
	 * Muni keys controller.
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
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		if (player.getPlayerLunaShop() == null) {
			PlayerLunaShop pls = new PlayerLunaShop(true, true, true);
			pls.setPersistentState(PersistentState.UPDATE_REQUIRED);
			player.setPlayerLunaShop(pls);
			playerLunaShopDAO.add(player.getObjectId(), pls.isFreeUnderpath(), pls.isFreeFactory(), pls.isFreeChest());
		}

		if (needsDailyReset(player.getCommonData().getLastOnline())) {
			player.setLunaConsumePoint(0);
			player.setLunaConsumeCount(0);
		} else {
			// 同日内登录：补发已达成但因停服、网络或历史数据未到账的档位奖励。
			// Same-day login: delivers reached tiers that were missed due to restart, network, or legacy data.
			grantLunaConsumeRewards(player);
		}

		// PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(6));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(7));
		sendSpecialCraft(player);
		sendDailyCraft(player);
		for (int i = 0; i < 9; i++) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(8, i, 0));
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(9));

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
	 * @param player 玩家 / player
	 * @param recipeId 配方 ID / recipe id
	 */
	public void specialDesign(Player player, int recipeId) {
		LunaTemplate recipe = DataManager.LUNA_DATA.getLunaTemplateById(recipeId);
		int product_id = recipe.getProductid();
		int quantity = recipe.getQuantity();
		ItemTemplate item = DataManager.ITEM_DATA.getItemTemplate(product_id);
		boolean isSuccess = isSuccess(recipeId);
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
	 * @param player 玩家 / player
	 */
	public void craftBox(Player player) {
		int itemId = 188055460;
		if (player.getPlayerLunaShop().isFreeChest()) {
			player.getPlayerLunaShop().setFreeChest(false);
		} else if (spendLunaFailed(player, MATERIAL_BOX_PRICE)) {
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(3, itemId, 1, true));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 1));
		ItemService.addItem(player, itemId, 1); // Luna Material Chest
	}

	private boolean isSuccess(int recipeId) {
		LunaTemplate recipe = DataManager.LUNA_DATA.getLunaTemplateById(recipeId);
		boolean result = false;
		float random = Rnd.get(1, 100);
		if (recipe.getRate() == 100) {
			result = true;
		} else if (recipe.getRate() < 100) {
			result = random <= recipe.getRate();
		}
		return result;
	}

	/**
	 * 购买材料。
	 * Buys materials.
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
		if (spendLunaFailed(player, price)) {
			return;
		}
		ItemService.addItem(player, itemId, count);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(4, player.getMuniKeys()));
	}

	/**
	 * dorinerkWardrobeLoad 方法。
	 * dorinerkWardrobeLoad method.
	 * @param player 玩家 / player
	 */
	public void dorinerkWardrobeLoad(Player player) {
		int size = playerWardrobeDAO.getItemSize(player.getObjectId());
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(8, player.getWardrobeSlot(), size));
	}

	/**
	 * dorinerkWardrobeAct 方法。
	 * dorinerkWardrobeAct method.
	 * 玩家 / player
	 * applySlot
	 * itemObjId
	 */
	public void dorinerkWardrobeAct(Player player, int applySlot, int itemObjId) {
		Item item = player.getInventory().getItemByObjId(itemObjId);
		// 物品不存在时静默 NPE 会让客户端等不到回包而卡死；回失败包。
		// A missing item would NPE and hang the client waiting for a reply; answer with a failure.
		if (item == null || player.getWardrobe() == null) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(10, 0x01, applySlot, 0, 0));
			return;
		}
		int itemId = item.getItemId();
		int itemOnDB = playerWardrobeDAO.getWardrobeItemBySlot(player.getObjectId(), applySlot);
		if (itemOnDB != 0) {
			playerWardrobeDAO.delete(player.getObjectId(), itemOnDB);
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
	 * 玩家 / player
	 * applySlot
	 * itemObjId
	 */
	public void dorinerkWardrobeModifyAppearance(Player player, int applySlot, int itemObjId) {
		int itemId = playerWardrobeDAO.getWardrobeItemBySlot(player.getObjectId(), applySlot);
		int reskinCount = playerWardrobeDAO.getReskinCountBySlot(player.getObjectId(), applySlot);
		ItemTemplate it = DataManager.ITEM_DATA.getItemTemplate(itemId);
		Storage inventory = player.getInventory();
		Item keepItem = inventory.getItemByObjId(itemObjId);
		// 物品或外观模板缺失时静默 NPE 会让客户端卡死；回失败包。
		// A missing item or skin template would NPE and hang the client; answer with a failure.
		if (keepItem == null || it == null) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(11, applySlot));
			return;
		}
		if (reskinCount != 0 && spendLunaFailed(player, WARDROBE_APPEARANCE_PRICE)) {
			return;
		}
		playerWardrobeDAO.setReskinCountBySlot(player.getObjectId(), applySlot, reskinCount + 1);
		keepItem.setItemSkinTemplate(it);
		if (!keepItem.getItemTemplate().isItemDyePermitted()) {
			keepItem.setItemColor(0);
		}
		keepItem.setLunaReskin(true);
		ItemPacketService.updateItemAfterInfoChange(player, keepItem, ItemUpdateType.STATS_CHANGE);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
				.STR_CHANGE_ITEM_SKIN_SUCCEED(new DescriptionId(keepItem.getItemTemplate().getNameId())));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(11, applySlot));
	}

	/**
	 * dorinerkWardrobeExtendSlots 方法。
	 * dorinerkWardrobeExtendSlots method.
	 * @param player 玩家 / player
	 */
	public void dorinerkWardrobeExtendSlots(Player player) {
		int currentSlot = player.getWardrobeSlot();
		int size = playerWardrobeDAO.getItemSize(player.getObjectId());
		int price = wardrobePrice(currentSlot + 1);
		if (price < 0) {
			// 衣柜已满 8 格：必须回当前状态包，否则客户端等待响应会卡死 UI。
			// Wardrobe already at max slots: echo current state or the client UI hangs waiting.
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(9, currentSlot, size));
			return;
		}
		if (spendLunaFailed(player, price)) {
			return;
		}
		player.setWardrobeSlot(currentSlot + 1);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(9, player.getWardrobeSlot(), size));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4));
	}

	/**
	 * takiAdventure 方法。
	 * takiAdventure method.
	 * 玩家 / player
	 * indun_id
	 */
	public void takiAdventure(Player player, int indun_id) {
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(14, indun_id));
	}

	/**
	 * takiAdventureTeleport 方法。
	 * takiAdventureTeleport method.
	 * 玩家 / player
	 * indun_id
	 */
	public void takiAdventureTeleport(Player player, int indun_id) {
		if (indun_id == 1) {
			boolean free = player.getPlayerLunaShop().isFreeUnderpath();
			if (!free && spendLunaFailed(player, LUNA_INSTANCE_PRICE)) {
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
			if (!free && spendLunaFailed(player, LUNA_INSTANCE_PRICE)) {
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
	 * @param player 玩家 / player
	 */
	public void munirunerksTreasureChamber(final Player player) {
		HashMap<Integer, Long> hm = new HashMap<>();
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
		hm.put(188054463, (long) 1); // [扎库隆]唯一神石包袱 / [Event] Fabled Godstone Bundle
		hm.put(188053002, (long) 1); // [Event] Noble Composite Manastone Bundle
		hm.put(188100335, (long) 2000); // 强化石粉末 / Enchantment Stone Dust
		hm.put(164000073, (long) 10); // Greater Courage Scroll
		hm.put(160002497, (long) 1); // Fresh Oily Plucar Dragon Salad
		hm.put(160002499, (long) 1); // Fresh Oily Plucar Dragon Soup

		if (player.getMuniKeys() > 0) {
			player.setMuniKeys(player.getMuniKeys() - 1);
		} else if (spendLunaFailed(player, TREASURE_CHEST_PRICE)) {
			return;
		}

		final HashMap<Integer, Long> mt = new HashMap<>();
		List<Integer> keys = new ArrayList<>(hm.keySet());
		Collections.shuffle(keys);
		for (int i = 0; i < Math.min(3, keys.size()); i++) {
			int key = keys.get(i);
			mt.put(key, hm.get(key));
		}
        ThreadPoolManager.getInstance().schedule(() -> {
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
		 }, 1);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0));
		// 消耗露娜的成就档位发放宝物盒钥匙；开盒优先用钥匙，无钥匙则以露娜直开并照常计入消耗档位。
		// Luna consume tiers grant treasure keys; opening prefers a key and falls back to Luna, which counts toward the tiers.
	}

	static int wardrobePrice(int wardrobeSlot) {
        return switch (wardrobeSlot) {
            case 3, 4 -> 10;
            case 5, 6, 7, 8 -> 12;
            default -> -1;
        };
    }

	/**
	 * diceGame 方法。
	 * diceGame method.
	 * @param player 玩家 / player
	 */
	public void diceGame(Player player) {
		int diceTry = player.getLunaDiceGameTry();
		// 黄金骰子：5 槽全微笑（推进制下 = 掷满 5 次）后可掷，35 露娜/次，掷出 C..SS 五档之一，结算按档发放。
		// Gold dice: after all 5 slots smile (5 rolls in the progressive model), 35 luna each, yields grade C..SS.
		if (diceTry >= DICE_GOLD_STAGE_TRIES) {
			// 真端黄金标志未清（未领奖）时拒绝再次黄金掷，否则会重复扣费并覆盖待领档位。
			// Retail rejects another gold roll while the gold flag is set (reward not claimed yet).
			if (player.getLunaGoldDiceGrade() > 0) {
				PacketSendUtility.sendPacket(player, SM_LUNA_SHOP.diceRollResult(1, -1, false));
				return;
			}
			if (spendLunaFailed(player, LUNA_GOLD_DICE_PRICE)) {
				PacketSendUtility.sendPacket(player, SM_LUNA_SHOP.diceRollResult(2, -1, false));
				return;
			}
			int grade = rollGoldGrade();
			player.setLunaGoldDiceGrade(grade);
			player.setLunaDiceGameTry(diceTry + 1);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 74));
			// specialFlag=1 + 开牌序号 5..7 才会让客户端翻金骰、切宝箱档并显示领取按钮（真端 finalOpenNum 同源）。
			// specialFlag=1 with open number 5..7 is what makes the client flip the gold dice, retier the chest and show the claim button.
			PacketSendUtility.sendPacket(player, SM_LUNA_SHOP.diceRollResult(0, goldDiceOpenNum(grade), true));
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4));
			log.debug("Luna gold dice roll. playerId={} grade={}", player.getObjectId(), grade);
			return;
		}
		// 普通掷：从右到左确定性点亮一个微笑猫，3 露娜/次（蓝猫 = 未填槽的默认图，无随机失败）。
		// Normal roll: deterministically lights the next smile shugo right-to-left, 3 luna each (no random miss).
		if (spendLunaFailed(player, LUNA_DICE_PRICE)) {
			// 真端拒绝路径必回掷骰结果包（result=2 露娜不足、dice=-1），否则客户端自动掷骰会卡死。
			// The retail server always answers a rejected roll (result=2, dice=-1) or the client auto-roll hangs.
			PacketSendUtility.sendPacket(player, SM_LUNA_SHOP.diceRollResult(2, -1, false));
			return;
		}
		int progress = diceTry + 1;
		player.setLunaDiceGame(progress, true);
		player.setLunaDiceGameTry(progress);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, diceTry == 0 ? 72 : 73));
		PacketSendUtility.sendPacket(player, SM_LUNA_SHOP.diceRollResult(0, progress, false));
		if (diceTry > 0) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4));
		}
		log.debug("Luna dice roll. playerId={} progress={}", player.getObjectId(), progress);
	}

	/**
	 * diceGameReward 方法。
	 * diceGameReward method.
	 * @param player 玩家 / player
	 */
	public void diceGameReward(Player player) {
		int goldGrade = player.getLunaGoldDiceGrade();
		int diceTry = player.getLunaDiceGameTry();
		// 真端按当前 openNum 找到对应奖励表（普通掷同样可领，档位随微笑数提升），黄金掷则用其档位。
		// Retail resolves the reward table from the current openNum (normal rolls are claimable too); gold rolls use their grade.
		LunaDiceItem reward = rollDiceReward(goldGrade > 0 ? goldGrade : rewardTableFor(diceTry));
		int rewardItemId = reward != null ? reward.getItemId() : 0;
		long rewardCount = reward != null ? reward.getCount() : 0;
		if (!canClaimDiceReward(diceTry) || rewardItemId <= 0
				|| ItemService.addItem(player, rewardItemId, rewardCount) != 0) {
			// 真端领奖失败回 result=1 + 空列表；静默 return 会让客户端领奖窗显示为空。
			// Retail answers a failed claim (result=1, empty list); a silent return leaves the client reward window blank.
			PacketSendUtility.sendPacket(player, SM_LUNA_SHOP.diceRewardFailure());
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(16, rewardItemId, rewardCount));
		player.setLunaDiceGame(0, true);
		player.setLunaDiceGameTry(0);
		player.setLunaGoldDiceGrade(0);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 72));
	}

	/**
	 * 黄金骰子随机档位：按中国区 luna_dice_gotcha_ratio.xml 的 dice_set_05（5500/4000/500）出 A/S/SS 三档。
	 * Rolls a gold dice grade from the China dice_set_05 weights (5500/4000/500) yielding A/S/SS only.
	 */
	static int rollGoldGrade() {
		int roll = Rnd.get(1, 10000);
		if (roll <= 5500) {
			return 3;
		}
		return roll <= 9500 ? 4 : 5;
	}

	/**
	 * 档位映射到黄金开牌序号 5..7（A/S/SS）。真端 finalOpenNum = 该组 &lt;success_total&gt; + 桶索引，
	 * 中国区 dice_set_05 的 success_total=5、权重 5500/4000/500 → 5/6/7；客户端收到 ≥5 即进黄金分支，
	 * 并以 openNum-5 作为翻牌颗数/宝箱档，special=1 时显示领取按钮。
	 * Maps a grade to gold open number 5..7 (A/S/SS): retail finalOpenNum = success_total + bucket index.
	 * China set dice_set_05 has success_total=5 and weights 5500/4000/500 -> 5/6/7; the client enters the
	 * gold branch at >=5, and uses openNum-5 as the flip count and chest tier.
	 */
	static int goldDiceOpenNum(int grade) {
		return grade + 2;
	}

	/**
	 * 普通掷得奖的档位表：笑猫 1、2 只对应 C，3、4 对应 B，5 对应 A（中国区 gotcha id 10/11/12/13/14/15）。
	 * Reward table for a normal roll: 1-2 smiles = C, 3-4 = B, 5 = A (China gotcha ids 10-15).
	 * @param diceTry 当前开牌序号 / current open number
	 * @return 奖励表 id / reward table id
	 */
	static int rewardTableFor(int diceTry) {
		if (diceTry <= 2) {
			return 1;
		}
		return diceTry <= 4 ? 2 : 3;
	}

	private static LunaDiceItem rollDiceReward(int grade) {
		List<LunaDiceItem> items = DataManager.LUNA_DICE.getDiceItems(grade);
		if (items == null || items.isEmpty()) {
			log.warn("Luna gold dice table {} missing or empty.", grade);
			return null;
		}
		return items.get(Rnd.get(0, items.size() - 1));
	}

	static boolean canClaimDiceReward(int diceTry) {
		return diceTry > 0;
	}

	static boolean canSpendLuna(long balance, long price) {
		return price >= 0 && balance >= price;
	}

	/**
	 * 发放所有已达成但尚未领取的露娜消耗档位奖励（钥匙 + 物品），以 lunaConsumeCount 为已领基线，
	 * 因此旧版精确匹配漏发的档位会在下一次消耗时自动补发。
	 * Grants every reached-but-unclaimed Luna consume-tier reward (keys + item) using lunaConsumeCount as the
	 * claimed baseline, so tiers missed by the legacy exact-match check heal on the next spend.
	 */
	private void grantLunaConsumeRewards(Player player) {
		for (LunaConsumeRewardsTemplate row : crossedTiers(
				DataManager.LUNA_CONSUME_REWARDS_DATA.getAll().values(), player.getLunaConsumePoint(),
				player.getLunaConsumeCount())) {
			player.setLunaConsumeCount(row.getId());
			if (row.getGachaCost() > 0) {
				muniKeysController(player, player.getMuniKeys() + row.getGachaCost());
			}
			if (row.getCreateItemId() > 0) {
				long remaining = ItemService.addItem(player, row.getCreateItemId(), row.getCreateItemCount());
				if (remaining > 0) {
					// 放入失败（背包满等）必须留痕，静默丢失会让“成就达成却没收到”无从排查。
					// A failed delivery (full inventory etc.) must be logged or "tier reached but nothing arrived" is undebuggable.
					log.warn(I18n.get("log.c3f1e2a4d7b8", player.getObjectId(), row.getId(), row.getCreateItemId(),
							remaining));
				}
				ItemTemplate t = DataManager.ITEM_DATA.getItemTemplate(row.getCreateItemId());
				if (t != null) {
					if (row.getCreateItemCount() == 1) {
						PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LUNA_REWARD_GOTCHA_ITEM(t.getNameId()));
					} else if (row.getCreateItemCount() > 1) {
						PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LUNA_REWARD_GOTCHA_ITEM_MULTI(row.getCreateItemCount(), t.getNameId()));
					}
				}
				log.debug("Luna consume tier granted. playerId={} tier={} point={} keys+={} item={}x{}", player.getObjectId(),
						row.getId(), player.getLunaConsumePoint(), row.getGachaCost(), row.getCreateItemId(),
						row.getCreateItemCount());
				// 面板“领取”状态靠这条推送点亮（真端 _SendLunaRewardGotchaPacket 同包形：subAction 12 + 物品对）。
				// The panel lights the tier up from this push (retail _SendLunaRewardGotchaPacket: subAction 12 + item pairs).
				HashMap<Integer, Long> granted = new HashMap<>();
				granted.put(row.getCreateItemId(), (long) row.getCreateItemCount());
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(granted));
			}
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
	}

	/**
	 * 检查玩家是否需要重置每日露娜成就（以每天 09:00 为重置分界线）。
	 * Checks if the player needs a daily reset (boundary at 09:00 every day).
	 */
	static boolean needsDailyReset(java.sql.Timestamp lastOnline) {
		if (lastOnline == null) {
			return false;
		}
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.set(java.util.Calendar.HOUR_OF_DAY, 9);
		cal.set(java.util.Calendar.MINUTE, 0);
		cal.set(java.util.Calendar.SECOND, 0);
		cal.set(java.util.Calendar.MILLISECOND, 0);
		long resetTime = cal.getTimeInMillis();
		if (System.currentTimeMillis() < resetTime) {
			resetTime -= 86400000L;
		}
		return lastOnline.getTime() < resetTime;
	}

	/**
	 * 区间判档：档位 ≤ 累计消耗且 ID 大于已领基线的行，按 ID 升序（表内档位随 ID 单调递增）。
	 * 若已领基线大于当前点数能达到的最大档位（历史脏数据，如 150 点却记录为 7），自动纠偏为 0 从头补发。
	 * Range-based tier selection: rows with threshold ≤ consumed point and id above the claimed baseline, by id.
	 * Heals legacy dirty state (e.g. claimedId=7 on consumePoint=150) by falling back to 0.
	 */
	static List<LunaConsumeRewardsTemplate> crossedTiers(Collection<LunaConsumeRewardsTemplate> rows, int consumePoint,
			int claimedId) {
		int maxAchievableId = rows.stream()
				.filter(row -> row.getSumCount() <= consumePoint)
				.mapToInt(LunaConsumeRewardsTemplate::getId)
				.max()
				.orElse(0);
		int effectiveClaimedId = claimedId > maxAchievableId ? 0 : claimedId;
		return rows.stream()
				.filter(row -> row.getSumCount() <= consumePoint && row.getId() > effectiveClaimedId)
				.sorted(Comparator.comparingInt(LunaConsumeRewardsTemplate::getId))
				.toList();
	}

	private boolean spendLunaFailed(Player player, long price) {
		synchronized (player.getClientConnection().getAccount()) {
			long balance = player.getLunaAccount();
			if (!canSpendLuna(balance, price)) {
				if (price < 0) {
					log.warn(I18n.get("log.962d3a2eb9ed", player.getObjectId(), price));
				} else {
					PacketSendUtility.sendMessage(player, "Not enough Luna.");
					PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0));
				}
				return true;
			}
			if (price == 0) {
				return false;
			}
			long remaining = balance - price;
			player.setLunaAccount(remaining);
			// 扣款未生效（LS 通信失败等）同样算失败，防止白给奖励。
			// An unapplied deduction (e.g. LS communication failure) also counts as failure.
			if (player.getLunaAccount() != remaining) {
				return true;
			}
			// 所有露娜消耗都计入档位成就（真端在 User::TryChangeLuna 唯一咽喉后自动判档发奖）。
			// All Luna spending counts toward the consume tiers (retail auto-grants right after User::TryChangeLuna).
			player.setLunaConsumePoint(player.getLunaConsumePoint() + (int) price);
			grantLunaConsumeRewards(player);
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0));
		return false;
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static LunaShopService getInstance() {
		LunaShopService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<LunaShopService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("LunaShopService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<LunaShopService> instanceProvider) {
		LunaShopService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

}
