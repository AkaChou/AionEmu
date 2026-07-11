package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerShugoSweepDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSweep;
import com.aionemu.gameserver.model.templates.shugosweep.ShugoSweepReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SHUGO_SWEEP;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 术古扫荡棋盘服务，管理骰子、棋盘进度与奖励。
 * Shugo Sweep board service managing dice, board progress, and rewards.
 *
 * @author Rinzler (Encom)
 */
@Slf4j
public class ShugoSweepService {

	/** Spring 实例提供者 / Spring instance provider */
	private static volatile ObjectProvider<ShugoSweepService> instanceProvider;

	/** Current board id / Current board id */
	private final int boardId = EventsConfig.EVENT_SHUGOSWEEP_BOARD;

	/**
	 * 初始化术古扫荡并注册每周重置 cron。
	 * Initializes Shugo Sweep and registers the weekly reset cron.
	 */
	public void initShugoSweep() {
		log.info(I18n.get("log.a06db47829be"));
		String weekly = "0 0 9 ? * WED *";
		GameCronServices.cronService().schedule(() -> DAOManager.getDAO(PlayerShugoSweepDAO.class).delete(), weekly);
	}

	/**
	 * 登录时加载棋盘数据并同步客户端。
	 * On login, loads board data and syncs the client.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		DAOManager.getDAO(PlayerShugoSweepDAO.class).load(player);
		if (player.getPlayerShugoSweep() == null) {
			PlayerSweep ps = new PlayerSweep(0, EventsConfig.EVENT_SHUGOSWEEP_FREEDICE, boardId);
			ps.setPersistentState(PersistentState.UPDATE_REQUIRED);
			player.setPlayerShugoSweep(ps);
			player.getPlayerShugoSweep().setShugoSweepByObjId(player.getObjectId());
			DAOManager.getDAO(PlayerShugoSweepDAO.class).add(player.getObjectId(), ps.getFreeDice(), ps.getStep(),
					ps.getBoardId());
		}

		if (player.getPlayerShugoSweep().getBoardId() != boardId) {
			PlayerSweep ps = new PlayerSweep(0, getPlayerSweep(player).getFreeDice(), boardId);
			ps.setPersistentState(PersistentState.UPDATE_REQUIRED);
			player.setPlayerShugoSweep(ps);
			player.getPlayerShugoSweep().setShugoSweepByObjId(player.getObjectId());
		}
		PacketSendUtility.sendPacket(player,
				new SM_SHUGO_SWEEP(getPlayerSweep(player).getBoardId(), getPlayerSweep(player).getStep(),
						getPlayerSweep(player).getFreeDice(), getCommonData(player).getGoldenDice(),
						getCommonData(player).getResetBoard(), 0));
	}

	/**
	 * 登出时持久化棋盘进度。
	 * On logout, persists board progress.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogout(Player player) {
		DAOManager.getDAO(PlayerShugoSweepDAO.class).store(player);
		player.getPlayerShugoSweep().setShugoSweepByObjId(player.getObjectId());
	}

	/**
	 * 投掷骰子推进棋盘并结算奖励。
	 * Rolls dice to advance the board and settle rewards.
	 *
	 * @param player 玩家 / player
	 */
	public void launchDice(final Player player) {
		int move = Rnd.get(1, 6);
		int step = getPlayerSweep(player).getStep();
		int newStep = step + move;
		int dice = getPlayerSweep(player).getFreeDice();
		int goldDice = getCommonData(player).getGoldenDice();
		int diff = newStep - 30;
		if (getPlayerSweep(player).getFreeDice() != 0) {
			getPlayerSweep(player).setFreeDice(dice - 1);
			player.getPlayerShugoSweep().setShugoSweepByObjId(player.getObjectId());
		} else {
			getCommonData(player).setGoldenDice(goldDice - 1);
			DAOManager.getDAO(PlayerDAO.class).storePlayer(player);
		}

		PacketSendUtility.sendPacket(player, new SM_SHUGO_SWEEP(boardId, getPlayerSweep(player).getStep(),
				getPlayerSweep(player).getFreeDice(), getCommonData(player).getGoldenDice(), 0, 0));

		if (newStep > 30) {
			log.debug("Shugo sweep move wraps board. playerId={} step={} move={} newStep={}", player.getObjectId(),
					step, move, newStep);
			getPlayerSweep(player).setStep(newStep);
			PacketSendUtility.sendPacket(player,
					new SM_SHUGO_SWEEP(getPlayerSweep(player).getBoardId(), getPlayerSweep(player).getStep(),
							getPlayerSweep(player).getFreeDice(), getCommonData(player).getGoldenDice(),
							getCommonData(player).getResetBoard(), move));
			getPlayerSweep(player).setStep(diff);
			rewardPlayer(player, getPlayerSweep(player).getStep(), diff);
			player.getPlayerShugoSweep().setShugoSweepByObjId(player.getObjectId());
		} else if (newStep == 30) {
			log.debug("Shugo sweep move reaches finish. playerId={} step={} move={} newStep={}", player.getObjectId(),
					step, move, newStep);
			getPlayerSweep(player).setStep(newStep);
			PacketSendUtility.sendPacket(player,
					new SM_SHUGO_SWEEP(getPlayerSweep(player).getBoardId(), getPlayerSweep(player).getStep(),
							getPlayerSweep(player).getFreeDice(), getCommonData(player).getGoldenDice(),
							getCommonData(player).getResetBoard(), move));
			rewardPlayer(player, getPlayerSweep(player).getStep(), newStep);
			player.getPlayerShugoSweep().setShugoSweepByObjId(player.getObjectId());
		} else {
			log.debug("Shugo sweep move. playerId={} step={} move={} newStep={}", player.getObjectId(), step, move,
					newStep);
			getPlayerSweep(player).setStep(newStep);
			player.getPlayerShugoSweep().setShugoSweepByObjId(player.getObjectId());
			PacketSendUtility.sendPacket(player,
					new SM_SHUGO_SWEEP(getPlayerSweep(player).getBoardId(), getPlayerSweep(player).getStep(),
							getPlayerSweep(player).getFreeDice(), getCommonData(player).getGoldenDice(),
							getCommonData(player).getResetBoard(), move));
			rewardPlayer(player, getPlayerSweep(player).getStep(), move);
		}
	}

	/**
	 * 重置玩家棋盘进度。
	 * Resets the player's board progress.
	 *
	 * @param player 玩家 / player
	 */
	public void resetBoard(Player player) {
		int reset = getCommonData(player).getResetBoard();
		getCommonData(player).setResetBoard(reset - 1);
		getPlayerSweep(player).setStep(0);
		PacketSendUtility.sendPacket(player,
				new SM_SHUGO_SWEEP(getPlayerSweep(player).getBoardId(), 0, getPlayerSweep(player).getFreeDice(),
						getCommonData(player).getGoldenDice(), getCommonData(player).getResetBoard(), 0));
	}

	/**
	 * 延迟发放当前格子奖励。
	 * Delays granting the reward for the current board step.
	 *
	 * @param player 玩家 / player
	 * @param step 当前格子 / current step
	 * @param move 步数（用于延迟） / move count (used for delay)
	 */
	private void rewardPlayer(final Player player, final int step, final int move) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (player.isOnline()) {
					ShugoSweepReward reward = getRewardForBoard(boardId, step);
					ItemService.addItem(player, reward.getItemId(), reward.getCount());
				}
			}
		}, move * 1200);

	}

	/**
	 * 返回玩家公共数据。
	 * Returns the player's common data.
	 *
	 * 玩家 / player
	 * common data
	 */
	private PlayerCommonData getCommonData(Player player) {
		return player.getCommonData();
	}

	/**
	 * 返回玩家扫荡棋盘状态。
	 * Returns the player's sweep board state.
	 *
	 * 玩家 / player
	 * sweep state
	 */
	private PlayerSweep getPlayerSweep(Player player) {
		return player.getPlayerShugoSweep();
	}

	/**
	 * 按棋盘与格子查询奖励模板。
	 * Looks up the reward template by board and step.
	 *
	 * board id
	 * step
	 * reward template
	 */
	private static ShugoSweepReward getRewardForBoard(int boardId, int step) {
		return DataManager.SHUGO_SWEEP_REWARD_DATA.getRewardBoard(boardId, step);
	}

	/**
	 * 返回单例，优先使用 Spring ObjectProvider。
	 * Returns the singleton, preferring a Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final ShugoSweepService getInstance() {
		ObjectProvider<ShugoSweepService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider that overrides the default singleton.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<ShugoSweepService> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {
		protected static final ShugoSweepService instance = new ShugoSweepService();
	}
}
