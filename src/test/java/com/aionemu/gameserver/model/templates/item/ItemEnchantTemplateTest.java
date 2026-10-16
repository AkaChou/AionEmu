package com.aionemu.gameserver.model.templates.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.ItemEnchantData;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 觉醒/强化表属性解析与表外无限成长测试。
 * Tests for enchant/authorize table parsing and limitless growth beyond the table.
 */
class ItemEnchantTemplateTest {

	private static final Path TEMPLATES = Path.of("src/main/resources/aion/data/static_data/items/enchant_templates.xml");

	@Test
	void indexesAuthorizeTableLevelsFromXml() throws Exception {
		ItemEnchantTemplate earrings = load().getEnchantTemplate(EnchantType.AUTHORIZE, 10046);

		List<StatFunction> level1 = earrings.getStats(1);
		assertNotNull(level1, "level 1 must resolve from the XML table");
		assertEquals(2, level1.size());
		assertEquals(9, level1.getFirst().getValue());

		List<StatFunction> level10 = earrings.getStats(10);
		assertEquals(2, level10.size());
		assertEquals(75, valueOf(level10, StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
	}

	@Test
	void growsBeyondTheTableWithLimitlessIncrements() throws Exception {
		ItemEnchantTemplate earrings = load().getEnchantTemplate(EnchantType.AUTHORIZE, 10046);

		// 表内 10 级 = 75，表外 11 级起每级 +5 / table stops at 75, plus 5 per level from 11 onward
		assertEquals(80, valueOf(earrings.getStats(11), StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
		assertEquals(175, valueOf(earrings.getStats(30), StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
		assertEquals(1025, valueOf(earrings.getStats(200), StatEnum.PVP_ATTACK_RATIO_PHYSICAL));

		// 成长不得改写表内缓存 / growing must not mutate the cached table entries
		assertEquals(75, valueOf(earrings.getStats(10), StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
	}

	@Test
	void reusesTheHighestLevelWhenTheTableHasNoLimitless() throws Exception {
		ItemEnchantTemplate bracelet = load().getEnchantTemplate(EnchantType.AUTHORIZE, 10112);
		List<StatFunction> level10 = bracelet.getStats(10);

		for (int level : new int[] { 11, 30, 200 }) {
			List<StatFunction> beyond = bracelet.getStats(level);
			assertNotNull(beyond, "authorize level " + level + " must still resolve");
			assertEquals(level10.size(), beyond.size());
			assertEquals(57, valueOf(beyond, StatEnum.PVP_DEFEND_RATIO_PHYSICAL));
		}
	}

	@Test
	void appendsLimitlessStatsMissingFromTheTable() throws Exception {
		ItemEnchantTemplate head = load().getEnchantTemplate(EnchantType.AUTHORIZE, 10050);

		// 表内只有 PvP 防御 77，表外追加 PvP 攻击 / table carries PvP defense only, PvP attack is appended beyond it
		List<StatFunction> level11 = head.getStats(11);
		assertEquals(4, level11.size());
		assertEquals(77, valueOf(level11, StatEnum.PVP_DEFEND_RATIO_PHYSICAL));
		assertEquals(5, valueOf(level11, StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
		assertEquals(5, valueOf(level11, StatEnum.PVP_ATTACK_RATIO_MAGICAL));
	}

	@Test
	void waitsForTheLimitlessStartLevel() throws Exception {
		ItemEnchantTemplate charm = load().getEnchantTemplate(EnchantType.AUTHORIZE, 10054);

		// 表内 7 级，成长起点 11：8-10 级持平 / table ends at 7, growth starts at 11, so 8-10 plateau
		for (int level : new int[] { 8, 9, 10 }) {
			assertEquals(1, charm.getStats(level).size());
			assertEquals(10, valueOf(charm.getStats(level), StatEnum.PHYSICAL_ATTACK));
		}

		List<StatFunction> level11 = charm.getStats(11);
		assertEquals(3, level11.size());
		assertEquals(10, valueOf(level11, StatEnum.PHYSICAL_ATTACK));
		assertEquals(6, valueOf(level11, StatEnum.PVP_DEFEND_RATIO_PHYSICAL));
	}

	@Test
	void overridesTablesWhoseXmlDataDisagreesWithRetail() throws Exception {
		ItemEnchantTemplate necklace = load().getEnchantTemplate(EnchantType.AUTHORIZE, 10040);

		// 真端为线性 5×等级，XML 表内 6 级 = 60 不参与计算 / retail is linear, the XML table's 60 at level 6 is ignored
		List<StatFunction> level1 = necklace.getStats(1);
		assertEquals(2, level1.size());
		assertEquals(5, valueOf(level1, StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
		assertEquals(30, valueOf(necklace.getStats(6), StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
		assertEquals(50, valueOf(necklace.getStats(10), StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
		assertEquals(150, valueOf(necklace.getStats(30), StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
	}

	@Test
	void returnsNullWhenTheTableCarriesNoModifiers() throws Exception {
		ItemEnchantData data = (ItemEnchantData) JAXBContext.newInstance(ItemEnchantData.class).createUnmarshaller()
				.unmarshal(new StringReader("""
						<enchant_templates>
							<enchant_template id="1" type="AUTHORIZE"/>
						</enchant_templates>
						"""));

		assertNull(data.getEnchantTemplate(EnchantType.AUTHORIZE, 1).getStats(1));
	}

	private static int valueOf(List<StatFunction> stats, StatEnum stat) {
		return stats.stream().filter(func -> func.getName() == stat).findFirst().orElseThrow().getValue();
	}

	private static ItemEnchantData load() throws Exception {
		return (ItemEnchantData) JAXBContext.newInstance(ItemEnchantData.class).createUnmarshaller()
				.unmarshal(TEMPLATES.toFile());
	}
}
