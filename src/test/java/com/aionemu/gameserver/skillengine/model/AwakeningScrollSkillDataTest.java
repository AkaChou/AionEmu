package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class AwakeningScrollSkillDataTest {

	@Test
	void awakeningScrollsUseScalingCastingSpeedBuffInSourceAndRuntimeData() throws Exception {
		assertAwakeningScrollData("src/main/resources/aion/game/data/static_data/items/item/item_misc_templates.xml",
				"src/main/resources/aion/game/data/static_data/skills/skill_templates.xml");
		assertAwakeningScrollData("aion/game/data/static_data/items/item/item_misc_templates.xml",
				"aion/game/data/static_data/skills/skill_templates.xml");
	}

	@Test
	void mysteriousScrollUsesScalingAttackAndCastingSpeedBuff() throws Exception {
		assertMysteriousScrollData("src/main/resources/aion/game/data/static_data/items/item/item_misc_templates.xml",
				"src/main/resources/aion/game/data/static_data/skills/skill_templates.xml");
		assertMysteriousScrollData("aion/game/data/static_data/items/item/item_misc_templates.xml",
				"aion/game/data/static_data/skills/skill_templates.xml");
	}

	@Test
	void oneHourAndEventAwakeningScrollsUseScalingCastingSpeedBuff() throws Exception {
		assertTimedAwakeningScrollData("src/main/resources/aion/game/data/static_data/items/item/item_misc_templates.xml",
				"src/main/resources/aion/game/data/static_data/skills/skill_templates.xml");
		assertTimedAwakeningScrollData("aion/game/data/static_data/items/item/item_misc_templates.xml",
				"aion/game/data/static_data/skills/skill_templates.xml");
	}

	@Test
	void arenaAwakeningScrollsUseScalingCastingSpeedBuff() throws Exception {
		assertArenaAwakeningScrollData("src/main/resources/aion/game/data/static_data/items/item/item_misc_templates.xml",
				"src/main/resources/aion/game/data/static_data/skills/skill_templates.xml");
		assertArenaAwakeningScrollData("aion/game/data/static_data/items/item/item_misc_templates.xml",
				"aion/game/data/static_data/skills/skill_templates.xml");
	}

	@Test
	void itemStatScrollsUseSkillLevelScalingBuffs() throws Exception {
		assertItemStatScrollScaling("src/main/resources/aion/game/data/static_data/skills/skill_templates.xml");
		assertItemStatScrollScaling("aion/game/data/static_data/skills/skill_templates.xml");
	}

	@Test
	void remainingItemConsumablesUseSkillLevelScalingBuffs() throws Exception {
		assertRemainingItemConsumableScaling("src/main/resources/aion/game/data/static_data/skills/skill_templates.xml");
		assertRemainingItemConsumableScaling("aion/game/data/static_data/skills/skill_templates.xml");
	}

	@Test
	void clientStatUpAttributesArePresentInSourceAndRuntimeData() throws Exception {
		assertClientStatUpAttributes("src/main/resources/aion/game/data/static_data/skills/skill_templates.xml");
		assertClientStatUpAttributes("aion/game/data/static_data/skills/skill_templates.xml");
	}

	@Test
	void clientBuffTemplatesAreNotOverwrittenByUnrelatedAttackSkills() throws Exception {
		assertClientBuffTemplates("src/main/resources/aion/game/data/static_data/skills/skill_templates.xml");
		assertClientBuffTemplates("aion/game/data/static_data/skills/skill_templates.xml");
	}

	private static void assertAwakeningScrollData(String itemPath, String skillPath) throws Exception {
		assertScrollSkillLevel(itemPath, 164000132, "9965", "1");
		assertScrollSkillLevel(itemPath, 164000133, "9965", "2");
		assertScrollSkillLevel(itemPath, 164000134, "9965", "3");

		Element effect = firstDescendant(skillTemplate(skillPath, 9965), "statup");
		assertNotNull(effect, "skill 9965 should define a statup effect");
		assertEquals("300000", effect.getAttribute("duration2"));

		Element change = firstDescendant(effect, "change");
		assertNotNull(change, "skill 9965 should change casting speed");
		assertEquals("BOOST_CASTING_TIME", change.getAttribute("stat"));
		assertEquals("PERCENT", change.getAttribute("func"));
		assertEquals("0", change.getAttribute("value"));
		assertEquals("3", change.getAttribute("delta"));
	}

	private static void assertMysteriousScrollData(String itemPath, String skillPath) throws Exception {
		assertScrollSkillLevel(itemPath, 164000421, "11217", "3");

		Element skill = skillTemplate(skillPath, 11217);
		Element attackSpeed = changeForStat(skill, "ATTACK_SPEED");
		assertNotNull(attackSpeed, "skill 11217 should change attack speed");
		assertEquals("PERCENT", attackSpeed.getAttribute("func"));
		assertEquals("0", attackSpeed.getAttribute("value"));
		assertEquals("-3", attackSpeed.getAttribute("delta"));

		Element castingSpeed = changeForStat(skill, "BOOST_CASTING_TIME");
		assertNotNull(castingSpeed, "skill 11217 should change casting speed");
		assertEquals("PERCENT", castingSpeed.getAttribute("func"));
		assertEquals("0", castingSpeed.getAttribute("value"));
		assertEquals("3", castingSpeed.getAttribute("delta"));
	}

	private static void assertTimedAwakeningScrollData(String itemPath, String skillPath) throws Exception {
		assertScrollSkillLevel(itemPath, 164002113, "10462", "3");
		assertScrollSkillLevel(itemPath, 164002118, "10467", "3");
		assertScrollSkillLevel(itemPath, 164002298, "10467", "3");
		assertScrollSkillLevel(itemPath, 164002399, "10467", "3");
		assertCastingSpeedSkill(skillPath, 10462, "3600000");
		assertCastingSpeedSkill(skillPath, 10467, "1800000");
	}

	private static void assertArenaAwakeningScrollData(String itemPath, String skillPath) throws Exception {
		assertScrollSkillLevel(itemPath, 164000158, "10364", "3");
		assertScrollSkillLevel(itemPath, 164000159, "10364", "3");
		assertScrollSkillLevel(itemPath, 164000191, "10364", "3");
		assertScrollSkillLevel(itemPath, 164000412, "10364", "3");
		assertCastingSpeedSkill(skillPath, 10364, "600000");
	}

	private static void assertCastingSpeedSkill(String skillPath, int skillId, String duration) throws Exception {
		Element effect = firstDescendant(skillTemplate(skillPath, skillId), "statup");
		assertNotNull(effect, "skill " + skillId + " should define a statup effect");
		assertEquals(duration, effect.getAttribute("duration2"));

		Element change = firstDescendant(effect, "change");
		assertNotNull(change, "skill " + skillId + " should change casting speed");
		assertEquals("BOOST_CASTING_TIME", change.getAttribute("stat"));
		assertEquals("PERCENT", change.getAttribute("func"));
		assertEquals("0", change.getAttribute("value"));
		assertEquals("3", change.getAttribute("delta"));
	}

	private static void assertItemStatScrollScaling(String skillPath) throws Exception {
		String[][] expectedChanges = {
				{ "9838", "MAXHP", "15" },
				{ "9838", "MAXMP", "15" },
				{ "9918", "FIRE_RESISTANCE", "20" },
				{ "9919", "WATER_RESISTANCE", "20" },
				{ "9920", "EARTH_RESISTANCE", "20" },
				{ "9921", "WIND_RESISTANCE", "20" },
				{ "9924", "PHYSICAL_DEFENSE", "10" },
				{ "9927", "EVASION", "10" },
				{ "9935", "SPEED", "12" },
				{ "9936", "SPEED", "18" },
				{ "9937", "SPEED", "24" },
				{ "9947", "SPEED", "12" },
				{ "9948", "SPEED", "18" },
				{ "9949", "SPEED", "24" },
				{ "9957", "PHYSICAL_CRITICAL", "30" },
				{ "9958", "MAGICAL_CRITICAL", "10" },
				{ "9961", "FLY_SPEED", "10" },
				{ "9966", "PHYSICAL_CRITICAL_RESIST", "10" },
				{ "9967", "MAGICAL_CRITICAL_RESIST", "10" },
				{ "10386", "ATTACK_SPEED", "-20" },
				{ "10387", "BOOST_CASTING_TIME", "20" },
				{ "10402", "ATTACK_SPEED", "-20" },
				{ "10403", "BOOST_CASTING_TIME", "20" },
				{ "10463", "PHYSICAL_CRITICAL", "30" },
				{ "10464", "MAGICAL_CRITICAL", "10" },
				{ "10468", "PHYSICAL_CRITICAL", "30" },
				{ "10469", "MAGICAL_CRITICAL", "10" },
				{ "10766", "PHYSICAL_CRITICAL", "125" },
				{ "10767", "MAGICAL_CRITICAL", "42" },
				{ "10768", "PHYSICAL_CRITICAL_RESIST", "65" },
				{ "10769", "MAGICAL_CRITICAL_RESIST", "42" },
				{ "10771", "MAGIC_SKILL_BOOST_RESIST", "60" },
				{ "10858", "SPEED", "30" },
				{ "10859", "ATTACK_SPEED", "-9" },
				{ "10860", "BOOST_CASTING_TIME", "9" },
				{ "10883", "FLY_SPEED", "10" },
				{ "10884", "FLY_SPEED", "10" },
				{ "11058", "PHYSICAL_CRITICAL", "30" },
				{ "11059", "MAGICAL_CRITICAL", "10" },
				{ "11060", "PHYSICAL_CRITICAL", "30" },
				{ "11061", "MAGICAL_CRITICAL", "10" },
				{ "11303", "BOOST_CASTING_TIME", "9" },
				{ "11304", "SPEED", "30" }
		};
		for (String[] expectedChange : expectedChanges) {
			Element change = changeForStat(skillTemplate(skillPath, Integer.parseInt(expectedChange[0])), expectedChange[1]);
			assertNotNull(change, "skill " + expectedChange[0] + " should change " + expectedChange[1]);
			assertEquals("0", change.getAttribute("value"), "skill " + expectedChange[0] + " " + expectedChange[1] + " value");
			assertEquals(expectedChange[2], change.getAttribute("delta"), "skill " + expectedChange[0] + " " + expectedChange[1] + " delta");
		}
	}

	private static void assertRemainingItemConsumableScaling(String skillPath) throws Exception {
		String[][] expectedChanges = {
				{ "9970", "BOOST_MAGICAL_SKILL", "10" },
				{ "10325", "ATTACK_SPEED", "-2" },
				{ "10325", "BOOST_CASTING_TIME", "2" },
				{ "10325", "PHYSICAL_ATTACK", "10" },
				{ "10794", "MAXHP", "500000" },
				{ "10854", "MAXHP", "500000" },
				{ "11091", "EVASION", "40" }
		};
		for (String[] expectedChange : expectedChanges) {
			Element change = changeForStat(skillTemplate(skillPath, Integer.parseInt(expectedChange[0])), expectedChange[1]);
			assertNotNull(change, "skill " + expectedChange[0] + " should change " + expectedChange[1]);
			assertEquals("0", change.getAttribute("value"), "skill " + expectedChange[0] + " " + expectedChange[1] + " value");
			assertEquals(expectedChange[2], change.getAttribute("delta"), "skill " + expectedChange[0] + " " + expectedChange[1] + " delta");
		}
	}

	private static void assertClientStatUpAttributes(String skillPath) throws Exception {
		String[][] expectedChanges = {
				{ "3000", "MAGIC_SKILL_BOOST_RESIST", "ADD", "69" },
				{ "3001", "MAGIC_SKILL_BOOST_RESIST", "ADD", "137" },
				{ "3002", "MAGIC_SKILL_BOOST_RESIST", "ADD", "205" },
				{ "3003", "MAGIC_SKILL_BOOST_RESIST", "ADD", "273" },
				{ "3004", "MAGIC_SKILL_BOOST_RESIST", "ADD", "341" },
				{ "3005", "MAGIC_SKILL_BOOST_RESIST", "ADD", "409" },
				{ "3006", "MAGIC_SKILL_BOOST_RESIST", "ADD", "477" },
				{ "3007", "MAGIC_SKILL_BOOST_RESIST", "ADD", "545" },
				{ "3008", "MAGIC_SKILL_BOOST_RESIST", "ADD", "613" },
				{ "18044", "PHYSICAL_DEFENSE", "ADD", "65000" },
				{ "22780", "BOOST_MAGICAL_SKILL", "ADD", "5000" }
		};
		for (String[] expectedChange : expectedChanges) {
			Element change = changeForStat(skillTemplate(skillPath, Integer.parseInt(expectedChange[0])), expectedChange[1]);
			assertNotNull(change, "skill " + expectedChange[0] + " should change " + expectedChange[1]);
			assertEquals(expectedChange[2], change.getAttribute("func"), "skill " + expectedChange[0] + " " + expectedChange[1] + " func");
			assertEquals(expectedChange[3], change.getAttribute("value"), "skill " + expectedChange[0] + " " + expectedChange[1] + " value");
		}
	}

	private static void assertClientBuffTemplates(String skillPath) throws Exception {
		assertSkillAttribute(skillPath, 11469, "skilltype", "MAGICAL");
		assertSkillAttribute(skillPath, 11469, "skillsubtype", "NONE");
		assertSkillAttribute(skillPath, 11469, "tslot", "BUFF");
		assertSkillAttribute(skillPath, 11469, "cooldown", "18000");
		assertChange(skillPath, 11469, "SPEED", "PERCENT", "20");
		assertChange(skillPath, 11469, "ATTACK_SPEED", "PERCENT", "-20");
		assertChange(skillPath, 11469, "BOOST_CASTING_TIME", "PERCENT", "20");

		assertSkillAttribute(skillPath, 17783, "skillsubtype", "BUFF");
		assertSkillAttribute(skillPath, 17783, "tslot", "BUFF");
		assertChange(skillPath, 17783, "PHYSICAL_DEFENSE", "PERCENT", "1");
		assertChange(skillPath, 17783, "PHYSICAL_ATTACK", "PERCENT", "1");
		assertChange(skillPath, 17783, "MAGICAL_ATTACK", "PERCENT", "1");

		assertSkillAttribute(skillPath, 21746, "skillsubtype", "BUFF");
		assertSkillAttribute(skillPath, 21746, "tslot", "BUFF");
		assertChange(skillPath, 21746, "PHYSICAL_DEFENSE", "ADD", "1000");
		assertSkillAttribute(skillPath, 21747, "skillsubtype", "BUFF");
		assertSkillAttribute(skillPath, 21747, "tslot", "BUFF");
		assertChange(skillPath, 21747, "PHYSICAL_DEFENSE", "ADD", "1000");
	}

	private static void assertSkillAttribute(String skillPath, int skillId, String attribute, String expected) throws Exception {
		Element skill = skillTemplate(skillPath, skillId);
		assertEquals(expected, skill.getAttribute(attribute), "skill " + skillId + " " + attribute);
	}

	private static void assertChange(String skillPath, int skillId, String stat, String func, String value) throws Exception {
		Element change = changeForStat(skillTemplate(skillPath, skillId), stat);
		assertNotNull(change, "skill " + skillId + " should change " + stat);
		assertEquals(func, change.getAttribute("func"), "skill " + skillId + " " + stat + " func");
		assertEquals(value, change.getAttribute("value"), "skill " + skillId + " " + stat + " value");
	}

	private static void assertScrollSkillLevel(String itemPath, int itemId, String skillId, String level) throws Exception {
		Element skillUse = firstDescendant(itemTemplate(itemPath, itemId), "skilluse");
		assertNotNull(skillUse, "Scroll " + itemId + " should use a skill");
		assertEquals(skillId, skillUse.getAttribute("skillid"));
		assertEquals(level, skillUse.getAttribute("level"));
	}

	private static Element itemTemplate(String itemPath, int itemId) throws Exception {
		Document document = xml(itemPath);
		NodeList items = document.getElementsByTagName("item_template");
		for (int i = 0; i < items.getLength(); i++) {
			Element item = (Element) items.item(i);
			if (Integer.toString(itemId).equals(item.getAttribute("id"))) {
				return item;
			}
		}
		throw new AssertionError("Missing item_template " + itemId);
	}

	private static Element skillTemplate(String skillPath, int skillId) throws Exception {
		Document document = xml(skillPath);
		NodeList skills = document.getElementsByTagName("skill_template");
		for (int i = 0; i < skills.getLength(); i++) {
			Element skill = (Element) skills.item(i);
			if (Integer.toString(skillId).equals(skill.getAttribute("skill_id"))) {
				return skill;
			}
		}
		throw new AssertionError("Missing skill_template " + skillId);
	}

	private static Element changeForStat(Element parent, String stat) {
		NodeList changes = parent.getElementsByTagName("change");
		for (int i = 0; i < changes.getLength(); i++) {
			Element change = (Element) changes.item(i);
			if (stat.equals(change.getAttribute("stat"))) {
				return change;
			}
		}
		return null;
	}

	private static Document xml(String path) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(Path.of(path).toFile());
	}

	private static Element firstDescendant(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element element) {
				if (name.equals(element.getTagName())) {
					return element;
				}
				Element descendant = firstDescendant(element, name);
				if (descendant != null) {
					return descendant;
				}
			}
		}
		return null;
	}
}
