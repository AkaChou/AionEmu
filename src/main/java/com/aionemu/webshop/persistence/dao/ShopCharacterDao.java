package com.aionemu.webshop.persistence.dao;

import java.util.List;

import com.aionemu.webshop.model.ShopCharacter;

/**
 * 角色只读数据访问层（供下单时选择发货对象与归属校验）。
 * Read-only character data access (used to pick a delivery target and to verify ownership).
 */
public interface ShopCharacterDao {

	/**
	 * 按账号名列出角色。
	 * Lists the characters of an account.
	 *
	 * @param accountName 账号名 / account name
	 * @return 角色摘要列表 / character summaries
	 */
	List<ShopCharacter> listByAccount(String accountName);

	/**
	 * 校验角色属于该账号。
	 * Verifies the character belongs to the account.
	 *
	 * @param accountName 账号名 / account name
	 * @param playerId    角色 ID / character id
	 * @return 归属成立返回 true / true when the character belongs to the account
	 */
	boolean isOwnedBy(String accountName, int playerId);

	/**
	 * 读取属于该账号的角色名；不存在时返回 null。
	 * Reads the name of a character belonging to the account; null when it does not exist.
	 *
	 * @param accountName 账号名 / account name
	 * @param playerId    角色 ID / character id
	 * @return 角色名或 null / the character name, or null
	 */
	String findName(String accountName, int playerId);
}
