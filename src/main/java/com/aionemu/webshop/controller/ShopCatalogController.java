package com.aionemu.webshop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aionemu.webshop.config.WebShopProperties;
import com.aionemu.webshop.model.ShopProductView;
import com.aionemu.webshop.persistence.dao.ShopCatalogDao;
import com.aionemu.webshop.persistence.entity.ShopCategoryPo;

/**
 * 商城浏览接口（公开，无需登录）：分类树、商品列表与单件商品。
 * Shop browsing endpoints (public, no sign-in required): the category tree, the product list and a single product.
 */
@RestController
@RequestMapping("/api/shop")
public class ShopCatalogController {

	private final ShopCatalogDao catalogDao;

	private final WebShopProperties properties;

	public ShopCatalogController(ShopCatalogDao catalogDao, WebShopProperties properties) {
		this.catalogDao = catalogDao;
		this.properties = properties;
	}

	/**
	 * 返回全部分类（前端据此建树）。
	 * Returns all categories so the front end can build the tree.
	 *
	 * @return 分类列表 / categories
	 */
	@GetMapping("/categories")
	public List<ShopCategoryPo> categories() {
		return catalogDao.listCategories();
	}

	/**
	 * 分页检索上架商品。
	 * Pages the listed products.
	 *
	 * @param keyword  关键字（中文名 / 英文名 / 物品 ID）/ keyword (Chinese name / English name / item id)
	 * @param category 分类路径前缀 / category path prefix
	 * @param page     页码，从 0 开始 / zero-based page index
	 * @param pageSize 每页条数 / page size
	 * @return 分页结果 / paged result
	 */
	@GetMapping("/products")
	public PageResult<ShopProductView> products(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "24") int pageSize) {
		int safePage = PageResult.normalisePage(page);
		int safeSize = PageResult.normalisePageSize(pageSize);
		List<ShopProductView> items = catalogDao.searchProducts(keyword, category, true, safePage, safeSize);
		long total = catalogDao.countProducts(keyword, category, true);
		return new PageResult<>(items, total, safePage, safeSize);
	}

	/**
	 * 按物品 ID 读取单件商品。
	 * Reads a single product by item id.
	 *
	 * @param itemId 物品 ID / item id
	 * @return 商品视图，不存在时 404 / the product view, or 404 when absent
	 */
	@GetMapping("/items/{itemId}")
	public ResponseEntity<ShopProductView> item(@PathVariable int itemId) {
		ShopProductView product = catalogDao.findProductByItemId(itemId);
		return product == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(product);
	}

	/**
	 * 返回图标 URL 前缀，供前端拼接图标地址（与后端资源映射保持单一来源）。
	 * Returns the icon URL prefix so the front end can assemble icon URLs from a single source shared with
	 * the back-end resource mapping.
	 *
	 * @return 图标 URL 前缀 / icon URL prefix
	 */
	@GetMapping("/config")
	public java.util.Map<String, String> config() {
		return java.util.Map.of("iconUrlPrefix", properties.getIconUrlPrefix());
	}
}
