
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

@Slf4j
public class WebshopService {
	private static final String SHOP_MAIL_SENDER = "Aion Shop";
	private static final String SHOP_MAIL_TITLE = "Shop Purchase";
	private static final String SHOP_MAIL_MESSAGE = "Your shop purchase has arrived.";
	private static volatile ObjectProvider<WebshopService> instanceProvider;

	public WebshopService() {
		this.load();
	}

	public static final WebshopService getInstance() {
		ObjectProvider<WebshopService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	public static void setInstanceProvider(ObjectProvider<WebshopService> provider) {
		instanceProvider = provider;
	}

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
