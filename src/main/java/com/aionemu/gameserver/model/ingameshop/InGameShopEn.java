package com.aionemu.gameserver.model.ingameshop;


import com.aionemu.boot.i18n.I18n;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.ingameshop.InGameShopProperty;
import com.aionemu.gameserver.configs.main.InGameShopConfig;
import com.aionemu.gameserver.dao.InGameShopDAO;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TOLL_INFO;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_PREMIUM_CONTROL;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.NonNull;

/**
 * 游戏内商城引擎：加载商品、处理购买请求并与登录服点数同步。
 * In-game shop engine: loads items, handles purchase requests and syncs toll with the login server.
 *
 * @author KID
 */
@Slf4j(topic = "INGAMESHOP_LOG")
public class InGameShopEn {

	/**
	 * -- SETTER --
	 * 设置实例提供者。 / Sets the instance provider.
	 */
	@Setter
	private static volatile ObjectProvider<InGameShopEn> instanceProvider;
	private volatile Map<Byte, List<IGItem>> items = Collections.emptyMap();
	private final InGameShopDAO dao;
	private volatile InGameShopProperty iGProperty;
	private int lastRequestId = 0;
	private final List<IGRequest> activeRequests = new ArrayList<>();
	private static final Map<Integer, Long> lastUsage = new HashMap<>();

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：旧实现在没有 provider 时回退到 {@code SingletonHolder.instance}，
	 * 等于在容器之外静默创建第二个实例（两套状态）。现在改为 fail-fast，
	 * 把"未注入"暴露在启动期，而不是潜伏成两套实例。
	 * The legacy static fallback is retired: without a provider the old code silently created a second instance
	 * through SingletonHolder, i.e. two live states. It now fails fast so a missing injection surfaces at startup.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static InGameShopEn getInstance() {
		ObjectProvider<InGameShopEn> provider = instanceProvider;
		InGameShopEn provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("InGameShopEn 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 使用指定的游戏内商城 DAO 构造引擎。
	 * Constructs the engine with the given in-game-shop DAO.
	 *
	 * @param dao 游戏内商城 DAO / in-game-shop DAO
	 */
	public InGameShopEn(@NonNull InGameShopDAO dao) {
		this.dao = dao;
		reload();
	}

	/**
	 * 构造引擎：DAOManager 已初始化时优先解析 Spring Bean，否则退回实现类无参构造。
	 * Constructs the engine: resolves the Spring bean when DAOManager is initialized, else falls back to the no-arg impl constructor.
	 */
	public InGameShopEn() {
		this(safeDao());
	}

	/**
	 * DAO 解析兜底：容器未就绪（离线/单测）时退回实现类无参构造。
	 * DAO resolution fallback: falls back to the no-arg impl constructor when the container is not ready (offline/unit tests).
	 */
	private static InGameShopDAO safeDao() {
		if (DAOManager.isInitialized()) {
			try {
				return DAOManager.getDAO(InGameShopDAO.class);
			} catch (Exception e) {
				log.debug("InGameShopDAO bean resolution failed, falling back to the no-arg impl constructor", e);
			}
		}
		return new com.aionemu.gameserver.dao.impl.InGameShopDAO();
	}

	/**
	 * 返回游戏内商城 DAO，供命令域复用同一绑定（单一所有权）。
	 * Returns the in-game-shop DAO so command-domain callers reuse the same binding (single ownership).
	 *
	 * @return 游戏内商城 DAO / in-game-shop DAO
	 */
	public InGameShopDAO inGameShopDAO() {
		return dao;
	}

	/**
	 * 重新加载。 / Reload.
	 */
	public synchronized void reload() {
		if (!InGameShopConfig.ENABLE_IN_GAME_SHOP) {
			log.info(I18n.get("log.835552c18c7b"));
			return;
		}
		InGameShopProperty reloadedProperty = InGameShopProperty.load();
		Map<Byte, List<IGItem>> reloadedItems = dao.loadInGameShopItems();
		iGProperty = reloadedProperty;
		items = reloadedItems;
		log.info(I18n.get("log.1e48e4bddea9", items.size()));
	}

	/**
	 * 获取商城物品。 / Returns the ig item.
	 */
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

	/**
	 * 获取物品。 / Returns the items.
	 */
	public Collection<IGItem> getItems(byte category) {
		if (!items.containsKey(category)) {
			return Collections.emptyList();
		}
		return items.get(category);
	}

	/**
	 * 返回 top sales / Returns the top sales
	 */
	public List<Integer> getTopSales(int subCategory, byte category) {
		byte max = 6;
		TreeMap<Integer, Integer> map = new TreeMap<>(Comparator.reverseOrder());
		if (!items.containsKey(category)) {
			return new ArrayList<>();
		}
		for (IGItem item : items.get(category))
			if (item.getSalesRanking() != 0 && (subCategory == 2 || item.getSubCategory() == subCategory)) {
				map.put(item.getSalesRanking(), item.getObjectId());
			}
		List<Integer> top = new ArrayList<>();
		byte cnt = 0;
		for (int objId : map.values()) {
			if (cnt > max) {
				break;
			}
			top.add(objId);
			cnt++;
		}
		return top;
	}

	/**
	 * 返回最大列表 / Returns the max list
	 */
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

	/**
	 * 接受请求 / Accept Request
	 */
	public void acceptRequest(Player player, int itemObjId) {
		if (player.getInventory().isFull()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DICE_INVEN_ERROR);
			return;
		}

		IGItem item = getInstance().getIGItem(itemObjId);
		if (InGameShopConfig.GAMESHOP_LIMIT) {
			if (item.getCategory() == InGameShopConfig.GAMESHOP_CATEGORY) {
				if (lastUsage.containsKey(player.getObjectId())) {
					if ((System.currentTimeMillis() - lastUsage.get(player.getObjectId())) < InGameShopConfig.GAMESHOP_LIMIT_TIME * 60 * 1000) {
						PacketSendUtility.sendMessage(player, "?????????????,??????????:" + (int) ((InGameShopConfig.GAMESHOP_LIMIT_TIME * 60 * 1000 - (System.currentTimeMillis() - lastUsage.get(player.getObjectId()))) / 1000) + " ?");
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
		if (InGameShopConfig.GAMESHOP_LIMIT) {
			if (item.getCategory() == InGameShopConfig.GAMESHOP_CATEGORY) {
				lastUsage.put(player.getObjectId(), System.currentTimeMillis());
			}
		}
	}

	/**
	 * 添加点数。 / Adds toll.
	 */
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

	/**
	 * Finish Request / Finish Request
	 */
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

}
