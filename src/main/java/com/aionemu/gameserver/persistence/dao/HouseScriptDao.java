package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.PlayerScripts;
import com.aionemu.gameserver.persistence.entity.HouseScriptPo;
import com.mybatisflex.core.service.IService;

/**
 * 房屋脚本数据访问层：负责存储与加载 house_scripts 数据。
 * House scripts data access layer: stores/loads house_scripts data.
 */
public interface HouseScriptDao extends IService<HouseScriptPo> {

	/**
	 * 获取玩家房屋脚本。
	 * Gets player house scripts.
	 *
	 * @param paramInt 房屋/玩家相关 ID / house or player related ID
	 * @return 玩家脚本 / player scripts
	 */
	PlayerScripts getPlayerScripts(int paramInt);

	/**
	 * 添加房屋脚本。
	 * Adds a house script.
	 *
	 * @param paramInt1    房屋/玩家相关 ID / house or player related ID
	 * @param paramInt2    脚本索引 / script index
	 * @param paramString  脚本内容 / script content
	 */
	void addScript(int paramInt1, int paramInt2, String paramString);

	/**
	 * 更新房屋脚本。
	 * Updates a house script.
	 *
	 * @param paramInt1    房屋/玩家相关 ID / house or player related ID
	 * @param paramInt2    脚本索引 / script index
	 * @param paramString  脚本内容 / script content
	 */
	void updateScript(int paramInt1, int paramInt2, String paramString);
}
