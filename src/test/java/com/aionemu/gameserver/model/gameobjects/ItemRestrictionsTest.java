package com.aionemu.gameserver.model.gameobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemMask;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 验证 {@link ItemRestrictions} 静态策略与 {@link Item} 门面的语义一致性：
 * 掩码判定、灵魂绑定联动以及会员权限跳过行为。
 * Verifies that the {@link ItemRestrictions} static policy and the {@link Item} facade stay
 * semantically identical: mask checks, soul-bind coupling and membership permission skips.
 */
class ItemRestrictionsTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private byte originalStoreWhAll;
	private byte originalStoreAwhAll;
	private byte originalStoreLwhAll;
	private byte originalTradeAll;
	private byte originalRemodelAll;
	private byte originalDisableSoulbind;

	/**
	 * 为权限判定设置互不相同的会员等级，避免未配置默认值 0 使全部权限同时命中。
	 * Assigns distinct membership levels so the unconfigured default 0 cannot match every permission.
	 */
	@BeforeEach
	void setUpMembershipLevels() {
		originalStoreWhAll = MembershipConfig.STORE_WH_ALL;
		originalStoreAwhAll = MembershipConfig.STORE_AWH_ALL;
		originalStoreLwhAll = MembershipConfig.STORE_LWH_ALL;
		originalTradeAll = MembershipConfig.TRADE_ALL;
		originalRemodelAll = MembershipConfig.REMODEL_ALL;
		originalDisableSoulbind = MembershipConfig.DISABLE_SOULBIND;
		MembershipConfig.STORE_WH_ALL = 5;
		MembershipConfig.STORE_AWH_ALL = 6;
		MembershipConfig.STORE_LWH_ALL = 7;
		MembershipConfig.TRADE_ALL = 8;
		MembershipConfig.REMODEL_ALL = 9;
		MembershipConfig.DISABLE_SOULBIND = 10;
	}

	/**
	 * 恢复全局配置字段，避免污染其他测试。
	 * Restores the global config fields so other tests stay unaffected.
	 */
	@AfterEach
	void restoreMembershipLevels() {
		MembershipConfig.STORE_WH_ALL = originalStoreWhAll;
		MembershipConfig.STORE_AWH_ALL = originalStoreAwhAll;
		MembershipConfig.STORE_LWH_ALL = originalStoreLwhAll;
		MembershipConfig.TRADE_ALL = originalTradeAll;
		MembershipConfig.REMODEL_ALL = originalRemodelAll;
		MembershipConfig.DISABLE_SOULBIND = originalDisableSoulbind;
	}

	@Test
	void sellableAndGodstoneChecksFollowTheTemplateMask() {
		Item item = item(ItemMask.SELLABLE | ItemMask.CAN_PROC_ENCHANT);

		assertTrue(item.isSellable());
		assertTrue(item.canSocketGodstone());
		assertFalse(item.canApExtract());
		assertFalse(item.canIdian());
		assertFalse(item.canAmplification());
		assertFalse(item.isArchDaevaItem());
	}

	@Test
	void warehouseStorageAndTradeRequireMaskAndAnUnboundItem() {
		Item item = item(ItemMask.STORABLE_IN_WH | ItemMask.TRADEABLE);
		Player player = playerWithPermissions((byte) -1);

		assertTrue(item.isStorableinWarehouse(player));
		assertTrue(item.isTradeable(player));

		item.setSoulBound(true);

		assertFalse(item.isStorableinWarehouse(player));
		assertFalse(item.isTradeable(player));
	}

	@Test
	void soulBindPermissionSkipOnlyAppliesWhenGranted() {
		Item item = item(ItemMask.STORABLE_IN_AWH);
		item.setSoulBound(true);

		assertTrue(item.isStorableinAccWarehouse(playerWithPermissions(MembershipConfig.DISABLE_SOULBIND)));
		assertFalse(item.isStorableinAccWarehouse(playerWithPermissions((byte) -1)));
	}

	@Test
	void effectiveMaskExpandsMembershipUnlocks() {
		Item item = item(0);
		Player player = playerWithPermissions(MembershipConfig.STORE_WH_ALL, MembershipConfig.TRADE_ALL);

		assertEquals(ItemMask.STORABLE_IN_WH | ItemMask.TRADEABLE, item.getItemMask(player));
	}

	@Test
	void remodelableFollowsTheMaskOnly() {
		Item item = item(ItemMask.REMODELABLE);
		item.setSoulBound(true);

		assertTrue(item.isRemodelable(playerWithPermissions((byte) -1)));
	}

	/**
	 * 构造带指定掩码的物品。
	 * Builds an item carrying the given template mask.
	 */
	private Item item(int mask) {
		ItemTemplate template = new ItemTemplate();
		template.modifyMask(true, mask);
		return new Item(1, template);
	}

	/**
	 * 构造仅授予指定权限位的玩家测试替身。
	 * Builds a player test double that grants exactly the given permission bytes.
	 */
	private Player playerWithPermissions(byte... granted) {
		TestPlayer player = objenesis.newInstance(TestPlayer.class);
		player.granted = granted;
		return player;
	}

	/**
	 * 玩家测试替身：仅覆写权限判定，避免依赖账号与全局配置。
	 * Player test double: only overrides permission checks, no account or global config needed.
	 */
	private static final class TestPlayer extends Player {

		private byte[] granted;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public boolean havePermission(byte perm) {
			for (byte g : granted) {
				if (g == perm) {
					return true;
				}
			}
			return false;
		}
	}
}
