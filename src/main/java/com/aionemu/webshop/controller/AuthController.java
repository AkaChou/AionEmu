package com.aionemu.webshop.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aionemu.webshop.auth.WebShopAuthService;
import com.aionemu.webshop.auth.WebShopSession;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录接口：账号沿用登录库，权限沿用 access_level。
 * Sign-in endpoints: accounts come from the login database and permissions from access_level.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final WebShopAuthService authService;

	public AuthController(WebShopAuthService authService) {
		this.authService = authService;
	}

	/**
	 * 返回当前登录状态，供页面初始化时决定显示登录表单还是商城。
	 * Returns the current sign-in state so a page can decide between the sign-in form and the shop.
	 *
	 * @param request 当前请求 / current request
	 * @return 登录状态 / sign-in state
	 */
	@GetMapping("/me")
	public Map<String, Object> me(HttpServletRequest request) {
		return describe(WebShopSession.from(request));
	}

	/**
	 * 登录并把会话写入 HTTP 会话。
	 * Signs in and stores the session in the HTTP session.
	 *
	 * @param body    请求体（name / password）/ request body (name / password)
	 * @param request 当前请求 / current request
	 * @return 成功返回登录状态，失败返回 401 与本地化原因 / the sign-in state on success, or 401 with a localised reason
	 */
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest body, HttpServletRequest request) {
		WebShopAuthService.SignInResult result = authService.signIn(body.name(), body.password());
		if (!result.ok()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", result.message()));
		}
		WebShopSession.store(request, result.session());
		return ResponseEntity.ok(describe(result.session()));
	}

	/**
	 * 登出：让 HTTP 会话失效。
	 * Signs out by invalidating the HTTP session.
	 *
	 * @param request 当前请求 / current request
	 * @return 登出后的状态（未登录）/ the state after signing out (signed out)
	 */
	@PostMapping("/logout")
	public Map<String, Object> logout(HttpServletRequest request) {
		WebShopSession.clear(request);
		return describe(null);
	}

	private static Map<String, Object> describe(WebShopSession session) {
		if (session == null) {
			return Map.of("signedIn", false, "admin", false);
		}
		return Map.of(
			"signedIn", true,
			"accountId", session.accountId(),
			"accountName", session.accountName(),
			"accessLevel", session.accessLevel(),
			"admin", session.isAdmin());
	}

	/**
	 * 登录请求体。
	 * Sign-in request body.
	 *
	 * @param name     账号名 / account name
	 * @param password 明文密码 / plain-text password
	 */
	public record LoginRequest(String name, String password) {
	}
}
