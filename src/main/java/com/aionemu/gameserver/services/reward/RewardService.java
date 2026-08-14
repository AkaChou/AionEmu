package com.aionemu.gameserver.services.reward;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.RewardServiceDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;
import com.aionemu.gameserver.services.mail.SystemMailService;

import java.util.ArrayList;
import java.util.List;

/**
 * 外部/运营奖励发放服务，校验待领奖励并通过系统邮件下发。
 * External/operator reward delivery service verifying pending rewards and mailing them via system mail.
 */
@Slf4j
public class RewardService {
	private RewardServiceDAO dao;
	private static volatile ObjectProvider<RewardService> instanceProvider;

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则回退本地单例）。
	 * Get the service singleton (prefer Spring ObjectProvider, otherwise local holder).
	 *
	 * @return 服务实例 / Service instance
	 */
	public static RewardService getInstance() {
		ObjectProvider<RewardService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<RewardService> instanceProvider) {
		RewardService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final RewardService instance = new RewardService();
	}

	/**
	 * 初始化 DAO。
	 * Initialize the DAO.
	 */
	public RewardService() {
		dao = DAOManager.getDAO(RewardServiceDAO.class);
	}

	/**
	 * 校验并下发玩家待领的运营奖励（黑云邮件）。
	 * Verify and deliver the player's pending operator rewards via Black Cloud mail.
	 *
	 * @param player 玩家 / Player
	 */
	public void verify(Player player) {
		List<RewardEntryItem> list = dao.getAvailable(player.getObjectId());
		if (list.size() == 0 || player.getMailbox() == null) {
			return;
		}
		List<Integer> rewarded = new ArrayList<Integer>();
		for (RewardEntryItem item : list) {
			if (DataManager.ITEM_DATA.getItemTemplate(item.id) == null) {
				log.warn(I18n.get("log.b873f28ac3e2", item.unique, item.id, player.getObjectId()));
				continue;
			}
			try {
				if (!GameFeatureServices.systemMailService().sendMail("$$CASH_ITEM_MAIL", player.getName(),
						item.id + ", " + item.count, "0, " + (System.currentTimeMillis() / 1000) + ",", item.id,
						(int) item.count, 0, 0, LetterType.BLACKCLOUD)) {
					continue;
				}
				log.info(I18n.get("log.3944ea5d4631", item.unique, player.getName(), item.count, item.id));
				rewarded.add(item.unique);
			} catch (Exception e) {
				log.error(I18n.get("log.b52e75494d2a", item.unique, item.count, item.id, player.getObjectId(), e));
				continue;
			}
		}
		if (rewarded.size() > 0) {
			dao.uncheckAvailable(rewarded);
			list.clear();
		}
	}
}
