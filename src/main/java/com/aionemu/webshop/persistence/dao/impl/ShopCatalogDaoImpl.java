package com.aionemu.webshop.persistence.dao.impl;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.webshop.model.ShopProductView;
import com.aionemu.webshop.persistence.dao.ShopCatalogDao;
import com.aionemu.webshop.persistence.entity.ShopCategoryPo;
import com.aionemu.webshop.persistence.entity.ShopItemCatalogPo;
import com.aionemu.webshop.persistence.mapper.ShopCategoryMapper;
import com.aionemu.webshop.persistence.mapper.ShopItemCatalogMapper;
import com.mybatisflex.core.query.QueryTable;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 商城目录数据访问实现。
 * Shop catalog data access implementation.
 */
@Slf4j
@Service
@Lazy
public class ShopCatalogDaoImpl extends ServiceImpl<ShopItemCatalogMapper, ShopItemCatalogPo>
	implements ShopCatalogDao {

	/** shop_product 连接别名。 / join alias of shop_product. */
	private static final QueryTable PRODUCT = new QueryTable("shop_product").as("p");

	/** shop_item_catalog 连接别名。 / join alias of shop_item_catalog. */
	private static final QueryTable CATALOG = new QueryTable("shop_item_catalog").as("c");

	/**
	 * LIKE 的转义符，与 SQL 中的 {@code ESCAPE '\'} 子句一致。
	 * The LIKE escape character, matching the {@code ESCAPE '\'} clause in the SQL.
	 */
	private static final String LIKE_ESCAPE = "\\";

	/**
	 * 商品视图列清单；别名逐字对齐 {@link ShopProductView} 的字段名，避免依赖驼峰自动映射配置。
	 * Column list of the product view; aliases match {@link ShopProductView} field names verbatim so no
	 * camel-case auto-mapping configuration is required.
	 */
	private static final String[] PRODUCT_COLUMNS = {
		"p.id AS productId",
		"p.item_id AS itemId",
		"c.name AS name",
		"c.name_zh AS nameZh",
		"c.item_level AS itemLevel",
		"c.category_path AS categoryPath",
		"c.icon_file AS iconFile",
		"p.price AS price",
		"p.purchase_limit AS purchaseLimit",
		"p.active AS active",
		"p.description AS description",
		"c.gender_limit AS genderLimit",
		"c.race_limit AS raceLimit",
		"c.class_limit AS classLimit"
	};

	private final ShopCategoryMapper categoryMapper;

	public ShopCatalogDaoImpl(ShopCategoryMapper categoryMapper) {
		this.categoryMapper = categoryMapper;
	}

	@Override
	public List<ShopCategoryPo> listCategories() {
		try {
			return categoryMapper.selectListByQuery(QueryWrapper.create()
				.orderBy("sort", true)
				.orderBy("path", true));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a1c4d7e30b21"), e);
			return List.of();
		}
	}

	@Override
	public List<ShopItemCatalogPo> searchCatalog(String keyword, int page, int pageSize) {
		try {
			return mapper.selectListByQuery(catalogQuery(keyword)
				.orderBy("item_id", true)
				.limit(pageSize)
				.offset((long) page * pageSize));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b2d5e8f41c32", keyword, page), e);
			return List.of();
		}
	}

	@Override
	public long countCatalog(String keyword) {
		try {
			return mapper.selectCountByQuery(catalogQuery(keyword).select("COUNT(*)"));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c3e6f9052d43", keyword, 0), e);
			return 0L;
		}
	}

	/**
	 * 构造目录检索条件（不含列清单与分页），供列表与计数共用。
	 * Builds the catalog filters (without column list and pagination) so listing and counting share them.
	 */
	private QueryWrapper catalogQuery(String keyword) {
		QueryWrapper query = QueryWrapper.create();
		String trimmed = trimToNull(keyword);
		if (trimmed != null) {
			String pattern = "%" + escapeLike(trimmed) + "%";
			query.and("(name_zh LIKE ? ESCAPE '" + LIKE_ESCAPE + LIKE_ESCAPE
				+ "' OR name LIKE ? ESCAPE '" + LIKE_ESCAPE + LIKE_ESCAPE
				+ "' OR CAST(item_id AS CHAR) = ?)", pattern, pattern, trimmed);
		}
		return query;
	}

	@Override
	public List<ShopProductView> searchProducts(String keyword, String category, boolean activeOnly, int page,
			int pageSize) {
		try {
			QueryWrapper query = productQuery(keyword, category, activeOnly)
				.select(PRODUCT_COLUMNS)
				.orderBy("p.sort", true)
				.orderBy("p.id", true)
				.limit(pageSize)
				.offset((long) page * pageSize);
			return mapper.selectListByQueryAs(query, ShopProductView.class);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c3e6f9052d43", keyword, category, page), e);
			return List.of();
		}
	}

	@Override
	public long countProducts(String keyword, String category, boolean activeOnly) {
		try {
			return mapper.selectCountByQuery(productQuery(keyword, category, activeOnly).select("COUNT(*)"));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d4f70a163e54", keyword, category), e);
			return 0L;
		}
	}

	@Override
	public ShopProductView findProductByItemId(int itemId) {
		try {
			QueryWrapper query = productQuery(null, null, false)
				.and("p.item_id = ?", itemId)
				.select(PRODUCT_COLUMNS)
				.limit(1);
			return mapper.selectOneByQueryAs(query, ShopProductView.class);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e5081b274f65", itemId), e);
			return null;
		}
	}

	@Override
	public ShopProductView findProductById(int productId) {
		try {
			QueryWrapper query = productQuery(null, null, false)
				.and("p.id = ?", productId)
				.select(PRODUCT_COLUMNS)
				.limit(1);
			return mapper.selectOneByQueryAs(query, ShopProductView.class);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f6192c385a76", productId), e);
			return null;
		}
	}

	/**
	 * 构造商品检索条件（不含列清单与分页），供列表与计数共用。
	 * Builds the product filters (without column list and pagination) so listing and counting share them.
	 */
	private QueryWrapper productQuery(String keyword, String category, boolean activeOnly) {
		QueryWrapper query = QueryWrapper.create()
			.from(PRODUCT)
			.leftJoin(CATALOG).on("p.item_id = c.item_id");
		if (activeOnly) {
			query.and("p.active = 1");
		}
		String trimmedCategory = trimToNull(category);
		if (trimmedCategory != null) {
			query.and("(c.category_path = ? OR c.category_path LIKE ? ESCAPE '" + LIKE_ESCAPE + LIKE_ESCAPE + "')",
				trimmedCategory, escapeLike(trimmedCategory) + "/%");
		}
		String trimmedKeyword = trimToNull(keyword);
		if (trimmedKeyword != null) {
			String pattern = "%" + escapeLike(trimmedKeyword) + "%";
			query.and("(c.name_zh LIKE ? ESCAPE '" + LIKE_ESCAPE + LIKE_ESCAPE
				+ "' OR c.name LIKE ? ESCAPE '" + LIKE_ESCAPE + LIKE_ESCAPE
				+ "' OR CAST(p.item_id AS CHAR) = ?)", pattern, pattern, trimmedKeyword);
		}
		return query;
	}

	/**
	 * 转义 LIKE 的通配符，避免用户输入被当作模式。
	 * Escapes LIKE wildcards so user input is never interpreted as a pattern.
	 */
	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	/**
	 * 去除空白并归一空串为 null。
	 * Trims whitespace and normalises blank strings to null.
	 */
	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
