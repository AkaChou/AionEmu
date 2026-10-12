package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerLunaShopPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家月神商店（Luna Shop）数据访问对象。
 * Player Luna Shop data access object.
 * Created by wanke on 13/02/2017.
 */
public interface PlayerLunaShopDao extends IService<PlayerLunaShopPo> {

	/**
	 * 加载玩家月神商店数据。
	 * Loads player Luna Shop data.
	 * @param player 玩家 / player
	 */
	void load(Player player);

	/**
	 * 新增月神商店免费次数记录。
	 * Adds a Luna Shop free-use record.
	 * @param playerId 玩家 ID / player id
	 * @param freeUnderpath 免费地下通道 / free underpath
	 * @param freeFactory 免费工厂 / free factory
	 * @param freeChest 免费宝箱 / free chest
	 * @return 若成功则为 true / true if successful
	 */
	boolean add(int playerId, boolean freeUnderpath, boolean freeFactory, boolean freeChest);

	/**
	 * 删除月神商店数据。
	 * Deletes Luna Shop data.
	 * @return 若成功则为 true / true if successful
	 */
	boolean delete();

	/**
	 * 持久化玩家月神商店数据。
	 * Stores player Luna Shop data.
	 * @param player 玩家 / player
	 * @return 若成功则为 true / true if successful
	 */
	boolean store(Player player);

	/**
	 * 按对象 ID 设置月神商店免费状态。
	 * Sets Luna Shop free flags by object ID.
	 * @param obj 对象 ID / object id
	 * @param freeUnderpath 免费地下通道 / free underpath
	 * @param freeFactory 免费工厂 / free factory
	 * @param freeChest 免费宝箱 / free chest
	 */
	void setLunaShopByObjId(int obj, boolean freeUnderpath, boolean freeFactory, boolean freeChest);
}
