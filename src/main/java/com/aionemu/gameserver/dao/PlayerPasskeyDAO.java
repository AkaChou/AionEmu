package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;

/**
 * 玩家/账号二级密码（Passkey）数据访问对象。
 * Player/account passkey data access object.
 *
 * @author cura
 */
public abstract class PlayerPasskeyDAO implements DAO {

	/**
	 * 为账号插入二级密码。
	 * Inserts a passkey for the account.
	 *
	 * @param accountId 账号 ID / account id
	 * @param passkey 通行码 / passkey
	 */
	public abstract void insertPlayerPasskey(int accountId, String passkey);

	/**
	 * 在校验旧密码后更新二级密码。
	 * Updates the passkey after verifying the old one.
	 *
	 * @param accountId 账号 ID / account id
	 * @param oldPasskey 旧通行码 / old passkey
	 * @param newPasskey 新通行码 / new passkey
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean updatePlayerPasskey(int accountId, String oldPasskey, String newPasskey);

	/**
	 * 强制更新二级密码（不校验旧密码）。
	 * Force-updates the passkey without verifying the old one.
	 *
	 * @param accountId 账号 ID / account id
	 * @param newPasskey 新通行码 / new passkey
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean updateForcePlayerPasskey(int accountId, String newPasskey);

	/**
	 * 校验账号二级密码是否匹配。
	 * Checks whether the passkey matches the account.
	 *
	 * @param accountId 账号 ID / account id
	 * @param passkey 通行码 / passkey
	 * @return 若匹配则为 true / true if matched
	 */
	public abstract boolean checkPlayerPasskey(int accountId, String passkey);

	/**
	 * 判断账号是否已设置二级密码。
	 * Checks whether the account already has a passkey.
	 *
	 * @param accountId 账号 ID / account id
	 * @return 是否已设置 / true if a passkey exists
	 */
	public abstract boolean existCheckPlayerPasskey(int accountId);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerPasskeyDAO.class.getName();
	}
}
