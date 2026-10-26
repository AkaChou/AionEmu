package com.aionemu.boot.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.aionemu.commons.utils.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 管理控制台会话服务：凭据校验与会话生命周期。
 * Admin console session service: credential validation and session lifecycle.
 * <p>
 * 凭据来自 {@code config/web/web.properties}，<b>不</b>来自游戏账号表：控制台是与游戏服并列的运维入口，
 * 独立凭据避免把「能改 GM 权限」的能力绑在玩家账号上。
 * <p>
 * The credentials come from {@code config/web/web.properties} and <b>not</b> from the game account table:
 * the console is an operator entry point parallel to the game server, and independent credentials avoid
 * binding the "can change GM access levels" capability to a player account.
 * <p>
 * 密码比对沿用游戏服的 {@code AccountController.encodePassword} 语义（SHA-1 + Base64），保持同一套约定。
 * <p>
 * The password comparison follows the game server's {@code AccountController.encodePassword} semantics
 * (SHA-1 + Base64), keeping one convention.
 * <p>
 * <b>线程安全</b>：会话表为并发映射；过期会话在访问时惰性清理。
 * <b>Thread safety</b>: the session table is a concurrent map; expired sessions are cleaned lazily on access.
 *
 * @author AionEmu
 */
@Slf4j
@Service
public class AdminConsoleSessionService {

	/**
	 * 会话标识到会话的映射。
	 * Session id to session map.
	 */
	private final Map<String, Session> sessions = new ConcurrentHashMap<>();

	/**
	 * 随机数生成器：用于会话标识。
	 * Random generator used for the session ids.
	 */
	private final SecureRandom random = new SecureRandom();

	/**
 * 控制台设置。
	 * Console settings.
	 */
	private final AdminConsoleSettings settings;

	/**
	 * 构造会话服务。
	 * Constructs the session service.
	 * @param settings 控制台设置 / console settings
	 */
	public AdminConsoleSessionService(AdminConsoleSettings settings) {
		this.settings = settings;
	}

	/**
	 * 校验凭据并建立会话。
	 * Validates the credentials and establishes a session.
	 * @param username 用户名 / user name
	 * @param password 密码 / password
	 * @return 会话标识；校验失败时为 {@code null} / the session id, {@code null} when validation fails
	 */
	public String login(String username, String password) {
		if (!settings.hasCredentials()) {
			// 凭据缺失时拒绝一切登录（fail-closed）：宁可无人能登录，也不放行无凭据访问。
			// Every login is rejected when the credentials are missing (fail-closed): locking everyone out is
			// preferable to granting credential-less access.
			return null;
		}
		if (username == null || password == null) {
			return null;
		}
		if (!settings.username().equals(username.trim())) {
			return null;
		}
		if (!encodePassword(password).equals(encodePassword(settings.password()))) {
			return null;
		}
		purgeExpired();
		String id = generateSessionId();
		sessions.put(id, new Session(Instant.now().plusSeconds(settings.sessionTimeoutMinutes() * 60L)));
		return id;
	}

	/**
	 * 结束会话。
	 * Ends a session.
	 * @param sessionId 会话标识 / session id
	 */
	public void logout(String sessionId) {
		if (sessionId != null) {
			sessions.remove(sessionId);
		}
	}

	/**
	 * 判断会话是否有效。
	 * Returns whether a session is valid.
	 * @param sessionId 会话标识 / session id
	 * @return 有效则为 true / true when valid
	 */
	public boolean isValid(String sessionId) {
		if (sessionId == null) {
			return false;
		}
		Session session = sessions.get(sessionId);
		if (session == null) {
			return false;
		}
		if (session.expiresAt().isBefore(Instant.now())) {
			sessions.remove(sessionId);
			return false;
		}
		return true;
	}

	/**
	 * 清理过期会话。
	 * Purges the expired sessions.
	 */
	private void purgeExpired() {
		Instant now = Instant.now();
		sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
	}

	/**
	 * 生成会话标识。
	 * Generates a session id.
	 * @return 会话标识 / session id
	 */
	private String generateSessionId() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * 编码密码：与游戏服的 {@code AccountController.encodePassword} 语义一致。
	 * Encodes a password, matching the game server's {@code AccountController.encodePassword} semantics.
	 * @param password 明文密码 / plain password
	 * @return 编码后的密码 / the encoded password
	 */
	private String encodePassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(password.getBytes(StandardCharsets.UTF_8));
			return Base64.encodeToString(digest.digest(), false);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 is not available", e);
		}
	}

	/**
	 * 会话。
	 * A session.
	 *
	 * @param expiresAt 过期时刻 / expiry instant
	 */
	private record Session(Instant expiresAt) {
	}
}
