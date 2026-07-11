package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.sql.Timestamp;
import java.util.concurrent.Future;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.dao.PlayerThievesListDAO;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CAPTCHA;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.events.thievesguildservice.ThievesStatusList;
import com.aionemu.gameserver.services.events.thievesguildservice.ThievesType;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.captcha.CAPTCHAUtil;

/**
 * 盗贼公会服务，处理盗贼任务、复仇与验证码校验。
 * Thieves guild service handling thief quests, revenge and captcha checks.
 */

@Slf4j
public class ThievesGuildService {

	private static volatile ObjectProvider<ThievesGuildService> instanceProvider;

	/**
	 * 玩家进入世界时处理。
	 * Handles player entering the world.
	 *
	 * @param player 玩家 / player
	 */
	public void onEnterWorld(Player player) {
		if (!CustomConfig.THIEVES_ENABLE) {
			return;
		}
		try {
			ThievesStatusList thieves = DAOManager.getDAO(PlayerThievesListDAO.class).loadThieves(player.getObjectId());
			if (thieves == null) {
				thieves = new ThievesStatusList(player.getObjectId(), 0, 0, 0L, 0, "Нет", 0,
						new Timestamp(System.currentTimeMillis()));
				DAOManager.getDAO(PlayerThievesListDAO.class).saveNewThieves(thieves);
			}
			player.setThieves(thieves);
			log.info(I18n.get("log.b578983bb7dd", player.getThieves().getPlayerId()));
		} catch (Exception ex) {
			log.error(I18n.get("log.c1bcc321b6d5", player.getName(), "]", ex));
		}
	}

	/**
	 * 处理盗贼逻辑。
	 * Handles thieves logic.
	 *
	 * @param player 玩家 / player
	 */
	public void thieves(Player player) {
		if (!CustomConfig.THIEVES_ENABLE)
			return;

		for (Player target : player.getKnownList().getKnownPlayers().values()) {
			if (!PlayerActions.isAlreadyDead(target) && MathUtil.isIn3dRange(target, player, 2)) {
				if (!player.isThieves()) {
					player.setIsThieves(true);
				}
				player.setCaptchaWord(CAPTCHAUtil.getRandomWord());
				player.setCaptchaImage(CAPTCHAUtil.createCAPTCHA(player.getCaptchaWord()).array());
				captchaCheck(player, target, 0, true, SecurityConfig.CAPTCHA_EXTRACTION_BAN_TIME * 1000L);
			}
		}
	}

	/**
	 * 创建复仇。
	 * Creates revenge.
	 *
	 * 玩家 / player
	 * target
	 */
	public void createRevenge(Player player, Player target) {
		if (!CustomConfig.THIEVES_ENABLE) {
			return;
		}
		if (player.isThievesDuel()) {
			return;
		}
		target.setThieves(DAOManager.getDAO(PlayerThievesListDAO.class).loadThieves(target.getObjectId()));
		ThievesStatusList thieves = target.getThieves();
		if (thieves.getRevengeName().equals(player.getName()) && !PlayerActions.isAlreadyDead(target)
				&& MathUtil.isIn3dRange(target, player, 2)) {
			player.setThievesDuel(true);
			thievesMessage(player, "Thief " + target.getName() + " in the reach zone. The duel begins.", 0);
			thievesMessage(target, "Sacrifice " + player.getName() + " in the zone of revenge. The duel begins.", 0);
			// GameGameplayServices.duelService().startDuel(player, target);
		}
		log.info(I18n.get("log.e833f1816c82", player.getName()));
	}

	/**
	 * 执行复仇。
	 * Executes revenge.
	 *
	 * 玩家 / player
	 * target
	 */
	public void revenge(Player player, Player target) {
		if (!CustomConfig.THIEVES_ENABLE) {
			return;
		}
		player.setThieves(DAOManager.getDAO(PlayerThievesListDAO.class).loadThieves(player.getObjectId()));
		target.setThieves(DAOManager.getDAO(PlayerThievesListDAO.class).loadThieves(target.getObjectId()));
		ThievesStatusList thievesPlayer = player.getThieves();
		ThievesStatusList thievesTarget = target.getThieves();
		if (thievesPlayer == null || thievesTarget == null) {
			return;
		}
		Timestamp nextTime = thievesTarget.getRevengeDate();
		Timestamp currentTime = new Timestamp(System.currentTimeMillis());
		int revengeCount = thievesPlayer.getRevengeCount();
		long kinahResult = (target.getInventory().getKinah() / thievesPlayer.getRankId())
				+ thievesTarget.getLastThievesKinah();
		if (!player.getName().equals(thievesTarget.getRevengeName()) && currentTime.after(nextTime)
				&& !currentTime.equals(nextTime)) {
			return;
		} else {
			player.getInventory().increaseKinah(kinahResult);
			target.getInventory().decreaseKinah(kinahResult);
			thievesPlayer.setRevengeCount(revengeCount + 1);
			thievesMessage(player, "Вы наказали вора " + target.getName() + " и вернули " + kinahResult + " кинар.", 0);
		}
		thievesTarget.setLastThievesKinah(0l);
		thievesTarget.setRevengeName("Нет");
		thievesTarget.setRevengeDate(new Timestamp(System.currentTimeMillis()));
		DAOManager.getDAO(PlayerThievesListDAO.class).storeThieves(thievesPlayer);
		DAOManager.getDAO(PlayerThievesListDAO.class).storeThieves(thievesTarget);
		log.info(I18n.get("log.917ee8fb0505", player.getName()));
	}
	/**
	 * 验证码校验。
	 * Captcha verification.
	 *
	 * 玩家 / player
	 * @param captchaCount 验证码次数 / captchaCount
	 * state
	 * @param delay 延迟毫秒 / delay
	 */
	public void captchaCheck(Player player, int captchaCount, boolean state, long delay) {
		captchaCheck(player, null, captchaCount, state, delay);
	}

	/**
	 * 验证码校验。
	 * Captcha verification.
	 *
	 * 玩家 / player
	 * target
	 * @param captchaCount 验证码次数 / captchaCount
	 * state
	 * @param delay 延迟毫秒 / delay
	 */
	public void captchaCheck(Player player, Player target, int captchaCount, boolean state, long delay) {
		stopThievesTask(player);

		if (state) {
			if (captchaCount < 3) {
				PacketSendUtility.sendPacket(player, new SM_CAPTCHA(captchaCount + 1, player.getCaptchaImage()));
			} else {
				player.setCaptchaWord(null);
				player.setCaptchaImage(null);
			}
			scheduleThievesTask(player, delay);
			log.info(I18n.get("log.3e0724b37bb6", player.getName()));
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400269));
			player.setCaptchaWord(null);
			player.setCaptchaImage(null);
			player.setIsThieves(false);
			// 盗贼成功 / Thieves success
			player.setThieves(DAOManager.getDAO(PlayerThievesListDAO.class).loadThieves(player.getObjectId()));
			ThievesStatusList thieves = player.getThieves();
			Timestamp nextTime = thieves.getRevengeDate();
			Timestamp currentTime = new Timestamp(System.currentTimeMillis());
			int thievesCount = thieves.getThievesCount();
			if (target != null && target.getName().equals(thieves.getRevengeName()) && currentTime.after(nextTime)
					&& !currentTime.equals(nextTime)) {
				long kinah = 0;
				switch (ThievesType.getThievesType(thieves.getRankId())) {
				case SILVER:
					kinah = 1000 * thieves.getRankId();
					break;
				case GOLD:
					kinah = 2000 * thieves.getRankId();
					break;
				case PLATINUM:
					kinah = 3000 * thieves.getRankId();
					break;
				case MITHRIL:
					kinah = 4000 * thieves.getRankId();
					break;
				case SERAMIUM:
					kinah = 5000 * thieves.getRankId();
					break;
				default:
					kinah = 600;
					break;
				}
				int rank = 0;
				switch (thieves.getThievesCount()) {
				case 10:
					rank = 1;
					thievesMessage(player, "Thief", 1);
					break;
				case 50:
					rank = 2;
					thievesMessage(player, "Pickpocket", 1);
					break;
				case 100:
					rank = 3;
					thievesMessage(player, "Voryaga", 1);
					break;
				case 150:
					rank = 4;
					thievesMessage(player, "Sleek Hands", 1);
					break;
				case 200:
					rank = 5;
					thievesMessage(player, "Fast hands", 1);
					break;
				case 300:
					rank = 6;
					thievesMessage(player, "Elusive", 1);
					break;
				}
				thieves.setRankId(rank);
				thieves.setThievesCount(thievesCount + 1);
				thieves.setRevengeName(target.getName());
				thieves.setLastThievesKinah(kinah);
				player.getInventory().increaseKinah(kinah);
				target.getInventory().decreaseKinah(kinah);
				DAOManager.getDAO(PlayerThievesListDAO.class).storeThieves(thieves);
				thievesMessage(player, "You are robbed " + target.getName(), 0);
				thievesMessage(player, "Be careful! Revenge can be swift from " + target.getName(), 0);
				thievesMessage(player, target.getName() + " Has the ability to attack you at any time ", 0);
				thievesMessage(player, "if " + target.getName()
						+ " you will be killed. He will get stolen from% and you will lose this %", 0);
				log.info(I18n.get("log.fc70f29d7a04", player.getName()));
			}
		}
	}

	private void thievesMessage(Player player, String msg, int type) {
		String typeMsg = "";
		switch (type) {
		case 1:
			typeMsg = "Received a new rank of theft: ";
			break;
		default:
			break;
		}
		PacketSendUtility.sendMessage(player,
				"[color:Guild;0 255 0][color:in;0 255 0][color:moat;0 255 0]: " + typeMsg + msg + ".");
	}

	private void stopThievesTask(Player player) {
		Future<?> thievesTask = player.getController().getTask(TaskId.THIEVES);
		if (thievesTask != null) {
			player.getController().cancelTask(TaskId.THIEVES);
		}
	}

	private void scheduleThievesTask(final Player player, long thievesTimer) {
		player.getController().addTask(TaskId.THIEVES, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				captchaCheck(player, 0, false, 0);
			}
		}, thievesTimer));
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static ThievesGuildService getInstance() {
		ObjectProvider<ThievesGuildService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<ThievesGuildService> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {
		protected static final ThievesGuildService instance = new ThievesGuildService();
	}
}
