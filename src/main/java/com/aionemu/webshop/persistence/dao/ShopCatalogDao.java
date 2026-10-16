package com.aionemu.webshop.persistence.dao;

import java.util.List;

import com.aionemu.webshop.model.ShopProductView;
import com.aionemu.webshop.persistence.entity.ShopCategoryPo;
import com.aionemu.webshop.persistence.entity.ShopItemCatalogPo;
import com.mybatisflex.core.service.IService;

/**
 * 商城目录数据访问层：分类树、物品目录检索，以及「商品 × 目录」的连接视图。
 * Shop catalog data access: the category tree, catalog item search, and the product × catalog join view.
 */
public interface ShopCatalogDao extends IService<ShopItemCatalogPo> {

	/**
	 * 按排序读取全部分类（数量有限，一次性返回给前端建树）。
	 * Reads all categories in display order (a bounded set, returned in one shot for the client to build the tree).
	 *
	 * @return 分类列表 / categories
	 */
	List<ShopCategoryPo> listCategories();

	/**
	 * 检索物品目录（后台选品用）：关键字可命中中文名、英文名或物品 ID，均可选。
	 * Searches the item catalog (used by the admin item picker): the keyword may match the Chinese name, the
	 * English name or the item id, any of which is optional.
	 *
	 * @param keyword  关键字，null 或空表示不过滤 / keyword, null or blank means no filter
	 * @param page     页码，从 0 开始 / zero-based page index
	 * @param pageSize 每页条数 / page size
	 * @return 目录条目 / catalog entries
	 */
	List<ShopItemCatalogPo> searchCatalog(String keyword, int page, int pageSize);

	/**
	 * 统计符合条件的目录条目数量，与 {@link #searchCatalog} 使用同一套过滤条件。
	 * Counts catalog entries matching the same filter as {@link #searchCatalog}.
	 *
	 * @param keyword 关键字 / keyword
	 * @return 命中数量 / matching count
	 */
	long countCatalog(String keyword);

	/**
	 * 检索商品列表项（前台与后台共用）：连接 shop_product 与 shop_item_catalog。
	 * Searches product list items (shared by the shop front end and the admin console): joins shop_product
	 * with shop_item_catalog.
	 *
	 * @param keyword    关键字，null 或空表示不过滤 / keyword, null or blank means no filter
	 * @param category   分类路径前缀，null 或空表示不过滤 / category path prefix, null or blank means no filter
	 * @param activeOnly 仅返回上架商品 / when true, only listed products are returned
	 * @param page       页码，从 0 开始 / zero-based page index
	 * @param pageSize   每页条数 / page size
	 * @return 商品列表项 / product list items
	 */
	List<ShopProductView> searchProducts(String keyword, String category, boolean activeOnly, int page, int pageSize);

	/**
	 * 统计符合条件的商品数量，与 {@link #searchProducts} 使用同一套过滤条件。
	 * Counts products matching the same filters as {@link #searchProducts}.
	 *
	 * @param keyword    关键字 / keyword
	 * @param category   分类路径前缀 / category path prefix
	 * @param activeOnly 仅统计上架商品 / count only listed products
	 * @return 命中数量 / matching count
	 */
	long countProducts(String keyword, String category, boolean activeOnly);

	/**
	 * 按物品 ID 读取单个商品视图。
	 * Reads a single product view by item id.
	 *
	 * @param itemId 物品 ID / item id
	 * @return 商品视图，不存在时为 null / product view, or null when absent
	 */
	ShopProductView findProductByItemId(int itemId);

	/**
	 * 按商品 ID 读取单个商品视图（前台详情页与下单校验共用）。
	 * Reads a single product view by product id (shared by the detail page and the purchase check).
	 *
	 * @param productId 商品 ID / product id
	 * @return 商品视图，不存在时为 null / product view, or null when absent
	 */
	ShopProductView findProductById(int productId);
}
