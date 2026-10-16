package com.aionemu.webshop.auth;

import java.io.Serial;
import java.io.Serializable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 网页商城的登录会话：承载账号 ID、账号名与权限等级。
 * The web shop login session: carries the account id, account name and access level.
 * <p>刻意不放进 {@code HttpSession} 之外的地方，也不在客户端存放任何凭据；登出即让会话失效。
 * Deliberately kept nowhere but {@code HttpSession}, with no credential stored on the client side;
 * logging out invalidates the session.</p>
 */
public record WebShopSession(int accountId, String accountName, int accessLevel) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/** 会话属性名。 / session attribute name. */
	private static final String ATTRIBUTE = "webshop.session";

	/**
	 * 管理员判定所要求的最低权限等级（3 = GM；语义见 admin.properties：0=Player,1=Support,2=Jr-GM,3=GM,4=Head-GM,5=Admin）。
	 * Minimum access level required for the admin console (3 = GM; the scale is defined in admin.properties:
	 * 0=Player, 1=Support, 2=Jr-GM, 3=GM, 4=Head-GM, 5=Admin).
	 */
	public static final int ADMIN_ACCESS_LEVEL = 3;

	/**
	 * 是否为管理员（GM 及以上）。
	 * Whether this session belongs to an administrator (GM or above).
	 *
	 * @return 达到管理员等级返回 true / true when the access level reaches the admin threshold
	 */
	public boolean isAdmin() {
		return accessLevel >= ADMIN_ACCESS_LEVEL;
	}

	/**
	 * 从请求中取出会话；未登录或会话失效时返回 null。
	 * Reads the session from the request; returns null when signed out or the session has expired.
	 *
	 * @param request 当前请求 / current request
	 * @return 登录会话或 null / the login session, or null
	 */
	public static WebShopSession from(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object value = session.getAttribute(ATTRIBUTE);
		return value instanceof WebShopSession shopSession ? shopSession : null;
	}

	/**
	 * 把会话写入 HTTP 会话（后续请求据此识别身份）。
	 * Stores the session into the HTTP session so later requests can identify the caller.
	 *
	 * @param request 当前请求 / current request
	 * @param session 登录会话 / login session
	 */
	public static void store(HttpServletRequest request, WebShopSession session) {
		request.getSession(true).setAttribute(ATTRIBUTE, session);
	}

	/**
	 * 让当前会话失效（登出）。
	 * Invalidates the current session (sign-out).
	 *
	 * @param request 当前请求 / current request
	 */
	public static void clear(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}
}
