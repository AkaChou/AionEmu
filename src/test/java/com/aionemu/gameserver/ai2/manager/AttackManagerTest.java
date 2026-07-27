package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.effect.SkillAttackInstantEffect;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import org.junit.jupiter.api.Test;

class AttackManagerTest {

	@Test
	void passiveImmobileStructureKeepsVisibleTarget() {
		assertTrue(AttackManager.shouldKeepTargetWhenImmobile(0, false));
		assertFalse(AttackManager.shouldKeepTargetWhenImmobile(1, false));
		assertFalse(AttackManager.shouldKeepTargetWhenImmobile(0, true));
	}

	@Test
	void utilitySkillsDoNotMakePassiveStructuresAttackers() {
		assertFalse(AttackManager.isOffensiveSkill(new TestSkillTemplate(SkillSubType.BUFF)));
		assertFalse(AttackManager.isOffensiveSkill(new TestSkillTemplate(SkillSubType.HEAL)));
		assertTrue(AttackManager.isOffensiveSkill(new TestSkillTemplate(SkillSubType.ATTACK)));
		assertTrue(AttackManager.isOffensiveSkill(
				new TestSkillTemplate(SkillSubType.NONE, new SkillAttackInstantEffect())));
	}

	private static final class TestSkillTemplate extends SkillTemplate {
		private TestSkillTemplate(SkillSubType subType, EffectTemplate... effects) {
			this.subType = subType;
			if (effects.length > 0) {
				this.effects = new Effects();
				this.effects.getEffects().addAll(List.of(effects));
			}
		}
	}
}
