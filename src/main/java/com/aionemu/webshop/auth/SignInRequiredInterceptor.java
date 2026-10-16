package com.aionemu.webshop.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 需登录接口的鉴权拦截器：下单、我的订单等个人操作必须已登录。
 * Authorisation interceptor for endpoints that require a session: ordering and personal order views need a
 * signed-in account.
 */
@Component
public class SignInRequiredInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (WebShopSession.from(request) == null) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return false;
		}
		return true;
	}
}
