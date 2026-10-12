package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.ingameshop.IGItem;
import com.aionemu.gameserver.persistence.dao.IngameshopDao;
import com.aionemu.gameserver.persistence.entity.IngameshopPo;
import com.aionemu.gameserver.persistence.mapper.IngameshopMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏内商城数据访问实现。
 * Data access implementation for in-game shop items.
 * @author xTz, KID
 */
@Slf4j
@Service
@Lazy
public class IngameshopDaoImpl extends ServiceImpl<IngameshopMapper, IngameshopPo> implements IngameshopDao {

	@Override
	public void deleteIngameShopItem(int itemId, byte category, byte list, int param) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(IngameshopPo::getItemId).eq(itemId)
				.and(IngameshopPo::getCategory).eq(category)
				.and(IngameshopPo::getSubCategory).eq(param)
				.and(IngameshopPo::getList).eq(list));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ab89a36682b8", itemId), e);
		}
	}

	@Override
	public Map<Byte, List<IGItem>> loadInGameShopItems() {
		Map<Byte, List<IGItem>> items = new LinkedHashMap<>();

		try {
			for (IngameshopPo po : mapper.selectListByQuery(QueryWrapper.create())) {
				// 子类别小于 3 的记录不参与展示。
				// Rows whose sub-category is below 3 are not exposed.
				if (po.getSubCategory() < 3) {
					continue;
				}
				items.computeIfAbsent(po.getCategory(), _ -> new ArrayList<>()).add(toIGItem(po));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e1ffb8726a87"), e);
		}
		return items;
	}

	@Override
	public void saveIngameShopItem(int objectId, int itemId, long itemCount, long itemPrice, byte category,
			byte subCategory, int list, int salesRanking, byte itemType, byte gift, String titleDescription,
			String description) {
		try {
			IngameshopPo po = new IngameshopPo();
			po.setObjectId(objectId);
			po.setItemId(itemId);
			po.setItemCount(itemCount);
			po.setItemPrice(itemPrice);
			po.setCategory(category);
			po.setSubCategory(subCategory);
			po.setList(list);
			po.setSalesRanking(salesRanking);
			po.setItemType(itemType);
			po.setGift(gift);
			po.setTitleDescription(titleDescription);
			po.setDescription(description);
			// 显式写入主键，故用 insertWithPk（insert 会丢弃 Auto 主键）。
			// The primary key is written explicitly, hence insertWithPk (insert drops Auto keys).
			mapper.insertWithPk(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.1bd07eff1e18", objectId), e);
		}
	}

	@Override
	public void increaseSales(int object, int current) {
		try {
			UpdateChain.of(IngameshopPo.class)
				.set(IngameshopPo::getSalesRanking, current)
				.where(IngameshopPo::getObjectId).eq(object)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.2b5a5b8ef0fb", object), e);
		}
	}

	/**
	 * 实体转商城物品模型。
	 * Converts the entity into the shop item model.
	 * @param po 商城物品实体 / shop item entity
	 * @return 商城物品 / shop item
	 */
	private static IGItem toIGItem(IngameshopPo po) {
		return new IGItem(po.getObjectId(), po.getItemId(), po.getItemCount(), po.getItemPrice(), po.getCategory(),
			po.getSubCategory(), po.getList(), po.getSalesRanking(), po.getItemType(), po.getGift(),
			po.getTitleDescription(), po.getDescription());
	}
}
