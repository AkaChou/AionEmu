package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.skillengine.condition.DualWeaponCondition;
import com.aionemu.gameserver.skillengine.condition.RideRobotCondition;
import com.aionemu.gameserver.skillengine.condition.SelfHideCondition;
import com.aionemu.gameserver.skillengine.condition.SkillGroupCondition;
import com.aionemu.gameserver.skillengine.effect.SpellAttackEffect;
import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;

class SkillTemplateTest {

	@Test
	void applyMcritDefaultsToTrue() {
		assertTrue(new SkillTemplate().isMcritApplied());
		assertTrue(new SkillTemplate().isMboostApplied());
		assertTrue(new SkillTemplate().isHealBoostApplied());
		assertTrue(new SkillTemplate().isMpHealBoostApplied());
	}

	@Test
	void unmarshalsApplyMcritAttribute() throws Exception {
		SkillTemplate template = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="MAGICAL" skillsubtype="ATTACK"
					activation="ACTIVE" duration="0" applymcrit="false" applymboost="false" applyhealboost="false"
					applymphealboost="false"/>
			</skill_data>
			""");

		assertFalse(template.isMcritApplied());
		assertFalse(template.isMboostApplied());
		assertFalse(template.isHealBoostApplied());
		assertFalse(template.isMpHealBoostApplied());
	}

	@Test
	void usesRetailMessageTypeWithoutChangingSkillType() throws Exception {
		SkillTemplate magicalMessage = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="PHYSICAL" type_message="MAGICAL"
					skillsubtype="ATTACK" activation="ACTIVE" duration="0"><effects/></skill_template>
			</skill_data>
			""");
		SkillTemplate inheritedMessage = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="PHYSICAL"
					skillsubtype="ATTACK" activation="ACTIVE" duration="0"><effects/></skill_template>
			</skill_data>
			""");

		Effect magical = new Effect(null, null, magicalMessage, 1, 0);
		Effect inherited = new Effect(null, null, inheritedMessage, 1, 0);
		magical.initialize();
		inherited.initialize();

		assertEquals(SkillType.PHYSICAL, magical.getSkillType());
		assertEquals(AttackStatus.RESIST, magical.getAttackStatus());
		assertEquals(AttackStatus.DODGE, inherited.getAttackStatus());
	}

	@Test
	void unmarshalsRetailLogoutPersistenceFlags() throws Exception {
		SkillData data = unmarshalData("""
			<skill_data>
				<skill_template skill_id="1" name="saved" nameId="1" delayId="77" skilltype="MAGICAL"
					skillsubtype="BUFF" activation="ACTIVE" duration="0" no_save_on_logout="true"
					spend_time_on_logout="true" remain_cooltime_on_login="true"/>
				<skill_template skill_id="2" name="not-saved" nameId="2" delayId="88" skilltype="MAGICAL"
					skillsubtype="BUFF" activation="ACTIVE" duration="0"/>
			</skill_data>
			""");

		SkillTemplate template = data.getSkillTemplate(1);
		assertTrue(template.isNoSaveOnLogout());
		assertTrue(template.isSpendTimeOnLogout());
		assertTrue(template.isRemainCooltimeOnLogin());
		assertTrue(data.shouldPersistCooldown(77));
		assertFalse(data.shouldPersistCooldown(88));
	}

	@Test
	void unmarshalsRetailCooldownDeltaAndStartConditions() throws Exception {
		SkillTemplate template = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="MAGICAL" skillsubtype="ATTACK"
						activation="ACTIVE" duration="0" cooldown_delta="-24" req_dispel_count="90"
						no_jump_cancel="true" obstacle="5">
					<startconditions><riderobot/><selfhide/><skillgroup value="SKILL_ALL_Highdeva_1"/></startconditions>
				</skill_template>
			</skill_data>
			""");

		assertEquals(-24, template.getCooldownDelta());
		assertEquals(90, template.getReqDispelCount());
		assertEquals(90, new Effect(null, null, template, 1, 0).getPower());
		assertTrue(template.isNoJumpCancel());
		assertEquals(5, template.getObstacle());
		assertTrue(template.getStartconditions().getConditions().stream().anyMatch(RideRobotCondition.class::isInstance));
		assertTrue(template.getStartconditions().getConditions().stream().anyMatch(SelfHideCondition.class::isInstance));
		SkillGroupCondition skillGroup = (SkillGroupCondition) template.getStartconditions().getConditions().stream()
			.filter(SkillGroupCondition.class::isInstance).findFirst().orElseThrow();
		assertEquals("SKILL_ALL_Highdeva_1", skillGroup.getValue());
	}

	@Test
	void delayTypeScalesCooldownByFinalAttackDelay() throws Exception {
		SkillTemplate scaled = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="PHYSICAL" skillsubtype="ATTACK"
					activation="ACTIVE" duration="0" cooldown="300" delay_type="1"/>
			</skill_data>
			""");
		SkillTemplate fixed = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="PHYSICAL" skillsubtype="ATTACK"
					activation="ACTIVE" duration="0" cooldown="300"/>
			</skill_data>
			""");

		assertEquals(450, scaled.scaleCooldownByAttackDelay(scaled.getCooldown(), 1500));
		assertEquals(300, fixed.scaleCooldownByAttackDelay(fixed.getCooldown(), 1500));
	}

	@Test
	void unmarshalsSpellAttackMagicalDefenseSwitch() throws Exception {
		SkillTemplate template = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="MAGICAL" skillsubtype="ATTACK"
					activation="ACTIVE" duration="0">
					<effects><spellatk checktime="1000" value="10" duration2="3000" mrresist="false"/></effects>
				</skill_template>
			</skill_data>
			""");

		SpellAttackEffect effect = (SpellAttackEffect) template.getEffects().getEffects().getFirst();
		assertFalse(effect.isMrResist());
	}

	@Test
	void defaultsHostileTypeToDirect() {
		assertEquals(HostileType.DIRECT, new SkillTemplate().getHostileType());
	}

	@Test
	void unmarshalsHostileType() throws Exception {
		SkillTemplate indirect = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="MAGICAL" skillsubtype="ATTACK"
					activation="ACTIVE" duration="0" hostile_type="INDIRECT"/>
			</skill_data>
			""");
		SkillTemplate none = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="MAGICAL" skillsubtype="ATTACK"
					activation="ACTIVE" duration="0" hostile_type="NONE"/>
			</skill_data>
			""");

		assertEquals(HostileType.INDIRECT, indirect.getHostileType());
		assertEquals(HostileType.NONE, none.getHostileType());
	}

	@Test
	void preservesRetailFieldsMissingFromLegacyTemplate() throws Exception {
		SkillTemplate template = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="MAGICAL" skillsubtype="ATTACK"
					activation="ACTIVE" duration="0" remove_flyend="true">
					<retail_fields>
						<field name="new_field" value="x"/>
						<field name="__type_desc__" occurrence="1" value="second"/>
					</retail_fields>
				</skill_template>
			</skill_data>
			""");

		assertTrue(template.isRemoveFlyEnd());
		assertEquals("x", template.getRetailFields().get("new_field"));
		assertEquals("second", template.getRetailFields().get("__type_desc__", 1));
	}

	@Test
	void unmarshalsDualWeaponCondition() throws Exception {
		SkillTemplate template = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="PHYSICAL" skillsubtype="ATTACK"
					activation="ACTIVE" duration="0">
					<startconditions><dualweapon/></startconditions>
				</skill_template>
			</skill_data>
			""");

		assertTrue(template.getStartconditions().getConditions().stream()
			.anyMatch(DualWeaponCondition.class::isInstance));
	}

	private static SkillTemplate unmarshal(String xml) throws Exception {
		return unmarshalData(xml).getSkillTemplate(1);
	}

	private static SkillData unmarshalData(String xml) throws Exception {
		return (SkillData) JAXBContext.newInstance(SkillData.class)
					.createUnmarshaller()
					.unmarshal(new StringReader(xml));
	}
}
