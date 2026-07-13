package com.aionemu.gameserver.skillengine.model;

import com.aionemu.gameserver.ai2.AITemplate;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.world.knownlist.KnownList;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillSpelledEventTest {

	@Test
	void notifiesNpcOnceForMultipleSuccessfulEffects() {
		SkillTemplate template = skillTemplate(1234);
		ObjenesisStd objenesis = new ObjenesisStd();
		TestCreature caster = objenesis.newInstance(TestCreature.class);
		Npc target = objenesis.newInstance(Npc.class);
		RecordingAI ai = new RecordingAI();
		target.setAi2(ai);
		caster.setKnownlist(new KnownList(caster));
		target.setKnownlist(new KnownList(target));
		Skill skill = new Skill(template, caster, 3, target, null);
		Effect effect = new Effect(caster, target, template, 3, 0);
		effect.addSucessEffect(new NoOpEffect());
		effect.addSucessEffect(new NoOpEffect());

		skill.applyEffect(List.of(effect));

		assertEquals(1, ai.calls);
		assertSame(caster, ai.caster);
		assertEquals(1234, ai.skillId);
		assertEquals(3, ai.skillLevel);
	}

	@Test
	void broadcastsSuccessfulSkillToNearbyNpc() {
		SkillTemplate template = skillTemplate(1234);
		ObjenesisStd objenesis = new ObjenesisStd();
		TestCreature caster = objenesis.newInstance(TestCreature.class);
		TestCreature target = objenesis.newInstance(TestCreature.class);
		Npc observer = objenesis.newInstance(Npc.class);
		RecordingAI observerAi = new RecordingAI();
		observer.setAi2(observerAi);
		caster.setKnownlist(new KnownList(caster));
		caster.getKnownList().getKnownObjects().put(observer.getObjectId(), observer);
		target.setKnownlist(new KnownList(target));
		target.getKnownList().getKnownObjects().put(observer.getObjectId(), observer);
		Skill skill = new Skill(template, caster, 3, target, null);
		Effect effect = new Effect(caster, target, template, 3, 0);
		effect.addSucessEffect(new NoOpEffect());

		skill.applyEffect(List.of(effect));

		assertEquals(1, observerAi.friendSpelledCalls);
		assertSame(caster, observerAi.caster);
		assertSame(target, observerAi.friend);
		assertEquals(1, observerAi.seeSpellCalls);
	}

	@Test
	void detectsFullyDodgedNpcSkill() {
		SkillTemplate template = skillTemplate(1234);
		ObjenesisStd objenesis = new ObjenesisStd();
		TestCreature caster = objenesis.newInstance(TestCreature.class);
		Npc target = objenesis.newInstance(Npc.class);
		Effect effect = new Effect(caster, target, template, 3, 0);
		effect.setAttackStatus(AttackStatus.OFFHAND_DODGE);

		assertTrue(Skill.isFullyDodgedNpc(effect));
		effect.setAttackStatus(AttackStatus.NORMALHIT);
		assertFalse(Skill.isFullyDodgedNpc(effect));
		effect.setAttackStatus(AttackStatus.DODGE);
		effect.addSucessEffect(new NoOpEffect());
		assertFalse(Skill.isFullyDodgedNpc(effect));
	}

	private static SkillTemplate skillTemplate(int skillId) {
		SkillTemplate template = new SkillTemplate();
		setField(template, "skillId", skillId);
		setField(template, "activationAttribute", ActivationAttribute.ACTIVE);
		return template;
	}

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = SkillTemplate.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class NoOpEffect extends EffectTemplate {

		@Override
		public void applyEffect(Effect effect) {
		}
	}

	private static final class RecordingAI extends AITemplate {

		private int calls;
		private Creature caster;
		private int skillId;
		private int skillLevel;
		private int friendSpelledCalls;
		private Creature friend;
		private int seeSpellCalls;
		@Override
		public void onSpelled(Creature caster, int skillId, int skillLevel) {
			calls++;
			this.caster = caster;
			this.skillId = skillId;
			this.skillLevel = skillLevel;
		}

		@Override
		public void onFriendSpelled(Creature caster, Creature friend, int skillId, int skillLevel) {
			friendSpelledCalls++;
			this.caster = caster;
			this.friend = friend;
		}

		@Override
		public void onSeeSpell(Creature caster, Creature target, int skillId, int skillLevel) {
			seeSpellCalls++;
		}
	}

	private static final class TestCreature extends Creature {

		@SuppressWarnings("unused")
		private TestCreature() {
			super(1, null, null, null, null);
		}

		@Override
		public String getName() {
			return "caster";
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}

}
