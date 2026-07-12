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

class MinionTransformSkillDataTest {

	@Test
	void boostedWedaTransformUsesDistinctEffectSlotsAndCorrectPenalty() throws Exception {
		Element skill = skillTemplate(4958);

		assertEquals("9246", skill.getAttribute("penalty_skill_id"));
		assertEffect(skill, "statup", "1");
		assertEffect(skill, "onetimeboostheal", "2");
		assertEffect(skill, "boostspellattack", "3");

		Element transform = assertEffect(skill, "shapechange", "4");
		assertEquals("1", transform.getAttribute("preeffect"));
		assertEquals("202710", transform.getAttribute("model"));

		Element penalty = skillTemplate(9246);
		Element castingTime = assertEffect(penalty, "boostskillcastingtime", "1");
		Element castingTimeChange = firstDirectChild(castingTime, "change");
		assertNotNull(castingTimeChange, "skill 9246 should change BOOST_CASTING_TIME");
		assertEquals("BOOST_CASTING_TIME", castingTimeChange.getAttribute("stat"));
		assertEquals("PERCENT", castingTimeChange.getAttribute("func"));
		assertEquals("20", castingTimeChange.getAttribute("value"));
	}

	@Test
	void relatedMinionSkillsUseReferenceCriticalData() throws Exception {
		assertWedaTransform(4957, "9248", "-10", "20", "8", "15", null);
		assertWedaTransform(5005, "9248", "-10", "20", "8", "15", null);
		assertWedaTransform(5006, "9246", "-20", "30", "15", "30", "3000");

		assertChange(assertEffect(skillTemplate(4888), "statup", "3"), "SLEEP_RESISTANCE", "ADD", "700");
		assertChange(assertEffect(skillTemplate(4889), "statup", "3"), "ABNORMAL_RESISTANCE_ALL", "ADD", "1000");
		assertChange(assertEffect(skillTemplate(4889), "statup", "3"), "SPEED", "PERCENT", "30");
		assertChange(assertEffect(skillTemplate(4981), "statup", "4"), "SPEED", "PERCENT", "10");
		assertChange(assertEffect(skillTemplate(4982), "statup", "3"), "SPEED", "PERCENT", "20");

		assertEquals("HEAL", skillTemplate(4955).getAttribute("skillsubtype"));
		assertEquals("2", assertEffect(skillTemplate(4956), "heal", "2").getAttribute("e"));
		assertEquals("DEBUFF", skillTemplate(4969).getAttribute("skillsubtype"));
		assertEquals("DEBUFF", skillTemplate(4970).getAttribute("tslot"));
		assertEquals("1500", skillTemplate(4973).getAttribute("cooldown"));
	}

	private static Element assertEffect(Element skill, String name, String position) {
		Element effects = firstDirectChild(skill, "effects");
		assertNotNull(effects, "skill " + skill.getAttribute("skill_id") + " should define effects");
		NodeList children = effects.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element element && name.equals(element.getTagName()) && position.equals(element.getAttribute("e"))) {
				return element;
			}
		}
		throw new AssertionError("skill " + skill.getAttribute("skill_id") + " should define <" + name + "> in effect slot " + position);
	}

	private static void assertWedaTransform(int skillId, String penaltySkillId, String attackSpeed, String physicalAttack,
			String magicBoost, String healBoost, String maxHp) throws Exception {
		Element skill = skillTemplate(skillId);
		assertEquals(penaltySkillId, skill.getAttribute("penalty_skill_id"));
		Element statup = assertEffect(skill, "statup", "1");
		assertChange(statup, "ATTACK_SPEED", "PERCENT", attackSpeed);
		assertChange(statup, "PHYSICAL_ATTACK", "PERCENT", physicalAttack);
		if (maxHp != null) {
			assertChange(statup, "MAXHP", "ADD", maxHp);
		}
		assertChange(assertEffect(skill, "onetimeboostheal", "2"), "HEAL_SKILL_BOOST", "PERCENT", healBoost);
		assertChange(assertEffect(skill, "boostspellattack", "3"), "BOOST_SPELL_ATTACK", "PERCENT", magicBoost);
		assertEffect(skill, "shapechange", "4");

		Element penalty = skillTemplate(Integer.parseInt(penaltySkillId));
		Element castingTimeChange = firstDirectChild(assertEffect(penalty, "boostskillcastingtime", "1"), "change");
		assertNotNull(castingTimeChange, "skill " + penaltySkillId + " should change BOOST_CASTING_TIME");
		assertEquals("BOOST_CASTING_TIME", castingTimeChange.getAttribute("stat"));
		assertEquals("PERCENT", castingTimeChange.getAttribute("func"));
		assertEquals(skillId == 4957 || skillId == 5005 ? "10" : "20", castingTimeChange.getAttribute("value"));
	}

	private static void assertChange(Element effect, String stat, String func, String value) {
		NodeList children = effect.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element element && "change".equals(element.getTagName()) && stat.equals(element.getAttribute("stat"))) {
				assertEquals(func, element.getAttribute("func"), stat + " func");
				if (value != null) {
					assertEquals(value, element.getAttribute("value"), stat + " value");
				}
				return;
			}
		}
		throw new AssertionError("Missing change " + stat);
	}

	private static Element skillTemplate(int skillId) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		Document document = factory.newDocumentBuilder()
				.parse(Path.of("src/main/resources/aion/data/static_data/skills/skill_templates.xml").toFile());
		NodeList skills = document.getElementsByTagName("skill_template");
		for (int i = 0; i < skills.getLength(); i++) {
			Element skill = (Element) skills.item(i);
			if (Integer.toString(skillId).equals(skill.getAttribute("skill_id"))) {
				return skill;
			}
		}
		throw new AssertionError("Missing skill_template " + skillId);
	}

	private static Element firstDirectChild(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element element && name.equals(element.getTagName())) {
				return element;
			}
		}
		return null;
	}
}
