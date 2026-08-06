package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.globaldrops.GlobalRule;
import com.aionemu.gameserver.model.templates.npc.NpcRating;

class GlobalDropDataTest {

	private static final Path RULES = Path.of("src/main/resources/aion/data/static_data/global_drops/global_rules.xml");
	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/global_drops/global_rules.xsd");

	@Test
	void kinahRulesCoverEveryNpcRatingAtGuaranteedChance() throws Exception {
		GlobalDropData data = (GlobalDropData) JAXBContext.newInstance(GlobalDropData.class)
				.createUnmarshaller().unmarshal(RULES.toFile());
		assertEquals(NpcRating.values().length, data.getAllRules().size());
		Map<NpcRating, GlobalRule> rulesByRating = new EnumMap<>(NpcRating.class);
		for (GlobalRule rule : data.getAllRules()) {
			assertEquals(1, rule.getGlobalRuleRatings().getGlobalDropRatings().size());
			NpcRating rating = rule.getGlobalRuleRatings().getGlobalDropRatings().getFirst().getRating();
			rulesByRating.put(rating, rule);
			assertEquals(100f, rule.getChance());
			assertTrue(rule.getNoReduction());
			assertTrue(rule.isCountPerNpcLevel());
			assertEquals(1, rule.getGlobalRuleItems().getGlobalDropItems().size());
			assertEquals(182400001, rule.getGlobalRuleItems().getGlobalDropItems().getFirst().getId());
		}

		assertEquals(NpcRating.values().length, rulesByRating.size());
		assertScaledRanges(rulesByRating.get(NpcRating.JUNK), 1, 3);
		assertScaledRanges(rulesByRating.get(NpcRating.NORMAL), 3, 12);
		assertScaledRanges(rulesByRating.get(NpcRating.ELITE), 35, 80);
		assertScaledRanges(rulesByRating.get(NpcRating.HERO), 80, 250);
		assertScaledRanges(rulesByRating.get(NpcRating.LEGENDARY), 250, 750);
	}

	@Test
	void globalDropRulesValidateAgainstSchema() throws Exception {
		SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(SCHEMA.toFile()).newValidator().validate(new StreamSource(RULES.toFile()));
	}

	private static void assertScaledRanges(GlobalRule rule, long minPerLevel, long maxPerLevel) {
		assertEquals(minPerLevel, rule.getMinCountForNpcLevel(0));
		assertEquals(maxPerLevel, rule.getMaxCountForNpcLevel(0));
		assertEquals(minPerLevel, rule.getMinCountForNpcLevel(1));
		assertEquals(maxPerLevel, rule.getMaxCountForNpcLevel(1));
		assertEquals(minPerLevel * 70, rule.getMinCountForNpcLevel(70));
		assertEquals(maxPerLevel * 70, rule.getMaxCountForNpcLevel(70));
	}
}
