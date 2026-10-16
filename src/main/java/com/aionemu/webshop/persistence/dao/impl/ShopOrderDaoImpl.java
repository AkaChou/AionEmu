package com.aionemu.webshop.persistence.dao.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.persistence.dao.WebRewardDao;
import com.aionemu.gameserver.persistence.entity.WebRewardPo;
import com.aionemu.webshop.model.ShopOrderLine;
import com.aionemu.webshop.model.ShopOrderView;
import com.aionemu.webshop.persistence.dao.ShopOrderDao;
import com.aionemu.webshop.persistence.entity.ShopItemCatalogPo;
import com.aionemu.webshop.persistence.entity.ShopOrderItemPo;
import com.aionemu.webshop.persistence.entity.ShopOrderPo;
import com.aionemu.webshop.persistence.mapper.ShopItemCatalogMapper;
import com.aionemu.webshop.persistence.mapper.ShopOrderItemMapper;
import com.aionemu.webshop.persistence.mapper.ShopOrderMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;

import lombok.extern.slf4j.Slf4j;

/**
 * 商城订单数据访问实现。
 * Shop order data access implementation.
 */
@Slf4j
@Service
@Lazy
public class ShopOrderDaoImpl implements ShopOrderDao {

	private final ShopOrderMapper orderMapper;

	private final ShopOrderItemMapper orderItemMapper;

	private final ShopItemCatalogMapper catalogMapper;

	/** 既有发货队列 DAO：写入 web_reward 即完成发货请求，不另开发货逻辑。 / existing delivery-queue DAO. */
	private final WebRewardDao webRewardDao;

	public ShopOrderDaoImpl(ShopOrderMapper orderMapper, ShopOrderItemMapper orderItemMapper,
			ShopItemCatalogMapper catalogMapper, WebRewardDao webRewardDao) {
		this.orderMapper = orderMapper;
		this.orderItemMapper = orderItemMapper;
		this.catalogMapper = catalogMapper;
		this.webRewardDao = webRewardDao;
	}

	/**
	 * 下单：订单头、订单行与发货队列在同一个事务内落库，避免出现「有订单无发货」。
	 * Places an order: header, line and delivery-queue row land in one transaction, so an order can never
	 * exist without its delivery.
	 */
	@Override
	@Transactional
	public boolean placeOrder(ShopOrderPo order, ShopOrderItemPo line) {
		orderMapper.insert(order);
		if (order.getId() == null) {
			log.error(I18n.get("log.f6192c385a76", order.getPlayerId()));
			return false;
		}
		line.setOrderId(order.getId());
		orderItemMapper.insert(line);

		WebRewardPo reward = new WebRewardPo();
		reward.setItemOwner(order.getPlayerId());
		reward.setItemId(line.getItemId());
		reward.setItemCount(line.getQuantity().longValue());
		webRewardDao.save(reward);
		return true;
	}

	@Override
	public List<ShopOrderView> listByAccount(int accountId, int page, int pageSize) {
		QueryWrapper query = QueryWrapper.create()
			.where("account_id = ?", accountId)
			.orderBy("created_at", false)
			.orderBy("id", false)
			.limit(pageSize)
			.offset((long) page * pageSize);
		return decorate(selectOrders(query));
	}

	@Override
	public long countByAccount(int accountId) {
		try {
			return orderMapper.selectCountByQuery(QueryWrapper.create().where("account_id = ?", accountId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a72b4d9e6c08", accountId), e);
			return 0L;
		}
	}

	@Override
	public List<ShopOrderView> listAll(String keyword, int page, int pageSize) {
		QueryWrapper query = QueryWrapper.create();
		String trimmed = trimToNull(keyword);
		if (trimmed != null) {
			String pattern = "%" + escapeLike(trimmed) + "%";
			query.and("(account_name LIKE ? ESCAPE '\\\\' OR player_name LIKE ? ESCAPE '\\\\')", pattern, pattern);
		}
		query.orderBy("created_at", false).orderBy("id", false)
			.limit(pageSize).offset((long) page * pageSize);
		return decorate(selectOrders(query));
	}

	@Override
	public long countAll(String keyword) {
		try {
			QueryWrapper query = QueryWrapper.create();
			String trimmed = trimToNull(keyword);
			if (trimmed != null) {
				String pattern = "%" + escapeLike(trimmed) + "%";
				query.and("(account_name LIKE ? ESCAPE '\\\\' OR player_name LIKE ? ESCAPE '\\\\')", pattern, pattern);
			}
			return orderMapper.selectCountByQuery(query);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b83c5e0f7d19", keyword), e);
			return 0L;
		}
	}

	@Override
	public long purchasedQuantity(int playerId, int itemId) {
		return orderItemMapper.sumPurchasedQuantity(playerId, itemId);
	}

	@Override
	public boolean requeueDelivery(int rewardUnique) {
		try {
			return UpdateChain.of(WebRewardPo.class)
				.set(WebRewardPo::getRewarded, 0)
				.where(WebRewardPo::getUnique).eq(rewardUnique)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d05e7a218f3b", rewardUnique), e);
			return false;
		}
	}

	/**
	 * 执行订单查询；失败时返回空列表而不向上抛，避免单条坏数据拖垮整个列表页。
	 * Runs the order query; returns an empty list on failure instead of propagating, so one bad row cannot
	 * break a whole list page.
	 */
	private List<ShopOrderPo> selectOrders(QueryWrapper query) {
		try {
			return orderMapper.selectListByQuery(query);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c94d6f107e2a"), e);
			return List.of();
		}
	}

	/**
	 * 为订单补齐订单行、物品名与发货状态。
	 * Decorates orders with their lines, item names and delivery state.
	 */
	private List<ShopOrderView> decorate(List<ShopOrderPo> orders) {
		if (orders.isEmpty()) {
			return List.of();
		}
		List<Integer> orderIds = orders.stream().map(ShopOrderPo::getId).toList();
		List<Integer> playerIds = orders.stream().map(ShopOrderPo::getPlayerId).distinct().toList();

		List<ShopOrderItemPo> lines = orderItemMapper.selectListByQuery(
			QueryWrapper.create().where("order_id IN (" + placeholders(orderIds.size()) + ")",
				orderIds.toArray()));
		List<WebRewardPo> rewards = webRewardDao.list(
			QueryWrapper.create().where("item_owner IN (" + placeholders(playerIds.size()) + ")",
				playerIds.toArray()));

		Map<Integer, ShopItemCatalogPo> catalog = loadCatalog(lines);
		Map<Integer, List<ShopOrderItemPo>> linesByOrder = new LinkedHashMap<>();
		for (ShopOrderItemPo line : lines) {
			linesByOrder.computeIfAbsent(line.getOrderId(), key -> new ArrayList<>()).add(line);
		}

		List<ShopOrderView> views = new ArrayList<>(orders.size());
		for (ShopOrderPo order : orders) {
			ShopOrderView view = new ShopOrderView();
			view.setId(order.getId());
			view.setAccountId(order.getAccountId());
			view.setAccountName(order.getAccountName());
			view.setPlayerId(order.getPlayerId());
			view.setPlayerName(order.getPlayerName());
			view.setTotalPrice(order.getTotalPrice());
			view.setCreatedAt(order.getCreatedAt());

			List<ShopOrderItemPo> orderLines = linesByOrder.getOrDefault(order.getId(), List.of());
			view.setLines(orderLines.stream().map(line -> toLine(line, catalog.get(line.getItemId()))).toList());
			Set<Integer> orderedItemIds = orderLines.stream()
				.map(ShopOrderItemPo::getItemId)
				.collect(Collectors.toSet());
			view.setDeliveries(rewards.stream()
				.filter(reward -> reward.getItemOwner() != null
					&& reward.getItemOwner() == order.getPlayerId()
					&& orderedItemIds.contains(reward.getItemId()))
				.map(ShopOrderDaoImpl::toDeliveryState)
				.toList());
			views.add(view);
		}
		return views;
	}

	/**
	 * 批量读取订单行涉及的目录条目。
	 * Batch-loads the catalog entries referenced by the order lines.
	 */
	private Map<Integer, ShopItemCatalogPo> loadCatalog(List<ShopOrderItemPo> lines) {
		if (lines.isEmpty()) {
			return Map.of();
		}
		List<Integer> itemIds = lines.stream().map(ShopOrderItemPo::getItemId).distinct().toList();
		Map<Integer, ShopItemCatalogPo> catalog = new HashMap<>();
		for (ShopItemCatalogPo po : catalogMapper.selectListByQuery(
			QueryWrapper.create().where("item_id IN (" + placeholders(itemIds.size()) + ")", itemIds.toArray()))) {
			catalog.put(po.getItemId(), po);
		}
		return catalog;
	}

	private static ShopOrderLine toLine(ShopOrderItemPo po, ShopItemCatalogPo catalog) {
		ShopOrderLine line = new ShopOrderLine();
		line.setId(po.getId());
		line.setOrderId(po.getOrderId());
		line.setProductId(po.getProductId());
		line.setItemId(po.getItemId());
		line.setQuantity(po.getQuantity());
		line.setUnitPrice(po.getUnitPrice());
		if (catalog != null) {
			line.setNameZh(catalog.getNameZh());
			line.setName(catalog.getName());
			line.setIconFile(catalog.getIconFile());
		}
		return line;
	}

	private static ShopOrderView.DeliveryState toDeliveryState(WebRewardPo po) {
		ShopOrderView.DeliveryState state = new ShopOrderView.DeliveryState();
		state.setItemId(po.getItemId() == null ? 0 : po.getItemId());
		state.setItemCount(po.getItemCount() == null ? 0L : po.getItemCount());
		state.setAdded(po.getAdded());
		state.setReceived(po.getReceived());
		state.setRewarded(po.getRewarded());
		return state;
	}

	/**
	 * 生成 {@code ?, ?, ?} 形式的占位符，占位符个数由入参数量决定（值本身仍走参数绑定）。
	 * Emits {@code ?, ?, ?} placeholders; only the count is interpolated, the values stay bound parameters.
	 */
	private static String placeholders(int count) {
		return String.join(", ", Collections.nCopies(count, "?"));
	}

	/**
	 * 转义 LIKE 的通配符。
	 * Escapes LIKE wildcards.
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
