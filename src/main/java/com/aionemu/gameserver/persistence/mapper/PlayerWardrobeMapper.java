package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerWardrobePo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_wardrobe 的映射层。
 * Mapper for table player_wardrobe.
 */
public interface PlayerWardrobeMapper extends BaseMapper<PlayerWardrobePo> {

	/**
	 * 插入或更新衣柜条目（MySQL upsert 方言，重复时仅回写物品与槽位，不回写重塑次数）。
	 * Inserts or updates a wardrobe entry (MySQL upsert dialect). On duplicate, only the item id and the slot are
	 * written back, not the reskin count.
	 * @param playerId 玩家对象 ID / player object id
	 * @param itemId 物品 ID / item id
	 * @param slot 槽位 / slot
	 * @param reskinCount 重塑皮肤次数 / reskin count
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_wardrobe` (`player_id`, `item_id`, `slot`, `reskin_count`) "
		+ "VALUES (#{playerId}, #{itemId}, #{slot}, #{reskinCount}) "
		+ "ON DUPLICATE KEY UPDATE `item_id` = VALUES(`item_id`), `slot` = VALUES(`slot`)")
	int upsert(@Param("playerId") int playerId, @Param("itemId") int itemId, @Param("slot") int slot,
		@Param("reskinCount") int reskinCount);
}
