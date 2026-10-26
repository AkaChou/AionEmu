package com.aionemu.boot.web;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 管理控制台鉴权过滤器：除登录与会话探测外，所有 API 请求都必须携带有效会话。
 * Admin console authentication filter: apart from login and the session probe, every API request must carry
 * a valid session.
 * <p>
 * 该过滤器只覆盖 {@code /api/admin-console/**}，静态资源（控制台页面本身）不受影响，因此未登录时页面
 * 仍能加载，由前端跳转到登录视图。
 * <p>
 * The filter only covers {@code /api/admin-console/**}; the static resources (the console page itself) are
 * unaffected, so the page still loads when logged out and the frontend routes to the login view.
 *
 * @author AionEmu
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class AdminConsoleAuthenticationFilter extends OncePerRequestFilter {

	/**
	 * API 前缀。
	 * API prefix.
	 */
	private static final String API_PREFIX = "/api/admin-console/";

	/**
	 * 会话 Cookie 名。
	 * Session cookie name.
	 */
	private static final String SESSION_COOKIE = "ADMIN_CONSOLE_SESSION";

	/**
	 * 无需鉴权的端点。
	 * Endpoints that need no authentication.
	 */
	private static final List<String> PUBLIC_ENDPOINTS = List.of(
		API_PREFIX + "login",
		API_PREFIX + "session");

	/**
	 * 会话服务。
	 * Session service.
	 */
	private final AdminConsoleSessionService sessionService;

	/**
	 * 只对 API 路径生效。
	 * Applies to the API paths only.
	 * @param request 请求 / request
	 * @return 是否需要鉴权 / whether authentication is required
	 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		if (!path.startsWith(API_PREFIX)) {
			return true;
		}
		return PUBLIC_ENDPOINTS.contains(path);
	}

	/**
	 * 校验会话；无效时返回 401。
	 * Validates the session and returns 401 when it is invalid.
	 * @param request 请求 / request
	 * @param response 响应 / response
	 * @param chain 过滤器链 / filter chain
	 * @throws ServletException Servlet 异常 / servlet exception
	 * @throws IOException IO 异常 / IO exception
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		if (!sessionService.isValid(sessionId(request))) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		chain.doFilter(request, response);
	}

	/**
	 * 从请求中取出会话标识。
	 * Extracts the session id from the request.
	 * @param request 请求 / request
	 * @return 会话标识；缺失时为 {@code null} / the session id, {@code null} when absent
	 */
	private String sessionId(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (SESSION_COOKIE.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
