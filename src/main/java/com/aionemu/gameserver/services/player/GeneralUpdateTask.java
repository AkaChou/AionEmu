package com.aionemu.gameserver.services.player;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.world.World;
import lombok.extern.slf4j.Slf4j;

/**
 * 玩家通用周期更新任务，批量持久化玩家常规数据。
 * Player general periodic update task batch-persisting regular player data.
 *
 * @author Source
 */

@Slf4j
class GeneralUpdateTask implements Runnable {

	private final int playerId;

	GeneralUpdateTask(int playerId) {
		this.playerId = playerId;
	}

	/**
	 * 执行任务。
	 * Runs the task.
	 */
	public void run() {
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
		if (player != null)
			try {
				DAOManager.getDAO(AbyssRankDAO.class).storeAbyssRank(player);
				DAOManager.getDAO(PlayerSkillListDAO.class).storeSkills(player);
				DAOManager.getDAO(PlayerQuestListDAO.class).store(player);
				DAOManager.getDAO(PlayerDAO.class).storePlayer(player);
				DAOManager.getDAO(PlayerStigmasEquippedDAO.class).storeItems(player);
				for (House house : player.getHouses())
					house.save();
			} catch (Exception ex) {
				log.error(I18n.get("log.fe1aeacc963e", player.getName(), ex), ex);
			}
	}
}
