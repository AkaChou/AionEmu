package com.aionemu.webshop.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aionemu.webshop.model.ShopProductView;
import com.aionemu.webshop.persistence.dao.ShopCatalogDao;
import com.aionemu.webshop.persistence.dao.ShopProductDao;
import com.aionemu.webshop.persistence.entity.ShopItemCatalogPo;

/**
 * 后台商品管理接口。整条 /api/admin/** 路径由 GM 鉴权拦截器保护。
 * Admin product endpoints. The whole /api/admin/** path is guarded by the GM authorisation interceptor.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

	private final ShopCatalogDao catalogDao;

	private final ShopProductDao productDao;

	public AdminProductController(ShopCatalogDao catalogDao, ShopProductDao productDao) {
		this.catalogDao = catalogDao;
		this.productDao = productDao;
	}

	/**
	 * 分页列出商品（含未上架），供后台表格。
	 * Pages products including delisted ones, for the admin table.
	 *
	 * @param keyword    关键字 / keyword
	 * @param category   分类路径前缀 / category path prefix
	 * @param page       页码，从 0 开始 / zero-based page index
	 * @param pageSize   每页条数 / page size
	 * @return 分页结果 / paged result
	 */
	@GetMapping("/products")
	public PageResult<ShopProductView> products(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int pageSize) {
		int safePage = PageResult.normalisePage(page);
		int safeSize = PageResult.normalisePageSize(pageSize);
		List<ShopProductView> items = catalogDao.searchProducts(keyword, category, false, safePage, safeSize);
		long total = catalogDao.countProducts(keyword, category, false);
		return new PageResult<>(items, total, safePage, safeSize);
	}

	/**
	 * 上架 / 下架并可选更新定价与限购。
	 * Lists or delists a product, optionally updating its price and purchase limit.
	 *
	 * @param body 请求体 / request body
	 * @return 成功返回 204，失败返回 400 / 204 on success, 400 on failure
	 */
	@PostMapping("/products")
	public ResponseEntity<Map<String, Object>> save(@RequestBody ProductRequest body) {
		boolean saved = productDao.upsert(body.itemId(), body.active(), body.price(), body.purchaseLimit(),
			body.description());
		if (!saved) {
			return ResponseEntity.badRequest().body(Map.of("error", "save failed"));
		}
		return ResponseEntity.noContent().build();
	}

	/**
	 * 检索物品目录，供后台选品。
	 * Searches the item catalog for the admin item picker.
	 *
	 * @param keyword  关键字（中文名 / 英文名 / 物品 ID）/ keyword (Chinese name / English name / item id)
	 * @param page     页码，从 0 开始 / zero-based page index
	 * @param pageSize 每页条数 / page size
	 * @return 分页结果 / paged result
	 */
	@GetMapping("/catalog")
	public PageResult<ShopItemCatalogPo> catalog(@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int pageSize) {
		int safePage = PageResult.normalisePage(page);
		int safeSize = PageResult.normalisePageSize(pageSize);
		List<ShopItemCatalogPo> items = catalogDao.searchCatalog(keyword, safePage, safeSize);
		long total = catalogDao.countCatalog(keyword);
		return new PageResult<>(items, total, safePage, safeSize);
	}

	/**
	 * 商品写入请求体。
	 * Product write request body.
	 *
	 * @param itemId        物品 ID / item id
	 * @param active        是否上架 / whether the product is listed
	 * @param price         价格，null 表示不修改 / price, null keeps the current value
	 * @param purchaseLimit 限购数量，null 表示不修改 / purchase limit, null keeps the current value
	 * @param description   商城侧描述，null 表示不修改 / shop-side description, null keeps the current value
	 */
	public record ProductRequest(int itemId, boolean active, BigDecimal price, Integer purchaseLimit,
			String description) {
	}
}
