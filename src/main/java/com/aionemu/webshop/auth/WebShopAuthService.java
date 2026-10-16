package com.aionemu.webshop.auth;

import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.PasswordHash;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.persistence.dao.AccountDataDao;

/**
 * 网页商城登录服务：直接校验登录库的既有账号，不引入第二套用户体系。
 * Web shop sign-in service: verifies against the existing login-database accounts, introducing no second
 * user store.
 * <p>密码散列与登录服共用 {@link PasswordHash}，因此同一账号在客户端与网页上的密码始终一致。
 * The password hash is shared with the login server through {@link PasswordHash}, so one account keeps the
 * same password on the client and on the web.</p>
 */
@Service
public class WebShopAuthService {

	private final AccountDataDao accountDataDao;

	public WebShopAuthService(AccountDataDao accountDataDao) {
		this.accountDataDao = accountDataDao;
	}

	/**
	 * 登录校验。
	 * Verifies sign-in credentials.
	 *
	 * @param name     账号名 / account name
	 * @param password 明文密码 / plain-text password
	 * @return 登录结果（成功时含会话，失败时含本地化原因）/ sign-in result (carries the session on success,
	 *         a localised reason on failure)
	 */
	public SignInResult signIn(String name, String password) {
		if (name == null || name.isBlank() || password == null || password.isEmpty()) {
			return SignInResult.failure(I18n.get("webshop.login.missing"));
		}
		Account account = accountDataDao.getAccount(name.trim());
		// 账号不存在与密码错误返回同一条提示，避免枚举账号名。
		// A missing account and a wrong password return the same message to prevent account enumeration.
		if (account == null || !PasswordHash.matches(password, account.getPasswordHash())) {
			return SignInResult.failure(I18n.get("webshop.login.invalid"));
		}
		if (account.getActivated() != 1) {
			return SignInResult.failure(I18n.get("webshop.login.inactive"));
		}
		return SignInResult.success(new WebShopSession(account.getId(), account.getName(), account.getAccessLevel()));
	}

	/**
	 * 登录结果。
	 * Sign-in result.
	 *
	 * @param ok      是否成功 / whether the sign-in succeeded
	 * @param message 失败原因（成功时为 null）/ failure reason (null on success)
	 * @param session 成功时的会话 / session on success
	 */
	public record SignInResult(boolean ok, String message, WebShopSession session) {

		static SignInResult success(WebShopSession session) {
			return new SignInResult(true, null, session);
		}

		static SignInResult failure(String message) {
			return new SignInResult(false, message, null);
		}
	}
}
