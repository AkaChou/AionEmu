package com.aionemu.webshop.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.webshop.auth.WebShopSession;
import com.aionemu.webshop.model.ShopProductView;
import com.aionemu.webshop.persistence.dao.ShopCatalogDao;
import com.aionemu.webshop.persistence.dao.ShopCharacterDao;
import com.aionemu.webshop.persistence.dao.ShopOrderDao;
import com.aionemu.webshop.persistence.entity.ShopOrderItemPo;
import com.aionemu.webshop.persistence.entity.ShopOrderPo;

/**
 * 下单服务：校验并落单，最后交给既有的发货队列。
 * Order service: validates, records the order and hands it to the existing delivery queue.
 * <p>本轮不结算任何货币（价格仅作为展示字段记录下来），发货由写入 {@code web_reward} 触发。
 * No currency is settled in this round (the price is only recorded as a display field); delivery is
 * triggered by writing to {@code web_reward}.</p>
 */
@Service
public class ShopOrderService {

	/** 单次下单的数量上限，防止误操作把整页库存打空。 / per-order quantity cap, guarding against accidental bulk buys. */
	private static final int MAX_QUANTITY = 99;

	private final ShopCatalogDao catalogDao;

	private final ShopOrderDao orderDao;

	private final ShopCharacterDao characterDao;

	public ShopOrderService(ShopCatalogDao catalogDao, ShopOrderDao orderDao, ShopCharacterDao characterDao) {
		this.catalogDao = catalogDao;
		this.orderDao = orderDao;
		this.characterDao = characterDao;
	}

	/**
	 * 下单并把物品排入发货队列。
	 * Places an order and queues the item for delivery.
	 *
	 * @param session   当前登录会话 / current sign-in session
	 * @param playerId  收货角色 ID / receiving character id
	 * @param productId 商品 ID / product id
	 * @param quantity  数量 / quantity
	 * @return 下单结果（成功时含订单 ID）/ the result, carrying the order id on success
	 */
	public PurchaseResult purchase(WebShopSession session, int playerId, int productId, int quantity) {
		if (quantity <= 0 || quantity > MAX_QUANTITY) {
			return PurchaseResult.failure(I18n.get("webshop.order.invalidQuantity", MAX_QUANTITY));
		}
		ShopProductView product = findActiveProduct(productId);
		if (product == null) {
			return PurchaseResult.failure(I18n.get("webshop.order.productUnavailable"));
		}
		if (!ownsCharacter(session.accountName(), playerId)) {
			return PurchaseResult.failure(I18n.get("webshop.order.invalidCharacter"));
		}
		int limit = product.getPurchaseLimit() == null ? 0 : product.getPurchaseLimit();
		if (limit > 0 && orderDao.purchasedQuantity(playerId, product.getItemId()) + quantity > limit) {
			return PurchaseResult.failure(I18n.get("webshop.order.limitReached", limit));
		}

		BigDecimal unitPrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
		ShopOrderPo order = new ShopOrderPo();
		order.setAccountId(session.accountId());
		order.setAccountName(session.accountName());
		order.setPlayerId(playerId);
		order.setPlayerName(characterName(session.accountName(), playerId));
		order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));

		ShopOrderItemPo line = new ShopOrderItemPo();
		line.setProductId(productId);
		line.setItemId(product.getItemId());
		line.setQuantity(quantity);
		line.setUnitPrice(unitPrice);

		if (!orderDao.placeOrder(order, line)) {
			return PurchaseResult.failure(I18n.get("webshop.order.failed"));
		}
		return PurchaseResult.success(order.getId());
	}

	/**
	 * 读取上架中的商品；下架或不存在的商品一律不可购买。
	 * Reads a listed product; a delisted or missing product is never purchasable.
	 */
	private ShopProductView findActiveProduct(int productId) {
		ShopProductView product = catalogDao.findProductById(productId);
		if (product == null || !Boolean.TRUE.equals(product.getActive())) {
			return null;
		}
		return product;
	}

	/**
	 * 校验角色属于该账号，避免把物品发给别人的角色。
	 * Verifies the character belongs to the account so items are never delivered to someone else's character.
	 */
	private boolean ownsCharacter(String accountName, int playerId) {
		return characterDao.isOwnedBy(accountName, playerId);
	}

	private String characterName(String accountName, int playerId) {
		return characterDao.findName(accountName, playerId);
	}

	/**
	 * 下单结果。
	 * Purchase result.
	 *
	 * @param ok      是否成功 / whether the purchase succeeded
	 * @param orderId 成功时的订单 ID / order id on success
	 * @param message 失败时的本地化原因 / localised reason on failure
	 */
	public record PurchaseResult(boolean ok, Integer orderId, String message) {

		static PurchaseResult success(int orderId) {
			return new PurchaseResult(true, orderId, null);
		}

		static PurchaseResult failure(String message) {
			return new PurchaseResult(false, null, message);
		}
	}
}
