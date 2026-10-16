package com.aionemu.webshop.persistence.dao.impl;

import java.math.BigDecimal;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.webshop.persistence.dao.ShopProductDao;
import com.aionemu.webshop.persistence.entity.ShopProductPo;
import com.aionemu.webshop.persistence.mapper.ShopProductMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 商品写操作数据访问实现。
 * Product write data access implementation.
 */
@Slf4j
@Service
@Lazy
public class ShopProductDaoImpl extends ServiceImpl<ShopProductMapper, ShopProductPo> implements ShopProductDao {

	@Override
	public boolean upsert(int itemId, boolean active, BigDecimal price, Integer purchaseLimit, String description) {
		try {
			ShopProductPo existing = mapper.selectOneByQuery(
				QueryWrapper.create().where("item_id = ?", itemId).limit(1));
			if (existing == null) {
				ShopProductPo created = new ShopProductPo();
				created.setItemId(itemId);
				created.setActive(active);
				created.setPrice(price == null ? BigDecimal.ZERO : price);
				created.setPurchaseLimit(purchaseLimit == null ? 0 : purchaseLimit);
				created.setDescription(description == null ? "" : description);
				// 新建商品排到列表末尾，等待后台手工调序。
				// A newly created product goes to the end of the list until the admin reorders it.
				created.setSort(nextSort());
				return mapper.insert(created) > 0;
			}
			existing.setActive(active);
			if (price != null) {
				existing.setPrice(price);
			}
			if (purchaseLimit != null) {
				existing.setPurchaseLimit(purchaseLimit);
			}
			if (description != null) {
				existing.setDescription(description);
			}
			return mapper.update(existing) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d05e7a218f3b", itemId), e);
			return false;
		}
	}

	/**
	 * 取当前最大排序值加一。
	 * Returns the current maximum sort value plus one.
	 */
	private int nextSort() {
		ShopProductPo last = mapper.selectOneByQuery(QueryWrapper.create().orderBy("sort", false).limit(1));
		return last == null || last.getSort() == null ? 0 : last.getSort() + 1;
	}
}
