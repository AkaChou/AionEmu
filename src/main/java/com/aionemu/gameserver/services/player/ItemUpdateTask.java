package com.aionemu.gameserver.services.player;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.World;
import lombok.extern.slf4j.Slf4j;

/**
 * 玩家物品周期更新任务，批量持久化物品数据。
 * Player item periodic update task batch-persisting item data.
 *
 * @author Source
 */

@Slf4j
class ItemUpdateTask implements Runnable {

	private final int playerId;

	ItemUpdateTask(int playerId) {
		this.playerId = playerId;
	}

	@Override
	/**
	 * 执行任务。
	 * Runs the task.
	 */
	public void run() {
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
		if (player != null)
			try {
				DAOManager.getDAO(InventoryDAO.class).store(player);
				DAOManager.getDAO(ItemStoneListDAO.class).save(player);
			} catch (Exception ex) {
				log.error(I18n.get("log.7850b77ba785", player.getName(), ex), ex);
			}
	}
}
