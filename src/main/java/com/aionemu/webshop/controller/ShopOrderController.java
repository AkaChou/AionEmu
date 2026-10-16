package com.aionemu.webshop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aionemu.webshop.auth.WebShopSession;
import com.aionemu.webshop.model.ShopCharacter;
import com.aionemu.webshop.model.ShopOrderView;
import com.aionemu.webshop.persistence.dao.ShopCharacterDao;
import com.aionemu.webshop.persistence.dao.ShopOrderDao;
import com.aionemu.webshop.service.ShopOrderService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 个人接口：角色列表、下单与我的订单。路径由登录拦截器保护。
 * Personal endpoints: character list, ordering and my orders. The path is guarded by the sign-in interceptor.
 */
@RestController
@RequestMapping("/api/me")
public class ShopOrderController {

	private final ShopOrderService orderService;

	private final ShopOrderDao orderDao;

	private final ShopCharacterDao characterDao;

	public ShopOrderController(ShopOrderService orderService, ShopOrderDao orderDao,
			ShopCharacterDao characterDao) {
		this.orderService = orderService;
		this.orderDao = orderDao;
		this.characterDao = characterDao;
	}

	/**
	 * 返回当前账号的角色列表，供下单时选择收货角色。
	 * Returns the characters of the signed-in account so the order form can pick a recipient.
	 *
	 * @param request 当前请求 / current request
	 * @return 角色列表 / characters
	 */
	@GetMapping("/characters")
	public List<ShopCharacter> characters(HttpServletRequest request) {
		return characterDao.listByAccount(WebShopSession.from(request).accountName());
	}

	/**
	 * 下单；物品经既有的发货队列以系统邮件送达。
	 * Places an order; the item reaches the player as a system mail through the existing delivery queue.
	 *
	 * @param body    请求体（playerId / productId / quantity）/ request body
	 * @param request 当前请求 / current request
	 * @return 成功返回订单 ID，失败返回 400 与本地化原因 / the order id on success, or 400 with a localised reason
	 */
	@PostMapping("/orders")
	public ResponseEntity<Map<String, Object>> purchase(@RequestBody PurchaseRequest body,
			HttpServletRequest request) {
		WebShopSession session = WebShopSession.from(request);
		ShopOrderService.PurchaseResult result = orderService.purchase(session,
			body.playerId(), body.productId(), body.quantity());
		if (!result.ok()) {
			return ResponseEntity.badRequest().body(Map.of("error", result.message()));
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("orderId", result.orderId()));
	}

	/**
	 * 分页返回当前账号的订单（含发货状态）。
	 * Pages the orders of the signed-in account, with delivery state.
	 *
	 * @param page     页码，从 0 开始 / zero-based page index
	 * @param pageSize 每页条数 / page size
	 * @param request  当前请求 / current request
	 * @return 分页结果 / paged result
	 */
	@GetMapping("/orders")
	public PageResult<ShopOrderView> orders(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int pageSize, HttpServletRequest request) {
		WebShopSession session = WebShopSession.from(request);
		int safePage = PageResult.normalisePage(page);
		int safeSize = PageResult.normalisePageSize(pageSize);
		List<ShopOrderView> items = orderDao.listByAccount(session.accountId(), safePage, safeSize);
		long total = orderDao.countByAccount(session.accountId());
		return new PageResult<>(items, total, safePage, safeSize);
	}

	/**
	 * 下单请求体。
	 * Purchase request body.
	 *
	 * @param playerId  收货角色 ID / receiving character id
	 * @param productId 商品 ID / product id
	 * @param quantity  数量 / quantity
	 */
	public record PurchaseRequest(int playerId, int productId, int quantity) {
	}
}
