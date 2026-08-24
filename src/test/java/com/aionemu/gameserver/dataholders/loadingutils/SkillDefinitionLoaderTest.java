package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.skillengine.action.ChargeUseAction;
import com.aionemu.gameserver.skillengine.action.ItemUseAction;
import com.aionemu.gameserver.skillengine.condition.ChainCondition;
import com.aionemu.gameserver.skillengine.condition.CombatCheckCondition;
import com.aionemu.gameserver.skillengine.condition.IdianChargeCondition;
import com.aionemu.gameserver.skillengine.condition.RideRobotCondition;
import com.aionemu.gameserver.skillengine.model.StigmaType;

class SkillDefinitionLoaderTest {

	@TempDir
	Path directory;

	@Test
	void expandsGroupsAndRetailFieldsAcrossParts() throws Exception {
		Files.writeString(directory.resolve("index.xml"), """
			<skill_bundle templates="2" groups="groups.xml">
				<part file="part-1.xml"/><part file="part-2.xml"/>
			</skill_bundle>
			""");
		Files.writeString(directory.resolve("groups.xml"), """
			<skill_groups><field_names><field id="1" name="retail_value"/></field_names><groups>
				<group id="G"><properties first_target="ME" first_target_range="1" target_relation="FRIEND" target_type="ONLYONE"/></group>
				<group id="A"><actions><chargeuse weapon="10" armor="11"/></actions></group>
			</groups></skill_groups>
			""");
		writePart("part-1.xml", 100, "SKILL_ONE", "first", 0);
		writePart("part-2.xml", 200, "SKILL_TWO", "second", 1);

		var data = SkillDefinitionLoader.load(directory.toFile());

		assertEquals(2, data.size());
		assertNotNull(data.getSkillTemplate(100).getProperties());
		assertEquals("first", data.getSkillTemplate(100).getRetailFields().get("retail_value"));
		assertEquals("second", data.getSkillTemplate(200).getRetailFields().get("retail_value", 1));
		assertEquals(100, data.getSkillTemplateByGroup("ONE").getSkillId());
		assertEquals(200, data.getSkillTemplateByGroup("TWO").getSkillId());
		ChargeUseAction charge = (ChargeUseAction) data.getSkillTemplate(100).getActions().getActions().getFirst();
		assertEquals(10, charge.getWeapon());
		assertEquals(11, charge.getArmor());
	}

	@Test
	void recordsDetailedLoadPhaseTimings() throws Exception {
		Files.writeString(directory.resolve("index.xml"), """
			<skill_bundle templates="1" groups="groups.xml">
				<part file="part-1.xml"/>
			</skill_bundle>
			""");
		Files.writeString(directory.resolve("groups.xml"), """
			<skill_groups><field_names><field id="1" name="retail_value"/></field_names><groups>
				<group id="G">
					<properties first_target="ME" first_target_range="1" target_relation="FRIEND" target_type="ONLYONE"/>
				</group>
				<group id="A"><actions><chargeuse weapon="10" armor="11"/></actions></group>
			</groups></skill_groups>
			""");
		writePart("part-1.xml", 100, "SKILL_ONE", "first", 0);
		var phaseTimings = new ConcurrentHashMap<String, Long>();

		var data = SkillDefinitionLoader.load(directory.toFile(), phaseTimings);

		assertEquals(1, data.size());
		assertEquals(Set.of("SkillIndexGroupsWall", "SkillJaxbContextWall", "SkillStreamJaxbWork",
			"SkillCooldownWall"), phaseTimings.keySet());
		assertTrue(phaseTimings.values().stream().allMatch(duration -> duration >= 0));
	}

	@Test
	void loadsCurrentRetailBundle() {
		var data = SkillDefinitionLoader.load(
			Path.of("src/main/resources/aion/definitions/compact/skills").toFile());

		assertEquals(14_518, data.size());
		assertNotNull(data.getSkillTemplate(21_125));
		for (int[] range : new int[][] { { 10592, 10599 }, { 11151, 11159 }, { 11452, 11465 }, { 11619, 11651 }, { 17603, 17604 } }) {
			for (int skillId = range[0]; skillId <= range[1]; skillId++) {
				assertNotNull(data.getSkillTemplate(skillId), "物品引用的技能 " + skillId + " 未加载");
			}
		}
		assertEquals("Test_Vritra1_ReduceSkill", data.getSkillTemplate(21919).getExclusiveAttribute());
		assertEquals("vritra2", data.getExclusiveAttribute("Test_Vritra1_ReduceSkill").tag());
		assertEquals(20, data.getItemExclusiveAttributes(111300460).getFirst().skillPercent());
		assertEquals(800, data.applyExclusiveSkillReduction(1_000, List.of(111300460),
			"Test_Vritra1_ReduceSkill"));
		assertEquals(920, data.applyExclusiveSkillReduction(1_000, List.of(111101711),
			"Test_Vritra1_ReduceSkill"));
		assertEquals(200, data.getExclusiveStatusImmune(List.of(111300461), "Test_Vritra1_Immune"));
		assertNotNull(data.getSkillTemplate(4698));
		assertNotNull(data.getSkillTemplate(11404));
		assertTrue(data.getSkillTemplate(3541).isTargetStop());
		assertEquals(12_000, data.getSkillTemplate(398).getToggleTimer());
		assertEquals(8674, data.getSkillTemplate(621).getPenaltySkillId());
		assertEquals("FI_BladeShock1", data.getSkillTemplate(727).getChargeSetName());
		assertEquals("PARRY", data.getSkillTemplate(584).getCounterSkill().name());
		assertEquals(80, data.getSkillTemplate(838).getPvpDamage());
		assertEquals(26, data.getSkillTemplate(838).getProperties().getTargetDistance());
		assertEquals(3, data.getSkillTemplate(838).getProperties().getEffectiveWidth());
		assertEquals(5, data.getSkillTemplate(838).getProperties().getEffectiveAltitude());
		assertEquals(0, data.getSkillTemplate(838).getProperties().getEffectiveAngle());
		for (int skillId : new int[] { 555, 670, 758, 769 }) {
			assertEquals(7, data.getSkillTemplate(skillId).getProperties().getEffectiveRange());
			assertEquals(5, data.getSkillTemplate(skillId).getProperties().getEffectiveAltitude());
			assertEquals(0, data.getSkillTemplate(skillId).getProperties().getEffectiveAngle());
		}
		assertEquals(5, data.getSkillTemplate(20069).getProperties().getEffectiveRange());
		assertEquals(45, data.getSkillTemplate(20069).getProperties().getEffectiveAltitude());
		assertEquals(0, data.getSkillTemplate(20069).getProperties().getEffectiveAngle());
		assertEquals(0, data.getSkillTemplate(20400).getProperties().getEffectiveRange());
		assertEquals(25, data.getSkillTemplate(20400).getProperties().getTargetDistance());
		assertEquals(8, data.getSkillTemplate(20400).getProperties().getEffectiveAltitude());
		assertEquals(2, data.getSkillTemplate(20400).getProperties().getEffectiveWidth());
		assertEquals(0, data.getSkillTemplate(20400).getProperties().getEffectiveAngle());
		assertEquals(100, data.getSkillTemplate(838).getChainSkillProb());
		assertTrue(data.getSkillTemplate(2993).isStance());
		assertTrue(data.getSkillTemplate(601).getStartconditions().getConditions().stream()
			.filter(ChainCondition.class::isInstance).map(ChainCondition.class::cast)
			.anyMatch(chain -> "W_CHAINB_2TH_1".equals(chain.getCategory())));
		assertTrue(data.getSkillTemplate(245).getActions().getActions().stream()
			.anyMatch(ItemUseAction.class::isInstance));
		assertEquals(2, data.getSkillTemplate(4769).getStanceType());
		assertEquals(StigmaType.BASIC, data.getSkillTemplate(539).getStigmaType());
		assertEquals(77, data.getSkillTemplate(1).getUseconditions().getConditions().stream()
			.filter(IdianChargeCondition.class::isInstance).map(IdianChargeCondition.class::cast)
			.findFirst().orElseThrow().getValue());
		assertTrue(data.getSkillTemplate(295).getStartconditions().getConditions().stream()
			.anyMatch(CombatCheckCondition.class::isInstance));
		assertFalse(data.getSkillTemplate(295).isApplyCastingTimeBonus());
		assertTrue(data.getSkillTemplate(257).isNoSaveOnLogout());
		assertTrue(data.getSkillTemplate(240).isSpendTimeOnLogout());
		assertTrue(data.getSkillTemplate(500).isRemainCooltimeOnLogin());
		assertTrue(data.shouldPersistCooldown(data.getSkillTemplate(500).getDelayId()));
		assertEquals(-36, data.getSkillTemplate(2383).getCooldownDelta());
		assertTrue(data.getSkillTemplate(2383).getStartconditions().getConditions().stream()
			.anyMatch(RideRobotCondition.class::isInstance));
		data.getSkillTemplates().stream()
			.filter(template -> template.getEffects() != null)
			.flatMap(template -> template.getEffects().getEffects().stream()
				.map(effect -> Map.entry(template.getSkillId(), effect)))
			.forEach(entry -> assertNotNull(entry.getValue().getEffectType(),
				() -> "技能 " + entry.getKey() + " 的效果 " + entry.getValue().getClass().getSimpleName() + " 缺少 EffectType"));
	}

	@Test
	void currentRetailBundleValidatesAgainstSchema() throws Exception {
		var validator = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
			.newSchema(Path.of("src/main/resources/aion/definitions/schemas/skills.xsd").toFile())
			.newValidator();
		try (var parts = Files.newDirectoryStream(
			Path.of("src/main/resources/aion/definitions/compact/skills"), "skill_templates_part_*.xml")) {
			for (Path part : parts) {
				validator.validate(new StreamSource(part.toFile()));
			}
		}
	}

	@Test
	void currentRetailFieldInventoryIsExplicit() throws Exception {
		Set<String> fieldNames = readRetailFieldNames(
			Path.of("src/main/resources/aion/definitions/compact/skills/groups.xml"));

		assertEquals(228, fieldNames.size());
		fieldNames.removeIf(name -> name.startsWith("effect"));
		assertEquals(Set.of("__type_desc__", "advancement_rate", "auto_attack", "charging_delay", "delay_type",
			"max_skill_point", "motion_mode", "penalty_type_succ", "pre_fx_delay", "prechain_skillname",
			"status_fx_slot", "status_fx_slot_lv", "status_sfx1", "system_fire_fx", "toggle_id", "use_arrow_count"),
			fieldNames);
	}

	private static Set<String> readRetailFieldNames(Path file) throws Exception {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		Set<String> fields = new HashSet<>();
		try (InputStream stream = Files.newInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("field")) {
					assertTrue(fields.add(reader.getAttributeValue(null, "name")), "retail field name must be unique");
				}
			}
			reader.close();
		}
		return fields;
	}

	private void writePart(String file, int id, String stack, String value, int occurrence) throws Exception {
		Files.writeString(directory.resolve(file), """
			<skill_data><skill_template skill_id="%d" name="test" nameId="1" stack="%s" skilltype="MAGICAL"
				skillsubtype="BUFF" activation="ACTIVE" duration="0">
				<group_ref id="G"/>%s<retail><f i="1" o="%d" v="%s"/></retail>
			</skill_template></skill_data>
			""".formatted(id, stack, id == 100 ? "<group_ref id=\"A\"/>" : "",
				occurrence, value));
	}
}
