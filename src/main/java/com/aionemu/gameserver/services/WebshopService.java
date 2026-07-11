package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.RewardServiceDAO;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;

/**
 * 网页商城服务，定时扫描并发放待领取的商城奖励邮件。
 * Web shop service that periodically scans and delivers pending shop reward mails.
 */
@Slf4j
public class WebshopService {
	private static final String SHOP_MAIL_SENDER = "Aion Shop";
	private static final String SHOP_MAIL_TITLE = "Shop Purchase";
	private static final String SHOP_MAIL_MESSAGE = "Your shop purchase has arrived.";
	private static volatile ObjectProvider<WebshopService> instanceProvider;

	/**
	 * 构造服务并启动定时发放任务。
	 * Constructs the service and starts the periodic delivery task.
	 */
	public WebshopService() {
		this.load();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 */
	public static final WebshopService getInstance() {
		ObjectProvider<WebshopService> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<WebshopService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 启动定时任务，为在线玩家发放待领取商城奖励。
	 * Starts the periodic task that delivers pending shop rewards to online players.
	 */
	private void load() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						RewardServiceDAO rewardDao = DAOManager.getDAO(RewardServiceDAO.class);
						List<RewardEntryItem> liste = rewardDao.getAvailable(pl.getObjectId());
						if (liste.isEmpty()) {
							return;
						} else {
							for (RewardEntryItem item : liste) {
								deliverRewardMail(pl.getName(), item, rewardDao, GameFeatureServices.systemMailService());
							}
						}
					}
				});
			}
		}, 5 * 1000, 5 * 1000);
	}

	/**
	 * 将单条奖励以系统邮件发放给玩家；失败时回滚标记。
	 * Delivers one reward as a system mail; rolls back the claim flag on failure.
	 *
	 * @param recipientName 收件人角色名 / recipient character name
	 * @param item 奖励条目 / reward entry item
	 * reward DAO
	 * @param mailService 系统邮件服务 / system mail service
	 * @return 发放成功返回 true / true if delivered
	 */
	static boolean deliverRewardMail(String recipientName, RewardEntryItem item, RewardServiceDAO rewardDao,
			SystemMailService mailService) {
		if (!rewardDao.setUpdate(item.unique)) {
			return false;
		}
		boolean delivered = mailService.sendMail(SHOP_MAIL_SENDER, recipientName, SHOP_MAIL_TITLE, SHOP_MAIL_MESSAGE,
				item.id, item.count, 0, 0, LetterType.EXPRESS);
		if (!delivered) {
			rewardDao.setUpdateDown(item.unique);
			return false;
		}
		return true;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final WebshopService instance = new WebshopService();
	}
}
