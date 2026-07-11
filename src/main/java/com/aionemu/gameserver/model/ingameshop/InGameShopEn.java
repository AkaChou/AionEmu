package com.aionemu.gameserver.model.ingameshop;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.ingameshop.InGameShopProperty;
import com.aionemu.gameserver.configs.main.AdvCustomConfig;
import com.aionemu.gameserver.configs.main.InGameShopConfig;
import com.aionemu.gameserver.dao.InGameShopDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.templates.mail.MailMessage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TOLL_INFO;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_PREMIUM_CONTROL;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 游戏内商城引擎：加载商品、处理购买请求并与登录服点数同步。
 * In-game shop engine: loads items, handles purchase requests and syncs toll with the login server.
 *
 * @author KID
 */
@Slf4j(topic = "INGAMESHOP_LOG")
public class InGameShopEn {

	private static volatile ObjectProvider<InGameShopEn> instanceProvider;
	private volatile Map<Byte, List<IGItem>> items = Collections.emptyMap();
	private InGameShopDAO dao;
	private volatile InGameShopProperty iGProperty;
	private int lastRequestId = 0;
	private final List<IGRequest> activeRequests = new ArrayList<>();
	private static Map<Integer, Long> lastUsage = new HashMap<>();

	/** 获取副本。 / Returns the instance. */
	public static InGameShopEn getInstance() {
		ObjectProvider<InGameShopEn> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/** 设置实例提供者。 / Sets the instance provider. */
	public static void setInstanceProvider(ObjectProvider<InGameShopEn> provider) {
		instanceProvider = provider;
	}

	public InGameShopEn() {
		reload();
	}

	/** 获取商城属性配置。 / Returns the igs property. */
	public InGameShopProperty getIGSProperty() {
		return iGProperty;
	}

	/** 重新加载。 / Reload. */
	public synchronized void reload() {
		if (!InGameShopConfig.ENABLE_IN_GAME_SHOP) {
			log.info(I18n.get("log.835552c18c7b"));
			return;
		}
		InGameShopProperty reloadedProperty = InGameShopProperty.load();
		InGameShopDAO reloadedDao = DAOManager.getDAO(InGameShopDAO.class);
		Map<Byte, List<IGItem>> reloadedItems = reloadedDao.loadInGameShopItems();
		dao = reloadedDao;
		iGProperty = reloadedProperty;
		items = reloadedItems;
		log.info(I18n.get("log.1e48e4bddea9", items.size()));
	}

	/** 获取商城物品。 / Returns the ig item. */
	public IGItem getIGItem(int id) {
		for (byte key : items.keySet()) {
			for (IGItem item : items.get(key)) {
				if (item.getObjectId() == id) {
					return item;
				}
			}
		}
		return null;
	}

	/** 获取物品。 / Returns the items. */
	public Collection<IGItem> getItems(byte category) {
		if (!items.containsKey(category)) {
			return Collections.emptyList();
		}
		return items.get(category);
	}

	/** 返回 top sales / Returns the top sales */
	public List<Integer> getTopSales(int subCategory, byte category) {
		byte max = 6;
		TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>(new DescFilter());
		if (!items.containsKey(category)) {
			return new ArrayList<>();
		}
		for (IGItem item : items.get(category))
			if (item.getSalesRanking() != 0 && (subCategory == 2 || item.getSubCategory() == subCategory)) {
				map.put(item.getSalesRanking(), item.getObjectId());
			}
		List<Integer> top = new ArrayList<>();
		byte cnt = 0;
		for (Iterator<Integer> i = map.values().iterator(); i.hasNext();) {
			int objId = i.next();
			if (cnt > max) {
				break;
			}
			top.add(objId);
			cnt++;
		}

		map.clear();
		return top;
	}

	/** 返回最大列表 / Returns the max list*/
	public int getMaxList(byte subCategoryId, byte category) {
		int id = 0;
		if (!items.containsKey(category)) {
			return id;
		}
		for (IGItem item : items.get(category)) {
			if (item.getSubCategory() == subCategoryId) {
				if (item.getList() > id) {
					id = item.getList();
				}
			}
		}

		return id;
	}

	/** 接受请求 / Accept Request */
	public void acceptRequest(Player player, int itemObjId) {
		if (player.getInventory().isFull()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DICE_INVEN_ERROR);
			return;
		}

		IGItem item = getInstance().getIGItem(itemObjId);
		if (AdvCustomConfig.GAMESHOP_LIMIT) {
			if (item.getCategory() == AdvCustomConfig.GAMESHOP_CATEGORY) {
				if (lastUsage.containsKey(player.getObjectId())) {
					if ((System.currentTimeMillis() - lastUsage.get(player.getObjectId())) < AdvCustomConfig.GAMESHOP_LIMIT_TIME * 60 * 1000) {
						PacketSendUtility.sendMessage(player, "?????????????,??????????:" + (int) ((AdvCustomConfig.GAMESHOP_LIMIT_TIME * 60 * 1000 - (System.currentTimeMillis() - lastUsage.get(player.getObjectId()))) / 1000) + " ?");
						return;
					}
				}
			}
		}
		lastRequestId++;
		IGRequest request = new IGRequest(lastRequestId, player.getObjectId(), itemObjId);
		request.accountId = player.getClientConnection().getAccount().getId();
		if (com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PREMIUM_CONTROL(request, item.getItemPrice())))
			activeRequests.add(request);
		if (AdvCustomConfig.GAMESHOP_LIMIT) {
			if (item.getCategory() == AdvCustomConfig.GAMESHOP_CATEGORY) {
				lastUsage.put(player.getObjectId(), System.currentTimeMillis());
			}
		}
	}

	/** 发送请求。 / Send request. */
	public void sendRequest(Player player, String receiver, String message, int itemObjId) {
		if (receiver.equalsIgnoreCase(player.getName())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INGAMESHOP_CANNOT_GIVE_TO_ME);
			return;
		}

		if (!InGameShopConfig.ALLOW_GIFTS) {
			PacketSendUtility.sendMessage(player, "Gifts are disabled.");
			return;
		}

		if (!DAOManager.getDAO(PlayerDAO.class).isNameUsed(receiver)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INGAMESHOP_NO_USER_TO_GIFT);
			return;
		}

		PlayerCommonData recipientCommonData = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonDataByName(receiver);
		if (recipientCommonData.getMailboxLetters() >= 100) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MAIL_MSG_RECIPIENT_MAILBOX_FULL(recipientCommonData.getName()));
			return;
		}

		if (!InGameShopConfig.ENABLE_GIFT_OTHER_RACE && !player.isGM()) {
			if (player.getRace() != recipientCommonData.getRace()) {
				PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(MailMessage.MAIL_IS_ONE_RACE_ONLY));
				return;
			}
		}
		IGItem item = getIGItem(itemObjId);
		lastRequestId++;
		IGRequest request = new IGRequest(lastRequestId, player.getObjectId(), receiver, message, itemObjId);
		request.accountId = player.getClientConnection().getAccount().getId();
		if (com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PREMIUM_CONTROL(request, item.getItemPrice()))) {
			activeRequests.add(request);
		}
	}

	/** 添加点数。 / Adds toll. */
	public void addToll(Player player, long cnt) {
		if (InGameShopConfig.ENABLE_IN_GAME_SHOP) {
			lastRequestId++;
			IGRequest request = new IGRequest(lastRequestId, player.getObjectId(), 0);
			request.accountId = player.getClientConnection().getAccount().getId();
			if (com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PREMIUM_CONTROL(request, cnt * -1))) {
				activeRequests.add(request);
			}
		} else {
			PacketSendUtility.sendMessage(player, "You can't add toll if ingameshop is disabled!");
		}
	}

	/** Finish Request / Finish Request */
	public void finishRequest(int requestId, int result, long toll, long luna) {
		IGRequest foundRequest = null;
		
		for (IGRequest request : activeRequests) {
			if (request.requestId == requestId) {
				foundRequest = request;
				break;
			}
		}
		
		if (foundRequest == null) {
			return;
		}
		
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(foundRequest.playerId);
		if (player != null) {
			if (result == 1) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INGAMESHOP_ERROR);
			} else if (result == 2) {
				IGItem item = getIGItem(foundRequest.itemObjId);
				if (item == null) {
					return;
				}
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_INGAMESHOP_NOT_ENOUGH_CASH("Toll"));
				PacketSendUtility.sendPacket(player, new SM_TOLL_INFO(toll));
			} else if (result == 3) {
				IGItem item = getIGItem(foundRequest.itemObjId);
				if (item == null) {
					return;
				}
				
				if (foundRequest.gift) {
					GameFeatureServices.systemMailService().sendMail(player.getName(), foundRequest.receiver, "In Game Shop", foundRequest.message, item.getItemId(), item.getItemCount(), 0L, 0L, LetterType.BLACKCLOUD);
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INGAMESHOP_GIFT_SUCCESS);
					player.getClientConnection().getAccount().setToll(toll);
					player.getClientConnection().getAccount().setLuna(luna);
				} else {
					ItemService.addItem(player, item.getItemId(), item.getItemCount());
					player.getClientConnection().getAccount().setToll(toll);
					player.getClientConnection().getAccount().setLuna(luna);
				}
				
				item.increaseSales();
				dao.increaseSales(item.getObjectId(), item.getSalesRanking());
				PacketSendUtility.sendPacket(player, new SM_TOLL_INFO(toll));
			} else if (result == 4) {
				player.getClientConnection().getAccount().setToll(toll);
				PacketSendUtility.sendPacket(player, new SM_TOLL_INFO(toll));
			}
		}
		
		activeRequests.remove(foundRequest);
	}

	class DescFilter implements Comparator<Object> {
		DescFilter() {
		}
		/** 比较 / compare. */
		public int compare(Object o1, Object o2) {
			Integer i1 = (Integer) o1;
			Integer i2 = (Integer) o2;
			return -i1.compareTo(i2);
		}
	}

	private static final class SingletonHolder {
		private static final InGameShopEn instance = new InGameShopEn();
	}
}
