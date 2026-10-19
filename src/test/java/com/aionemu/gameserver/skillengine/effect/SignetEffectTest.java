package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class SignetEffectTest {

	@Test
	void usesRetailTypeLevelAndFamilyBaseSkillId() throws ReflectiveOperationException {
		SignetEffect signet = new SignetEffect();
		setField(signet, "signetType", 6);
		setField(signet, "signetLevel", 2);

		assertEquals(6, signet.getSignetType());
		assertEquals(2, signet.getSignetLevel());
		assertEquals(8456, CarveSignetEffect.nextSignetSkillId(8458, 5, 3));
	}

	@Test
	void advancesSignetLevelOneStepAtATime() {
		// 猛兽的咆哮（3417）：步进 1、上限 5，逐层 1→2→3→4→5 且封顶
		// TigerFang (3417): hop 1, cap 5 — climbs 1→2→3→4→5 and then holds
		assertEquals(1, CarveSignetEffect.nextSignetLevel(0, 5, 1));
		assertEquals(2, CarveSignetEffect.nextSignetLevel(1, 5, 1));
		assertEquals(4, CarveSignetEffect.nextSignetLevel(3, 5, 1));
		assertEquals(5, CarveSignetEffect.nextSignetLevel(4, 5, 1));
		assertEquals(5, CarveSignetEffect.nextSignetLevel(5, 5, 1));
	}

	@Test
	void jumpsByRetailCarveHopAndCapsAtMaxLevel() {
		// 真端步进 3：3 + 3 封顶于 5 / retail hop 3: 3 + 3 capped at 5
		assertEquals(5, CarveSignetEffect.nextSignetLevel(3, 5, 3));
		// 真端步进 5：0 + 5 一步直达上限 / retail hop 5: 0 + 5 lands on the cap in one cast
		assertEquals(5, CarveSignetEffect.nextSignetLevel(0, 5, 5));
		// 真端步进 2：1 + 2 / retail hop 2: 1 + 2
		assertEquals(3, CarveSignetEffect.nextSignetLevel(1, 5, 2));
	}

	@Test
	void floorsSignetLevelAtOne() {
		// 步进为 0 时仍须落在合法的最低层，避免解析出 8302 这类不存在的印记技能
		// A zero hop still lands on the lowest legal level, avoiding skill ids such as the non-existent 8302
		assertEquals(1, CarveSignetEffect.nextSignetLevel(0, 5, 0));
	}

	@Test
	void usesRetailBurstDamageBySignetTypeAndLevel() {
		assertEquals(20, SignetBurstEffect.getBurstDamagePercent(1, 1));
		assertEquals(30, SignetBurstEffect.getBurstDamagePercent(2, 1));
		assertEquals(75, SignetBurstEffect.getBurstDamagePercent(5, 2));
		assertEquals(100, SignetBurstEffect.getBurstDamagePercent(6, 2));
		assertEquals(400, SignetBurstEffect.scaleBurstDamage(475, 100));
		assertEquals(900, SignetBurstEffect.scaleBurstDamage(1265, 75));
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
