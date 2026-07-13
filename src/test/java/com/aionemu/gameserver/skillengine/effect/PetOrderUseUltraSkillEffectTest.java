package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PetOrderUseUltraSkillEffectTest {

	@Test
	void resolvesRetailUltraIndexAndKeepsOldTemplatesCompatible() {
		PetOrderUseUltraSkillEffect effect = new PetOrderUseUltraSkillEffect();
		effect.ultraSkill = 5;
		assertEquals(3835, effect.resolveOrderSkillId(11608));

		effect.ultraSkill = 0;
		assertEquals(11608, effect.resolveOrderSkillId(11608));
	}
}
