package com.aionemu.boot.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 管理控制台会话服务测试。
 * Tests for the admin console session service.
 */
class AdminConsoleSessionServiceTest {

	/**
	 * 正确凭据建立会话，错误凭据被拒绝。
	 * Correct credentials establish a session; wrong credentials are rejected.
	 */
	@Test
	void correctCredentialsEstablishASession() {
		AdminConsoleSessionService service = new AdminConsoleSessionService(
			new AdminConsoleSettings(true, "127.0.0.1", 8080, "aion", "aion", 120));

		String sessionId = service.login("aion", "aion");

		assertNotEquals(null, sessionId, "correct credentials must establish a session");
		assertTrue(service.isValid(sessionId));
	}

	/**
	 * 错误凭据不得建立会话。
	 * Wrong credentials must not establish a session.
	 */
	@Test
	void wrongCredentialsAreRejected() {
		AdminConsoleSessionService service = new AdminConsoleSessionService(
			new AdminConsoleSettings(true, "127.0.0.1", 8080, "aion", "aion", 120));

		assertNull(service.login("aion", "wrong"));
		assertNull(service.login("wrong", "aion"));
		assertNull(service.login(null, null));
	}

	/**
	 * 凭据缺失时拒绝一切登录（fail-closed）：宁可无人能登录，也不放行无凭据访问。
	 * Every login is rejected when the credentials are missing (fail-closed): locking everyone out is
	 * preferable to granting credential-less access.
	 */
	@Test
	void missingCredentialsRejectEveryLogin() {
		AdminConsoleSessionService service = new AdminConsoleSessionService(
			new AdminConsoleSettings(true, "127.0.0.1", 8080, "", "", 120));

		assertNull(service.login("", ""));
		assertNull(service.login("aion", "aion"));
	}

	/**
	 * 退出后会话立即失效。
	 * A session stops being valid right after the logout.
	 */
	@Test
	void logoutInvalidatesTheSession() {
		AdminConsoleSessionService service = new AdminConsoleSessionService(
			new AdminConsoleSettings(true, "127.0.0.1", 8080, "aion", "aion", 120));

		String sessionId = service.login("aion", "aion");
		service.logout(sessionId);

		assertFalse(service.isValid(sessionId));
	}

	/**
	 * 未知或缺失的会话标识一律无效。
	 * An unknown or missing session id is always invalid.
	 */
	@Test
	void unknownSessionIdsAreInvalid() {
		AdminConsoleSessionService service = new AdminConsoleSessionService(
			new AdminConsoleSettings(true, "127.0.0.1", 8080, "aion", "aion", 120));

		assertFalse(service.isValid(null));
		assertFalse(service.isValid("not-a-session"));
	}

	/**
	 * 每次登录建立独立会话。
	 * Every login establishes an independent session.
	 */
	@Test
	void eachLoginEstablishesAnIndependentSession() {
		AdminConsoleSessionService service = new AdminConsoleSessionService(
			new AdminConsoleSettings(true, "127.0.0.1", 8080, "aion", "aion", 120));

		String first = service.login("aion", "aion");
		String second = service.login("aion", "aion");

		assertNotEquals(first, second, "each login must get its own session id");
		assertTrue(service.isValid(first));
		assertTrue(service.isValid(second));
	}
}
