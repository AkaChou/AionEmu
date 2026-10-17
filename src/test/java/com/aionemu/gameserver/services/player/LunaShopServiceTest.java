package com.aionemu.gameserver.services.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.luna.LunaConsumeRewardsTemplate;

class LunaShopServiceTest {

	@Test
	void lunaPricesMatchChinaClientAndNeverAllowOverdraft() {
		assertEquals(2, LunaShopService.MATERIAL_BOX_PRICE);
		assertEquals(12, LunaShopService.WARDROBE_APPEARANCE_PRICE);
		assertEquals(20, LunaShopService.LUNA_INSTANCE_PRICE);
		assertEquals(5, LunaShopService.TREASURE_CHEST_PRICE);
		assertEquals(10, LunaShopService.wardrobePrice(3));
		assertEquals(12, LunaShopService.wardrobePrice(8));
		assertEquals(-1, LunaShopService.wardrobePrice(9));
		// 真端无每日掷骰次数上限（User::StartRollDice 只查格子段位），露娜余额即闸门。
		// The retail server has no daily roll cap; the Luna balance is the only gate.
		assertEquals(3, LunaShopService.LUNA_DICE_PRICE);
		// 黄金骰子：5 掷全微笑（无掷空）后按实机客户端显示收 35。
		// Gold dice: charged 35 after 5 smile-only rolls, per the live client display.
		assertEquals(35, LunaShopService.LUNA_GOLD_DICE_PRICE);
		assertEquals(5, LunaShopService.DICE_GOLD_STAGE_TRIES);
		assertTrue(LunaShopService.canSpendLuna(2, 2));
		assertFalse(LunaShopService.canSpendLuna(1, 2));
		assertFalse(LunaShopService.canSpendLuna(10, -1));
		assertFalse(LunaShopService.canClaimDiceReward(0));
		assertTrue(LunaShopService.canClaimDiceReward(1));
	}

	@Test
	void goldDiceUsesRetailOpenNumbersAndChinaWeights() {
		// 真端 finalOpenNum = success_total(5) + 桶索引 → 5/6/7，客户端 ≥5 才翻金骰并显示领取按钮。
		// Retail finalOpenNum = success_total(5) + bucket index -> 5/6/7; the client flips gold dice at >=5.
		assertEquals(5, LunaShopService.goldDiceOpenNum(3));
		assertEquals(6, LunaShopService.goldDiceOpenNum(4));
		assertEquals(7, LunaShopService.goldDiceOpenNum(5));
		// 中国区 dice_set_05 权重 5500/4000/500 → 只出 A/S/SS 三档。
		// China dice_set_05 weights 5500/4000/500 yield only A/S/SS.
		for (int i = 0; i < 2000; i++) {
			int grade = LunaShopService.rollGoldGrade();
			assertTrue(grade >= 3 && grade <= 5, "gold grade out of A..SS range: " + grade);
		}
	}

	@Test
	void consumeTiersGrantByRangeNotExactPoint() {
		List<LunaConsumeRewardsTemplate> rows = List.of(tier(1, 25), tier(2, 50), tier(3, 100), tier(4, 150),
				tier(5, 300), tier(6, 500), tier(7, 1000));
		// 消耗被骰子等零头垫开（如 23+5=28）也必须触发档位；旧 switch 精确匹配会永久漏发。
		// Spending that skips the exact tier value (e.g. 23+5=28) must still grant; the legacy exact switch missed it.
		assertEquals(List.of(1), ids(LunaShopService.crossedTiers(rows, 28, 0)));
		// 一次消耗跨多档时全部发放，且按档位升序。
		// All tiers crossed in one spend are granted, ascending.
		assertEquals(List.of(1, 2, 3), ids(LunaShopService.crossedTiers(rows, 103, 0)));
		// 已领基线之后的下一次消耗补发历史漏发档位。
		// The next spend after the claimed baseline heals historically missed tiers.
		assertEquals(List.of(3), ids(LunaShopService.crossedTiers(rows, 103, 2)));
		// 未达档与已领完均为空。
		// Nothing below the first tier and nothing after all tiers are claimed.
		assertEquals(List.of(), ids(LunaShopService.crossedTiers(rows, 24, 0)));
		assertEquals(List.of(), ids(LunaShopService.crossedTiers(rows, 1030, 7)));
		// 历史脏数据（如 consumePoint=150 但 claimedId=7 远超当前最大档位 4）自愈：重置基线为 0 并补发 1..4
		assertEquals(List.of(1, 2, 3, 4), ids(LunaShopService.crossedTiers(rows, 150, 7)));
	}

	@Test
	void lunaConsumeRewardsXmlUnmarshalMatchesChinaClient() throws Exception {
		java.io.File file = new java.io.File("src/main/resources/aion/data/static_data/luna/luna_consume_rewards.xml");
		jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(com.aionemu.gameserver.dataholders.LunaConsumeRewardsData.class);
		jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
		com.aionemu.gameserver.dataholders.LunaConsumeRewardsData data = (com.aionemu.gameserver.dataholders.LunaConsumeRewardsData) unmarshaller.unmarshal(file);
		assertEquals(7, data.size());
		assertEquals(7, data.getAll().size());
		LunaConsumeRewardsTemplate tier1 = data.getLunaConsumeRewardsId(1);
		assertEquals(1, tier1.getId());
		assertEquals("luna_reward_01", tier1.getName());
		assertEquals(25, tier1.getSumCount());
		assertEquals(0, tier1.getGachaCost());
		assertEquals(188055460, tier1.getCreateItemId());
		assertEquals(1, tier1.getCreateItemCount());
	}

	private static LunaConsumeRewardsTemplate tier(int rowId, int sumCount) {
		// 参数名避开继承字段：匿名类体内简单名 id/luna_sum_count 会遮蔽外层同名参数。
		// Parameter names avoid the inherited fields: simple names shadow same-named captured parameters.
		return new LunaConsumeRewardsTemplate() {
			{
				this.id = rowId;
				this.luna_sum_count = sumCount;
			}
		};
	}

	private static List<Integer> ids(List<LunaConsumeRewardsTemplate> rows) {
		return rows.stream().map(LunaConsumeRewardsTemplate::getId).toList();
	}
}
