package com.aionemu.gameserver.services.trade;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.configs.main.PricesConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.siege.Influence;

/**
 * 价格服务，按势力影响度与配置计算全局物价、税率及 NPC 买卖修正。
 * Prices service computing global prices, taxes and vendor buy/sell modifiers by influence and config.
 * <p>
 * 用于数据包：SM_PRICES、SM_TRADELIST、SM_SELL_ITEM；以及神石镶嵌、传送等服务费。
 * Used by packets SM_PRICES, SM_TRADELIST, SM_SELL_ITEM and service fees (godstone socket, teleporter, etc.).
 *
 * @author Sarynth
 * @author wakizashi
 */
public class PricesService {

	/**
	 * 获取全局物价指数（用于 SM_PRICES），受攻城势力影响度调节。
	 * Get global price index (for SM_PRICES), adjusted by siege influence.
	 *
	 * @param playerRace 玩家种族 / player race
	 * @return 全局物价指数 / global buying price index
	 */
	public static final int getGlobalPrices(Race playerRace) {
		int defaultPrices = PricesConfig.DEFAULT_PRICES;

		if (!SiegeConfig.SIEGE_ENABLED) {
			return defaultPrices;
		}
		float influenceValue = 0;
		switch (playerRace) {
		case ASMODIANS:
			influenceValue = GameRuntimeServices.influence().getGlobalAsmodiansInfluence();
			break;
		case ELYOS:
			influenceValue = GameRuntimeServices.influence().getGlobalElyosInfluence();
			break;
		default:
			influenceValue = 0.5f;
			break;
		}
		if (influenceValue == 0.5f) {
			return defaultPrices;
		} else if (influenceValue > 0.5f) {
			float diff = influenceValue - 0.5f;
			return Math.round(defaultPrices - ((diff / 2) * 100));
		} else {
			float diff = 0.5f - influenceValue;
			return Math.round(defaultPrices + ((diff / 2) * 100));
		}
	}

	/**
	 * 获取全局物价修正系数（用于 SM_PRICES）。
	 * Get global prices modifier (for SM_PRICES).
	 *
	 * @return 修正系数值 / modifier value
	 */
	public static final int getGlobalPricesModifier() {
		return PricesConfig.DEFAULT_MODIFIER;
	}

	/**
	 * 获取税率（用于 SM_PRICES），弱势势力税率升高。
	 * Get tax rate (for SM_PRICES); weaker influence increases tax.
	 *
	 * @param playerRace 玩家种族 / player race
	 * @return 税率值 / tax value
	 */
	public static final int getTaxes(Race playerRace) {
		int defaultTax = PricesConfig.DEFAULT_TAXES;

		if (!SiegeConfig.SIEGE_ENABLED) {
			return defaultTax;
		}
		float influenceValue = 0;
		switch (playerRace) {
		case ASMODIANS:
			influenceValue = GameRuntimeServices.influence().getGlobalAsmodiansInfluence();
			break;
		case ELYOS:
			influenceValue = GameRuntimeServices.influence().getGlobalElyosInfluence();
			break;
		default:
			influenceValue = 0.5f;
			break;
		}
		if (influenceValue >= 0.5f) {
			return defaultTax;
		}
		float diff = 0.5f - influenceValue;
		return Math.round(defaultTax + ((diff / 4) * 100));
	}

	/**
	 * 获取 NPC 购买修正系数（用于 SM_TRADELIST）。
	 * Get vendor buy modifier (for SM_TRADELIST).
	 *
	 * @return 购买修正系数 / buy price modifier
	 */
	public static final int getVendorBuyModifier() {
		return PricesConfig.VENDOR_BUY_MODIFIER;
	}

	/**
	 * 获取 NPC 出售修正系数（用于 SM_SELL_ITEM，可按种族不同）。
	 * Get vendor sell modifier (for SM_SELL_ITEM; may differ by race).
	 *
	 * @param playerRace 玩家种族 / player race
	 * @return 出售修正系数 / selling modifier
	 */
	public static final int getVendorSellModifier(Race playerRace) {
		return (int) ((int) ((int) (PricesConfig.VENDOR_SELL_MODIFIER * getGlobalPrices(playerRace) / 100F)
				* getGlobalPricesModifier() / 100F) * getTaxes(playerRace) / 100F);
	}

	/**
	 * 按全局物价、修正与税率计算服务费用。
	 * Compute service fee from base price with global prices, modifier and taxes.
	 * <p>
	 * 需依次乘以 Prices、Modifier、Taxes，并每次向下取整以匹配客户端计算。
	 * Requires multiplication by Prices, Modifier, Taxes in order, rounding down each step to match the client.
	 *
	 * @param basePrice 基础价格 / base price
	 * @param playerRace 玩家种族 / player race
	 * @return 修正后价格 / modified price
	 */
	public static final long getPriceForService(long basePrice, Race playerRace) {
		// 较复杂。需乘以价格、修正与税收 / Tricky. Requires multiplication by Prices, Modifier, Taxes
		// 按顺序并每次向下取整以匹配客户端计算。 / In order, and round down each time to match client calculation.
		return (long) ((long) ((long) (basePrice * getGlobalPrices(playerRace) / 100D) * getGlobalPricesModifier()
				/ 100D) * getTaxes(playerRace) / 100D);
	}

	/**
	 * 计算玩家向 NPC 购买时所需基纳（含购买修正与物价/税率）。
	 * Compute kinah required when buying from a vendor (includes buy modifier, prices and taxes).
	 *
	 * @param requiredKinah 物品基础基纳 / required base kinah
	 * @param playerRace 玩家种族 / player race
	 * @return 修正后所需基纳 / modified required kinah
	 */
	public static final long getKinahForBuy(long requiredKinah, Race playerRace) {
		// 200 万以上基纳物品需要双精度。 / Requires double precision for 2mil+ kinah items.
		return (long) ((long) ((long) ((long) (requiredKinah * getVendorBuyModifier() / 100.0D)
				* getGlobalPrices(playerRace) / 100.0D) * getGlobalPricesModifier() / 100.0D) * getTaxes(playerRace)
				/ 100.0D);
	}

	/**
	 * 计算向 NPC 出售时获得的基纳。
	 * Compute kinah gained when selling to a vendor.
	 *
	 * @param kinahReward 基础基纳奖励 / base kinah reward
	 * @param playerRace 玩家种族 / player race
	 * @return 修正后基纳 / modified kinah reward
	 */
	public static final long getKinahForSell(long kinahReward, Race playerRace) {
		return (long) (kinahReward * getVendorSellModifier(playerRace) / 100D);
	}
}
