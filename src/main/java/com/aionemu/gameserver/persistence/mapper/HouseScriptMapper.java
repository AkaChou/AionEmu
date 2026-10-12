package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.HouseScriptPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 house_scripts 的映射层。
 * Mapper for table house_scripts.
 */
public interface HouseScriptMapper extends BaseMapper<HouseScriptPo> {

	/**
	 * 合并写入房屋脚本（不存在则插入，已存在则更新 script）。
	 * Merges a house script (insert when absent, update script when present).
	 * <p>
	 * ON DUPLICATE KEY UPDATE 为 MySQL 方言语法，Flex CRUD 无法表达，故保留注解 SQL。
	 * ON DUPLICATE KEY UPDATE is MySQL dialect syntax that Flex CRUD cannot express, so the annotated SQL is kept.
	 * </p>
	 *
	 * @param houseId  房屋 ID / house id
	 * @param position 脚本槽位 / script slot
	 * @param script   脚本内容 / script content
	 * @return 受影响行数 / affected row count
	 */
	@Insert("INSERT INTO `house_scripts` (`house_id`, `index`, `script`) VALUES (#{houseId}, #{position}, #{script}) "
		+ "ON DUPLICATE KEY UPDATE `script` = VALUES(`script`)")
	int mergeScript(@Param("houseId") int houseId, @Param("position") int position, @Param("script") String script);
}
