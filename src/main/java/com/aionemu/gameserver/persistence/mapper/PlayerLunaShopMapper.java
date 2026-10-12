package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Delete;

import com.aionemu.gameserver.persistence.entity.PlayerLunaShopPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_luna_shop 的映射层。
 * Mapper for table player_luna_shop.
 */
public interface PlayerLunaShopMapper extends BaseMapper<PlayerLunaShopPo> {

	/**
	 * 清空全部月神商店记录（无 WHERE 的 DELETE 被 Flex CRUD 拒绝，须保留注解 SQL）。
	 * Deletes every Luna Shop record (Flex CRUD refuses WHERE-less DELETE, hence annotated SQL).
	 * @return 影响行数 / affected rows
	 */
	@Delete("DELETE FROM `player_luna_shop`")
	int deleteAll();
}
