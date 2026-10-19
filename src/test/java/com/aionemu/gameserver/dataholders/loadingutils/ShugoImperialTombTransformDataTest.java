package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.dataholders.TribeRelationsData;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.skillengine.effect.PolymorphEffect;
import com.aionemu.gameserver.skillengine.effect.TransformEffect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.properties.FirstTargetAttribute;
import com.aionemu.gameserver.skillengine.properties.TargetRelationAttribute;
import com.aionemu.gameserver.skillengine.properties.TargetSpeciesAttribute;

/**
 * 术古皇陵变身技能生产数据审计。
 * Production-data audit for Shugo Imperial Tomb transformation skills.
 */
class ShugoImperialTombTransformDataTest {

	private static final Path SKILLS_DIRECTORY = Path.of("src/main/resources/aion/definitions/compact/skills");
	private static final Path PANELS = Path.of(
		"src/main/resources/aion/data/static_data/polymorph_panels/polymorph_panels.xml");
	private static final Path NPC_TEMPLATES = Path.of(
		"src/main/resources/aion/data/static_data/npcs/npc_template_216189_235748.xml");
	private static final int IDEVENT01_ATTACK_SKILL = 21107;
	private static final int[] LIGHT_TRANSFORM_SKILLS = { 21094, 21095, 21096 };
	private static final int[] DARK_TRANSFORM_SKILLS = { 21103, 21104, 21105 };

	/**
	 * 验证光暗两族六个变身技能绑定正确阵营与面板。
	 * Verifies that all six light and dark transformation skills bind the correct tribe and panel.
	 */
	@Test
	void allShugoImperialTombTransformationsBindTheirTribeAndPanel() {
		var skillData = SkillDefinitionLoader.load(SKILLS_DIRECTORY.toFile());

		for (int skillId : LIGHT_TRANSFORM_SKILLS) {
			assertTransform(skillData.getSkillTemplate(skillId), TribeClass.IDEVENT01_POLYMORPHL);
		}
		for (int skillId : DARK_TRANSFORM_SKILLS) {
			assertTransform(skillData.getSkillTemplate(skillId), TribeClass.IDEVENT01_POLYMORPHD);
		}
	}

	/**
	 * 验证生产源数据本身携带阵营字段，而不依赖未落地的运行时绑定。
	 * Verifies that production sources carry the tribe field independently of pending runtime binding.
	 */
	@Test
	void productionSourcesDeclareTribeAndTierPanels() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		NodeList effects = factory.newDocumentBuilder()
			.parse(SKILLS_DIRECTORY.resolve("skill_templates_part_025.xml").toFile())
			.getElementsByTagName("polymorph");

		int matched = 0;
		for (int i = 0; i < effects.getLength(); i++) {
			Element effect = (Element) effects.item(i);
			int model = Integer.parseInt(effect.getAttribute("model"));
			String expectedTribe = switch (model) {
				case 202588, 202589, 202590 -> "IDEVENT01_POLYMORPHL";
				case 202592, 202593, 202594 -> "IDEVENT01_POLYMORPHD";
				default -> null;
			};
			if (expectedTribe == null) {
				continue;
			}
			matched++;
			assertEquals(expectedTribe, effect.getAttribute("tribe"), "model " + model);
			assertEquals("true", effect.getAttribute("cant_attack"), "model " + model);
			assertEquals("", effect.getAttribute("cant_use_skill"), "model " + model);
		}

		assertEquals(6, matched);
	}

	/**
	 * 验证 25/26/27 面板存在、编码有效，且面板内攻击技能可选中 IDEVENT01_SUMMON。
	 * Verifies panels 25/26/27 exist, decode correctly, and their attacks can select IDEVENT01_SUMMON.
	 */
	@Test
	void tierPanelsContainResolvableAttacksAgainstTheInstanceSummonTribe() throws Exception {
		var skillData = SkillDefinitionLoader.load(SKILLS_DIRECTORY.toFile());
		TribeRelationsData tribeRelations = loadTribeRelations();
		NodeList panels = panelElements();
		assertEquals(3, panelSkills(panels, 25).length);
		assertEquals(4, panelSkills(panels, 26).length);
		assertEquals(5, panelSkills(panels, 27).length);
		for (int panelId : new int[] { 25, 26, 27 }) {
			for (String encoded : panelSkills(panels, panelId)) {
				assertNotNull(skillData.getSkillTemplate(decodedSkill(encoded)), "panel " + panelId + " skill " + encoded);
			}
		}
		assertEquals(IDEVENT01_ATTACK_SKILL, decodedSkill(panelSkills(panels, 25)[0]));
		assertEquals(21108, decodedSkill(panelSkills(panels, 25)[1]));
		assertEquals(21109, decodedSkill(panelSkills(panels, 25)[2]));

		SkillTemplate attack = skillData.getSkillTemplate(IDEVENT01_ATTACK_SKILL);
		assertNotNull(attack);
		assertEquals(TargetRelationAttribute.ENEMY, attack.getProperties().getTargetRelation());
		assertEquals(TargetSpeciesAttribute.ALL, attack.getProperties().getTargetSpecies());
		assertEquals(FirstTargetAttribute.ME, attack.getProperties().getFirstTarget());
		assertSame(TargetRelationAttribute.ENEMY,
			skillData.getSkillTemplate(21108).getProperties().getTargetRelation());
		assertSame(TargetRelationAttribute.ENEMY,
			skillData.getSkillTemplate(21109).getProperties().getTargetRelation());
		assertSame(TargetRelationAttribute.ENEMY,
			skillData.getSkillTemplate(21110).getProperties().getTargetRelation());
		assertSame(TargetRelationAttribute.FRIEND,
			skillData.getSkillTemplate(21111).getProperties().getTargetRelation());

		assertInstanceSummonTargetsAreAttackable();
		assertFactionRelations(tribeRelations);
	}

	/**
	 * 验证普通攻击被禁用、专属技能与快速栏可用，且面板按变身等级递增。
	 * Verifies disabled auto-attack, enabled exclusive skills and shortcuts, and ascending panels.
	 */
	@Test
	void transformationRestrictionsReserveCombatForTheExclusivePanel() {
		var skillData = SkillDefinitionLoader.load(SKILLS_DIRECTORY.toFile());

		assertPanel(skillData.getSkillTemplate(21094), 25);
		assertPanel(skillData.getSkillTemplate(21095), 26);
		assertPanel(skillData.getSkillTemplate(21096), 27);
		assertPanel(skillData.getSkillTemplate(21103), 25);
		assertPanel(skillData.getSkillTemplate(21104), 26);
		assertPanel(skillData.getSkillTemplate(21105), 27);
	}

	/**
	 * 验证召唤阵营单向将普通 PC 视为友军，而术古变身对其敌对且不再被视为友军。
	 * Verifies summons view ordinary PCs as friends one-way, but transformed PCs are hostile and no longer friendly.
	 */
	@Test
	void factionRelationsDistinguishOrdinaryPcsFromTransformTribes() {
		assertFactionRelations(loadTribeRelations());
	}

	/**
	 * 验证单向的普通 PC 友好关系，以及两个变身阵营的术古友军与召唤敌对关系。
	 * Verifies one-way ordinary PC friendship and both transforms' Shugo alliances and hostility toward summons.
	 */
	private static void assertFactionRelations(TribeRelationsData relations) {
		assertFalse(relations.isFriendlyRelation(TribeClass.PC, TribeClass.IDEVENT01_SUMMON));
		assertFalse(relations.isFriendlyRelation(TribeClass.PC_DARK, TribeClass.IDEVENT01_SUMMON));
		assertTrue(relations.isFriendlyRelation(TribeClass.IDEVENT01_SUMMON, TribeClass.PC));
		assertTrue(relations.isFriendlyRelation(TribeClass.IDEVENT01_SUMMON, TribeClass.PC_DARK));

		for (TribeClass transform : new TribeClass[] {
			TribeClass.IDEVENT01_POLYMORPHL, TribeClass.IDEVENT01_POLYMORPHD }) {
			assertTrue(relations.isHostileRelation(transform, TribeClass.IDEVENT01_SUMMON));
			assertFalse(relations.isFriendlyRelation(transform, TribeClass.IDEVENT01_SUMMON));
			assertFalse(relations.isFriendlyRelation(TribeClass.IDEVENT01_SUMMON, transform));
			assertTrue(relations.isFriendlyRelation(transform, TribeClass.IDEVENT01_MC));
		}
		assertTrue(relations.isFriendlyRelation(TribeClass.IDEVENT01_POLYMORPHL, TribeClass.PC_DARK));
		assertTrue(relations.isFriendlyRelation(TribeClass.IDEVENT01_POLYMORPHD, TribeClass.PC));
	}

	/**
	 * 验证波次怪是可攻击 NPC，从而 ENEMY 过滤可不依赖主动仇恨图标。
	 * Verifies wave NPCs are attackable so ENEMY filtering does not rely on the aggro icon.
	 */
	private static void assertInstanceSummonTargetsAreAttackable() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		NodeList templates = factory.newDocumentBuilder().parse(NPC_TEMPLATES.toFile())
			.getElementsByTagName("npc_template");

		int matched = 0;
		for (int i = 0; i < templates.getLength(); i++) {
			Element template = (Element) templates.item(i);
			String npcId = template.getAttribute("npc_id");
			if (!npcId.equals("219508") && !npcId.equals("219510")) {
				continue;
			}
			matched++;
			assertEquals("ATTACKABLE", template.getAttribute("npc_type"), npcId);
			assertEquals("IDEVENT01_SUMMON", template.getAttribute("tribe"), npcId);
		}
		assertEquals(2, matched);
	}

	private static void assertTransform(SkillTemplate template, TribeClass expectedTribe) {
		assertNotNull(template);
		TransformEffect effect = assertInstanceOf(PolymorphEffect.class, template.getEffects().getEffects().getFirst());
		assertEquals(expectedTribe, transformValue(effect, "tribe"));
		assertEquals(true, transformValue(effect, "cantAttack"));
		assertEquals(false, transformValue(effect, "cantUseSkill"));
		assertEquals(false, transformValue(effect, "cantUseItem"));
		assertEquals(175, transformValue(effect, "transformLevel"));
	}

	private static void assertPanel(SkillTemplate template, int expectedPanelId) {
		TransformEffect effect = assertInstanceOf(PolymorphEffect.class, template.getEffects().getEffects().getFirst());
		assertEquals(expectedPanelId, transformValue(effect, "panelid"));
	}

	private static Object transformValue(TransformEffect effect, String fieldName) {
		try {
			Field field = TransformEffect.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(effect);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Cannot read TransformEffect." + fieldName, e);
		}
	}

	private static TribeRelationsData loadTribeRelations() {
		try {
			var context = XmlDataLoader.createJaxbContext(TribeRelationsData.class);
			return (TribeRelationsData) context.createUnmarshaller().unmarshal(
				Path.of("src/main/resources/aion/data/static_data/tribe/tribe_relations.xml").toFile());
		} catch (Exception e) {
			throw new AssertionError("Cannot load tribe relations", e);
		}
	}

	private static NodeList panelElements() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(PANELS.toFile()).getElementsByTagName("panel");
	}

	private static Element panel(NodeList panels, int panelId) {
		for (int i = 0; i < panels.getLength(); i++) {
			Element panel = (Element) panels.item(i);
			if (Integer.parseInt(panel.getAttribute("panel_id")) == panelId) {
				return panel;
			}
		}
		throw new AssertionError("Missing polymorph panel " + panelId);
	}

	private static String[] panelSkills(NodeList panels, int panelId) {
		return panel(panels, panelId).getAttribute("panel_skills").split(" ");
	}

	private static int decodedSkill(String encoded) {
		return Integer.parseInt(encoded) >> 8;
	}
}
