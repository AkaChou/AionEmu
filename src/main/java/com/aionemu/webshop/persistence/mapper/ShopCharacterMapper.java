package com.aionemu.webshop.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.aionemu.webshop.model.ShopCharacter;

/**
 * 角色只读投影的查询映射（直接读 players 表，不经过游戏服运行期服务）。
 * Query mapper for the read-only character projection (reads the players table directly, without going
 * through game-server runtime services).
 */
public interface ShopCharacterMapper {

	/**
	 * 按账号名列出角色，供下单时选择发货对象。
	 * Lists the characters of an account so the order form can pick a delivery target.
	 *
	 * @param accountName 账号名 / account name
	 * @return 角色摘要列表 / character summaries
	 */
	@Select("SELECT id AS id, `name` AS name, `race` AS race, `gender` AS gender, "
		+ "`player_class` AS playerClass FROM players WHERE account_name = #{accountName} ORDER BY id")
	List<ShopCharacter> findByAccountName(@Param("accountName") String accountName);

	/**
	 * 校验角色归属：仅当该角色属于指定账号时才返回其 ID。
	 * Verifies ownership: returns the character id only when it belongs to the given account.
	 *
	 * @param accountName 账号名 / account name
	 * @param playerId    角色 ID / character id
	 * @return 命中时返回角色 ID，否则为 null / the character id when it matches, otherwise null
	 */
	@Select("SELECT id FROM players WHERE account_name = #{accountName} AND id = #{playerId}")
	Integer findOwnedPlayerId(@Param("accountName") String accountName, @Param("playerId") int playerId);

	/**
	 * 读取属于指定账号的角色名；不存在时返回 null。
	 * Reads the name of a character belonging to the given account; null when it does not exist.
	 *
	 * @param accountName 账号名 / account name
	 * @param playerId    角色 ID / character id
	 * @return 角色名或 null / the character name, or null
	 */
	@Select("SELECT `name` FROM players WHERE account_name = #{accountName} AND id = #{playerId}")
	String findName(@Param("accountName") String accountName, @Param("playerId") int playerId);
}
