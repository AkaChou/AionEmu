package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.skillengine.effect.SkillAttackInstantEffect;
import com.aionemu.gameserver.skillengine.effect.modifier.ActionModifier;
import com.aionemu.gameserver.skillengine.effect.modifier.TargetRaceDamageModifier;

/**
 * 验证天族训练攻城兵器绑定的攻击技能能覆盖城门距离并命中城门种族。
 * Verifies that the Elyos training siege weapon's attack skill covers gate range and gate races.
 */
class SiegeWeaponGateAttackTest {
	private static final Path SKILLS_DIRECTORY = Path.of("src/main/resources/aion/definitions/compact/skills");

	@Test
	void bindsTrainingSiegeWeaponToReachableGateDamage() throws Exception {
		NpcSkillData npcSkillData = NpcSkillDefinitionLoader.load(
			SKILLS_DIRECTORY.resolve("npc-skills.xml").toFile());
		assertEquals(18008, npcSkillData.getNpcSkillList(201054).getNpcSkills().getFirst().getSkillid());

		SkillData skillData = SkillDefinitionLoader.load(SKILLS_DIRECTORY.toFile());
		var skill = skillData.getSkillTemplate(18008);
		assertNotNull(skill);
		assertEquals(15, skill.getProperties().getFirstTargetRange());

		assertEquals(1, skill.getEffects().getEffects().size());
		assertSame(SkillAttackInstantEffect.class, skill.getEffects().getEffects().getFirst().getClass());
		SkillAttackInstantEffect effect = (SkillAttackInstantEffect) skill.getEffects().getEffects().getFirst();
		assertEquals(100, effect.getValue());
		assertEquals(Set.of(Race.PC_LIGHT_CASTLE_DOOR, Race.PC_DARK_CASTLE_DOOR, Race.DRAGON_CASTLE_DOOR),
			gateRaces(effect.getModifiers().getActionModifiers()));
	}

	private static Set<Race> gateRaces(List<ActionModifier> modifiers) throws Exception {
		Field raceField = TargetRaceDamageModifier.class.getDeclaredField("skillTargetRace");
		raceField.setAccessible(true);
		return modifiers.stream()
			.map(TargetRaceDamageModifier.class::cast)
			.map(modifier -> {
				try {
					return (Race) raceField.get(modifier);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException("unable to read siege damage target race", e);
				}
			})
			.collect(Collectors.toUnmodifiableSet());
	}
}
