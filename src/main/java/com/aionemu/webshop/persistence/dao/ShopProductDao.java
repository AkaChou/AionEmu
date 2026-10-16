package com.aionemu.webshop.persistence.dao;

import java.math.BigDecimal;

import com.aionemu.webshop.persistence.entity.ShopProductPo;
import com.mybatisflex.core.service.IService;

/**
 * 商品写操作数据访问层（后台上下架与编辑）。
 * Product write data access (admin listing and editing).
 * <p>物品名称、图标与分类都来自目录表，此处只维护商城侧的定价、限购与上架状态；商品不存在时按物品 ID 创建。
 * Item names, icons and categories all come from the catalog; this layer only owns the shop-side price,
 * purchase limit and listing state, creating the product from the item id when it does not exist yet.</p>
 */
public interface ShopProductDao extends IService<ShopProductPo> {

	/**
	 * 上架或更新一个商品（按物品 ID 定位）。
	 * Lists or updates a product, located by item id.
	 *
	 * @param itemId        物品 ID / item id
	 * @param active        是否上架 / whether the product is listed
	 * @param price         价格（null 表示保持原值）/ price (null keeps the current value)
	 * @param purchaseLimit 限购数量（null 表示保持原值）/ purchase limit (null keeps the current value)
	 * @param description   商城侧描述（null 表示保持原值）/ shop-side description (null keeps the current value)
	 * @return 写入成功返回 true / true when the write succeeded
	 */
	boolean upsert(int itemId, boolean active, BigDecimal price, Integer purchaseLimit, String description);
}
