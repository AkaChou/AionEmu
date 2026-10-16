package com.aionemu.webshop.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 后台管理接口的鉴权拦截器：仅 GM（access_level ≥ 3）可访问。
 * Authorisation interceptor for the admin console API: only GMs (access_level ≥ 3) may pass.
 * <p>拦截器只做准入判定，具体业务逻辑不在这里；未登录返回 401，权限不足返回 403，便于前端区分处理。
 * The interceptor only decides admission; business logic stays elsewhere. It answers 401 when signed out
 * and 403 when the access level is too low, so the front end can tell the two apart.</p>
 */
@Component
public class AdminOnlyInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		WebShopSession session = WebShopSession.from(request);
		if (session == null) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return false;
		}
		if (!session.isAdmin()) {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			return false;
		}
		return true;
	}
}
