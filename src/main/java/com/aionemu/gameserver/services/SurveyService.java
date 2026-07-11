package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.dao.SurveyControllerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.survey.SurveyItem;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import lombok.extern.slf4j.Slf4j;

/**
 * 问卷/调查奖励服务，轮询待领调查并向玩家发放奖励。
 * Survey reward service polling pending surveys and granting player rewards.
 *
 * @author KID
 */
@Slf4j
public class SurveyService {

	private static volatile ObjectProvider<SurveyService> instanceProvider;
	private ConcurrentMap<Integer, SurveyItem> activeItems;
	private volatile String htmlTemplate;
	private Future<?> updateTask;

	/**
	 * 判断调查是否有效；若有效则触发领取流程。
	 * Checks whether the survey is active; if so, requests the reward flow.
	 *
	 * 玩家 / player
	 * survey id
	 * whether active
	 */
	public boolean isActive(Player player, int survId) {
		boolean avail = this.activeItems.containsKey(survId);
		if (avail) {
			this.requestSurvey(player, survId);
		}
		return avail;
	}

	/**
	 * 构造服务并启动周期刷新任务。
	 * Constructs the service and starts the periodic update task.
	 */
	public SurveyService() {
		activeItems = new ConcurrentHashMap<Integer, SurveyItem>();
		reload();
	}

	/**
	 * 重载 HTML 模板与刷新间隔。
	 * Reloads the HTML template and refresh interval.
	 */
	public synchronized void reload() {
		htmlTemplate = GameStaticDataServices.htmlCache().getHTML("surveyTemplate.xhtml");
		if (updateTask != null) {
			updateTask.cancel(false);
		}
		updateTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new TaskUpdate(), 2000,
				SecurityConfig.SURVEY_DELAY * 60000);
	}

	/**
	 * 处理玩家领取调查奖励。
	 * Handles a player claiming a survey reward.
	 *
	 * 玩家 / player
	 * survey id
	 */
	public void requestSurvey(Player player, int survId) {

		SurveyItem item = activeItems.get(survId);
		if (item == null) {
			// 当前没有进行中的调查。 / There is no survey underway.
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300684));
			return;
		}

		if (item.ownerId != player.getObjectId()) {
			// 没有可参与的调查。 / There is no remaining survey to take part in.
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300037));
			return;
		}
		ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(item.itemId);
		if (template == null) {
			return;
		}
		if (player.getInventory().isFull(template.getExtraInventoryId())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY);
			log.warn(I18n.get("log.b817769a6b45", player.getName()));
			return;
		}
		if (DAOManager.getDAO(SurveyControllerDAO.class).useItem(item.uniqueId)) {

			ItemService.addItem(player, item.itemId, item.count);
			if (item.itemId == ItemId.KINAH.value()) { // You received %num0 Kinah as reward for the survey.
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300945, item.count));
			} else if (item.count == 1) { // You received %0 item as reward for the survey.
				PacketSendUtility.sendPacket(player,
						new SM_SYSTEM_MESSAGE(1300945, new DescriptionId(template.getNameId())));
			} else {
				// 你因调查获得了 %num1 个 %0 作为奖励。 / You received %num1 %0 items as reward for the survey.
				PacketSendUtility.sendPacket(player,
						new SM_SYSTEM_MESSAGE(1300946, item.count, new DescriptionId(template.getNameId())));
			}
			template = null;
			activeItems.remove(survId);
		}
	}

	/**
	 * 从数据库拉取新调查并通知在线玩家。
	 * Loads new surveys from DB and notifies online owners.
	 */
	public void taskUpdate() {
		List<SurveyItem> newList = DAOManager.getDAO(SurveyControllerDAO.class).getAllNew();
		if (newList.size() == 0) {
			return;
		}
		List<Integer> players = new ArrayList<Integer>();
		int cnt = 0;
		for (SurveyItem item : newList) {
			activeItems.put(item.uniqueId, item);
			cnt++;
			if (!players.contains(item.ownerId)) {
				players.add(item.ownerId);
			}
		}
		log.info(I18n.get("log.febfef8995af", cnt, players.size()));
		for (int ownerId : players) {
			Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(ownerId);
			if (player != null) {
				showAvailable(player);
			}
		}
	}

	/**
	 * 向玩家展示其可用的调查 HTML。
	 * Shows available survey HTML pages to the player.
	 *
	 * @param player 玩家 / player
	 */
	public void showAvailable(Player player) {
		for (SurveyItem item : this.activeItems.values()) {
			if (item.ownerId != player.getObjectId()) {
				continue;
			}
			String context = htmlTemplate;
			context = context.replace("%itemid%", item.itemId + "");
			context = context.replace("%itemcount%", item.count + "");
			context = context.replace("%html%", item.html);
			context = context.replace("%radio%", item.radio);

			HTMLService.sendData(player, item.uniqueId, context);
		}
	}

	public class TaskUpdate implements Runnable {

		@Override
		public void run() {
			log.info(I18n.get("log.644783da9bbc"));
			taskUpdate();
		}
	}

	private static class SingletonHolder {

		protected static final SurveyService instance = new SurveyService();
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static final SurveyService getInstance() {
		ObjectProvider<SurveyService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<SurveyService> provider) {
		instanceProvider = provider;
	}
}
