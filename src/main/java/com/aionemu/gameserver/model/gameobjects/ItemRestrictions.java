package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemMask;

/**
 * 物品限制判定策略（无分配静态策略）。
 * Allocation-free static policy for item restriction checks.
 * <p>该类型只服务 {@link Item}：集中物品掩码（mask）叠加会员权限解锁、仓库/交易/改造/出售、
 * 灵魂绑定、AP 提取、伊迪安、神石、注能与高阶守护者等限制判定。全部为静态纯函数，不持有状态、
 * 不创建辅助对象；对外仍通过 {@link Item} 的原公开方法访问（门面签名不变）。
 * This type only serves {@link Item}: it centralizes the item mask combined with membership
 * permission unlocks, warehouse/trade/remodel/sell checks, soul-bind, AP extract, idian, godstone,
 * amplification and archdaeva checks. Every check is a static pure function holding no state and
 * allocating no helper object; external callers keep using the original {@link Item} facade methods.</p>
 */
final class ItemRestrictions {

	/**
	 * 仅静态策略，禁止实例化。
	 * Static policy only; not instantiable.
	 */
	private ItemRestrictions() {
	}

	/**
	 * 返回叠加会员权限解锁位后的有效掩码。
	 * Returns the effective mask with membership permission unlock bits applied.
	 * @param item 物品 / item
	 * @param player 玩家 / player
	 * @return 有效掩码 / effective mask
	 */
	static int effectiveMask(Item item, Player player) {
		return withMembershipUnlocks(player, item.getItemTemplate().getMask());
	}

	/**
	 * 对该玩家而言物品是否视为已灵魂绑定（指定会员权限可跳过绑定）。
	 * Whether the item counts as soul bound for the given player (a membership permission can skip binding).
	 * @param item 物品 / item
	 * @param player 玩家 / player
	 * @return 对该玩家是否已绑定 / whether soul bound for the player
	 */
	static boolean isSoulBoundFor(Item item, Player player) {
		if (player.havePermission(MembershipConfig.DISABLE_SOULBIND)) {
			return false;
		}
		return item.isSoulBound();
	}

	/**
	 * 是否可存入个人仓库。
	 * Whether the item is storable in the regular warehouse.
	 * @param item 物品 / item
	 * @param player 玩家 / player
	 * @return 是否可存入 / whether storable
	 */
	static boolean isStorableInWarehouse(Item item, Player player) {
		return hasMaskBit(effectiveMask(item, player), ItemMask.STORABLE_IN_WH) && !isSoulBoundFor(item, player);
	}

	/**
	 * 是否可存入账号仓库。
	 * Whether the item is storable in the account warehouse.
	 * @param item 物品 / item
	 * @param player 玩家 / player
	 * @return 是否可存入 / whether storable
	 */
	static boolean isStorableInAccWarehouse(Item item, Player player) {
		return hasMaskBit(effectiveMask(item, player), ItemMask.STORABLE_IN_AWH) && !isSoulBoundFor(item, player);
	}

	/**
	 * 是否可存入军团仓库。
	 * Whether the item is storable in the legion warehouse.
	 * @param item 物品 / item
	 * @param player 玩家 / player
	 * @return 是否可存入 / whether storable
	 */
	static boolean isStorableInLegWarehouse(Item item, Player player) {
		return hasMaskBit(effectiveMask(item, player), ItemMask.STORABLE_IN_LWH) && !isSoulBoundFor(item, player);
	}

	/**
	 * 是否可交易。
	 * Whether the item is tradeable.
	 * @param item 物品 / item
	 * @param player 玩家 / player
	 * @return 是否可交易 / whether tradeable
	 */
	static boolean isTradeable(Item item, Player player) {
		return hasMaskBit(effectiveMask(item, player), ItemMask.TRADEABLE) && !isSoulBoundFor(item, player);
	}

	/**
	 * 是否可改造外观。
	 * Whether the item is remodelable.
	 * @param item 物品 / item
	 * @param player 玩家 / player
	 * @return 是否可改造 / whether remodelable
	 */
	static boolean isRemodelable(Item item, Player player) {
		return hasMaskBit(effectiveMask(item, player), ItemMask.REMODELABLE);
	}

	/**
	 * 是否可出售。
	 * Whether the item is sellable.
	 * @param item 物品 / item
	 * @return 是否可出售 / whether sellable
	 */
	static boolean isSellable(Item item) {
		return hasMaskBit(item.getItemMask(), ItemMask.SELLABLE);
	}

	/**
	 * 是否可提取欧比斯点数。
	 * Whether AP can be extracted from the item.
	 * @param item 物品 / item
	 * @return 是否可提取 AP / whether AP extract
	 */
	static boolean canApExtract(Item item) {
		return hasMaskBit(item.getItemMask(), ItemMask.CAN_AP_EXTRACT);
	}

	/**
	 * 是否可镶嵌伊迪安石。
	 * Whether an idian stone can be applied.
	 * @param item 物品 / item
	 * @return 是否可镶嵌伊迪安 / whether idian applicable
	 */
	static boolean canIdian(Item item) {
		return hasMaskBit(item.getItemMask(), ItemMask.CAN_IDIAN);
	}

	/**
	 * 是否可以镶嵌神石。
	 * Whether a godstone can be socketed.
	 * @param item 物品 / item
	 * @return 是否可镶嵌 / whether socket godstone
	 */
	static boolean canSocketGodstone(Item item) {
		return hasMaskBit(item.getItemMask(), ItemMask.CAN_PROC_ENCHANT);
	}

	/**
	 * 是否允许注能。
	 * Whether the item supports amplification.
	 * @param item 物品 / item
	 * @return 是否允许注能 / whether amplifiable
	 */
	static boolean canAmplification(Item item) {
		return hasMaskBit(item.getItemMask(), ItemMask.CAN_AMPLIFICATION);
	}

	/**
	 * 是否为高阶守护者物品。
	 * Whether the item is an archdaeva item.
	 * @param item 物品 / item
	 * @return 是否为高阶守护者物品 / whether archdaeva item
	 */
	static boolean isArchDaevaItem(Item item) {
		return hasMaskBit(item.getItemMask(), ItemMask.ITEM_ARCHDAEVA);
	}

	/**
	 * 按会员配置展开存储/交易/改造解锁位。
	 * Expands the storage/trade/remodel unlock bits according to membership configuration.
	 * @param player 玩家 / player
	 * @param mask 原始掩码 / raw mask
	 * @return 叠加解锁位后的掩码 / mask with unlock bits applied
	 */
	private static int withMembershipUnlocks(Player player, int mask) {
		int newMask = mask;
		if (player.havePermission(MembershipConfig.STORE_WH_ALL)) {
			newMask = newMask | ItemMask.STORABLE_IN_WH;
		}
		if (player.havePermission(MembershipConfig.STORE_AWH_ALL)) {
			newMask = newMask | ItemMask.STORABLE_IN_AWH;
		}
		if (player.havePermission(MembershipConfig.STORE_LWH_ALL)) {
			newMask = newMask | ItemMask.STORABLE_IN_LWH;
		}
		if (player.havePermission(MembershipConfig.TRADE_ALL)) {
			newMask = newMask | ItemMask.TRADEABLE;
		}
		if (player.havePermission(MembershipConfig.REMODEL_ALL)) {
			newMask = newMask | ItemMask.REMODELABLE;
		}
		return newMask;
	}

	/**
	 * 检查掩码是否包含指定标志位。
	 * Checks whether the mask contains the given flag bit.
	 * @param mask 掩码 / mask
	 * @param flag 标志位 / flag bit
	 * @return 是否包含 / whether present
	 */
	private static boolean hasMaskBit(int mask, int flag) {
		return (mask & flag) == flag;
	}
}
