package com.aionemu.commons.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 账号密码散列工具：SHA-1 摘要后以 Base64 包装，是登录服与网页商城共用的唯一实现。
 * Account password hashing: an SHA-1 digest wrapped in Base64; the single implementation shared by the
 * login server and the web shop.
 * <p>
 * 该格式与 {@code ls_account_data.password} 列的历史数据一致（无盐、无迭代），因此只适用于兼容既有账号，
 * 新密码学要求下不应作为首选。
 * The format matches the historical values of the {@code ls_account_data.password} column (unsalted, no
 * key stretching); it exists for compatibility with existing accounts and is not a preferred choice under
 * modern cryptographic requirements.
 * </p>
 *
 * @author SoulKeeper
 */
public final class PasswordHash {

	private PasswordHash() {
	}

	/**
	 * 计算密码散列：先 SHA-1 摘要，再 Base64（无换行）编码。
	 * Hashes a password: SHA-1 digest, then Base64 without line breaks.
	 *
	 * @param password 明文密码 / plain-text password
	 * @return Base64 编码的 SHA-1 散列 / Base64-encoded SHA-1 digest
	 * @throws IllegalStateException 运行环境缺失 SHA-1 时 / when SHA-1 is unavailable in the runtime
	 */
	public static String encode(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(password.getBytes(StandardCharsets.UTF_8));
			return Base64.encodeToString(digest.digest(), false);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 digest is unavailable", e);
		}
	}

	/**
	 * 校验明文密码是否与已存散列匹配（常量时间比较，避免早退泄露前缀）。
	 * Verifies a plain-text password against a stored hash (constant-time comparison, no early exit that
	 * would leak the matching prefix length).
	 *
	 * @param password     明文密码 / plain-text password
	 * @param storedHash   数据库中的散列值 / hash stored in the database
	 * @return 匹配返回 true / true when they match
	 */
	public static boolean matches(String password, String storedHash) {
		if (password == null || storedHash == null) {
			return false;
		}
		return MessageDigest.isEqual(encode(password).getBytes(StandardCharsets.UTF_8),
			storedHash.getBytes(StandardCharsets.UTF_8));
	}
}
