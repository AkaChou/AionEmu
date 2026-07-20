package com.aionemu.gameserver.controllers.effect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.StigmaType;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;

class EffectControllerTest {

	@Test
	void rejectedEffectReportsThatItWasNotStarted() {
		TestEffectController controller = new TestEffectController();
		TestEffect existingEffect = passiveEffect(controller, "existing", 10, 1, 2);
		TestEffect rejectedEffect = passiveEffect(controller, "rejected", 11, 1, 1);

		assertTrue(controller.addEffect(existingEffect));
		assertFalse(controller.addEffect(rejectedEffect));
		assertTrue(existingEffect.started());
		assertFalse(rejectedEffect.started());
	}

	@Test
	void replacingPassiveEffectByEffectIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect oldEffect = passiveEffect(controller, "old", 1, 1);
		TestEffect unrelatedEffect = passiveEffect(controller, "unrelated", 2, 1);
		TestEffect replacementEffect = passiveEffect(controller, "replacement", 1, 1);

		controller.addEffect(oldEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.addEffect(replacementEffect));
		assertNull(controller.passiveEffect("old"));
		assertSame(unrelatedEffect, controller.passiveEffect("unrelated"));
		assertSame(replacementEffect, controller.passiveEffect("replacement"));
	}

	@Test
	void replacingAbnormalEffectByEffectIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect oldEffect = abnormalEffect(controller, "old", 10, 1, 1);
		TestEffect unrelatedEffect = abnormalEffect(controller, "unrelated", 11, 2, 1);
		TestEffect replacementEffect = abnormalEffect(controller, "replacement", 12, 1, 1);

		controller.addEffect(oldEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.addEffect(replacementEffect));
		assertNull(controller.abnormalEffect("old"));
		assertSame(unrelatedEffect, controller.abnormalEffect("unrelated"));
		assertSame(replacementEffect, controller.abnormalEffect("replacement"));
	}

	@Test
	void replacingAbnormalEffectWithSameStackEndsExistingEffect() {
		TestEffectController controller = new TestEffectController();
		TestEffect oldEffect = abnormalEffect(controller, "same", 10, 1, 1);
		TestEffect replacementEffect = abnormalEffect(controller, "same", 11, 2, 1);

		controller.addEffect(oldEffect);
		controller.addEffect(replacementEffect);

		assertTrue(oldEffect.ended());
		assertSame(replacementEffect, controller.abnormalEffect("same"));
	}

	@Test
	void effectEndedByConcurrentReplacementCannotStartAfterwards() throws Exception {
		TestCreature creature = new TestCreature();
		TestEffectController controller = new TestEffectController(creature);
		creature.setEffectController(controller);
		CountDownLatch oldStartEntered = new CountDownLatch(1);
		CountDownLatch continueOldStart = new CountDownLatch(1);
		LifecycleEffectTemplate oldTemplate = new LifecycleEffectTemplate(1);
		SkillTemplate oldSkill = skillTemplate("same", 10, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
		setField(oldSkill, "effects", effects(oldTemplate));
		DelayedStartEffect oldEffect = new DelayedStartEffect(creature, oldSkill, oldStartEntered, continueOldStart);
		oldEffect.addAllEffectToSucess();
		Effect replacement = new Effect(creature, creature,
			skillTemplateWithEffects("same", 11, new TestEffectTemplate(2, 1)), 1, 0);
		replacement.addAllEffectToSucess();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread addingOld = new Thread(() -> {
			try {
				controller.addEffect(oldEffect);
			} catch (Throwable t) {
				failure.set(t);
			}
		});

		addingOld.start();
		assertTrue(oldStartEntered.await(5, TimeUnit.SECONDS));
		try {
			assertTrue(controller.addEffect(replacement));
		} finally {
			continueOldStart.countDown();
		}
		addingOld.join(5000);

		assertFalse(addingOld.isAlive());
		assertNull(failure.get());
		assertTrue(oldEffect.isStopped());
		assertEquals(0, oldTemplate.startCount());
		replacement.endEffect();
	}

	@Test
	void clearingStaleEffectDoesNotRemoveReplacementWithSameStack() {
		TestEffectController controller = new TestEffectController();
		TestEffect staleEffect = abnormalEffect(controller, "same", 10, 1, 1);
		TestEffect replacementEffect = abnormalEffect(controller, "same", 11, 2, 1);

		controller.addEffect(staleEffect);
		controller.addEffect(replacementEffect);
		staleEffect.clearFromController();

		assertFalse(replacementEffect.ended());
		assertSame(replacementEffect, controller.abnormalEffect("same"));
	}

	@Test
	void removeEffectBySkillIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect matchingEffect = abnormalEffect(controller, "matching", 10, 1, 1);
		TestEffect unrelatedEffect = abnormalEffect(controller, "unrelated", 11, 2, 1);

		controller.addEffect(matchingEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.removeEffect(10));
		assertNull(controller.abnormalEffect("matching"));
		assertSame(unrelatedEffect, controller.abnormalEffect("unrelated"));
	}

	@Test
	void removePassiveEffectBySkillIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect matchingEffect = passiveEffect(controller, "matching", 10, 1, 1);
		TestEffect unrelatedEffect = passiveEffect(controller, "unrelated", 11, 2, 1);

		controller.addEffect(matchingEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.removePassiveEffect(10));
		assertNull(controller.passiveEffect("matching"));
		assertSame(unrelatedEffect, controller.passiveEffect("unrelated"));
	}

	@Test
	void removeNoshowEffectBySkillIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect matchingEffect = noshowEffect(controller, "matching", 10, 1, 1);
		TestEffect unrelatedEffect = noshowEffect(controller, "unrelated", 11, 2, 1);

		controller.addEffect(matchingEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.removeNoshowEffect(10));
		assertNull(controller.noshowEffect("matching"));
		assertSame(unrelatedEffect, controller.noshowEffect("unrelated"));
	}

	@Test
	void removeEffectByDispelCategoryToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect matchingEffect = abnormalEffect(controller, "matching", 10, 1, 1);
		TestEffect unrelatedEffect = abnormalEffect(controller, "unrelated", 11, 2, 1);

		controller.addEffect(matchingEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.removeEffectByDispelCat(DispelCategoryType.BUFF, SkillTargetSlot.BUFF, 1, 0, 1));
		assertNull(controller.abnormalEffect("matching"));
		assertSame(unrelatedEffect, controller.abnormalEffect("unrelated"));
	}

	@Test
	void retailCategoryAndPowerControlDispel() {
		TestEffectController controller = new TestEffectController();
		TestEffect deathSentence = effect(controller, "death-sentence", 18461, 1, 1,
			ActivationAttribute.ACTIVE, SkillTargetSlot.DEBUFF).withPower(90);
		TestEffect protectedDebuff = effect(controller, "protected", 10, 2, 1,
			ActivationAttribute.ACTIVE, SkillTargetSlot.DEBUFF);
		setField(deathSentence.getSkillTemplate(), "dispelCategory", DispelCategoryType.DEBUFF_PHYSICAL);
		setField(deathSentence.getSkillTemplate(), "reqDispelLevel", 1);
		setField(protectedDebuff.getSkillTemplate(), "dispelCategory", DispelCategoryType.NEVER);
		controller.addEffect(deathSentence);
		controller.addEffect(protectedDebuff);

		controller.removeEffectByDispelCat(DispelCategoryType.DEBUFF_PHYSICAL, SkillTargetSlot.DEBUFF, 2, 1, 20);

		assertSame(deathSentence, controller.abnormalEffect("death-sentence"));
		assertEquals(70, deathSentence.getPower());
		assertSame(protectedDebuff, controller.abnormalEffect("protected"));

		controller.removeEffectByDispelCat(DispelCategoryType.DEBUFF_PHYSICAL, SkillTargetSlot.DEBUFF, 2, 1, 70);

		assertNull(controller.abnormalEffect("death-sentence"));
		assertSame(protectedDebuff, controller.abnormalEffect("protected"));
	}

	@Test
	void dispelDoesNotRemoveReplacementAddedWhileOldEffectEnds() throws Exception {
		TestEffectController controller = new TestEffectController();
		CountDownLatch oldEffectCleared = new CountDownLatch(1);
		CountDownLatch finishOldEffect = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		TestEffect oldEffect = abnormalEffect(controller, "same", 10, 1, 1).afterClear(() -> {
			oldEffectCleared.countDown();
			try {
				finishOldEffect.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError(e);
			}
		});
		TestEffect replacementEffect = abnormalEffect(controller, "same", 11, 2, 1);
		controller.addEffect(oldEffect);

		Thread dispel = new Thread(() -> {
			try {
				controller.removeEffectByDispelCat(DispelCategoryType.BUFF, SkillTargetSlot.BUFF, 1, 0, 1);
			} catch (Throwable t) {
				failure.set(t);
			}
		});
		dispel.start();
		try {
			assertTrue(oldEffectCleared.await(5, TimeUnit.SECONDS));
			controller.addEffect(replacementEffect);
		} finally {
			finishOldEffect.countDown();
		}
		dispel.join(5000);

		assertFalse(dispel.isAlive());
		assertNull(failure.get());
		assertFalse(replacementEffect.ended());
		assertSame(replacementEffect, controller.abnormalEffect("same"));
	}

	@Test
	void filteredDispelHonorsLevelPowerAndRemovalLimit() {
		TestEffectController controller = new TestEffectController();
		TestEffect partiallyReduced = abnormalEffect(controller, "partial", 10, 1, 1).withPower(20);
		TestEffect removed = abnormalEffect(controller, "removed", 11, 2, 1).withPower(10);
		TestEffect forced = abnormalEffect(controller, "forced", 12, 3, 1).withPower(10);
		TestEffect unrelated = abnormalEffect(controller, "unrelated", 13, 4, 1).withPower(10);
		setField(partiallyReduced.getSkillTemplate(), "reqDispelLevel", 5);
		setField(removed.getSkillTemplate(), "reqDispelLevel", 5);
		setField(forced.getSkillTemplate(), "reqDispelLevel", 200);
		controller.addEffect(partiallyReduced);
		controller.addEffect(removed);
		controller.addEffect(forced);
		controller.addEffect(unrelated);

		assertEquals(1, controller.removeEffectsByDispel(effect -> effect.getSkillId() != 13, 1, 5, 10));
		assertEquals(10, partiallyReduced.getPower());
		assertSame(partiallyReduced, controller.abnormalEffect("partial"));
		assertNull(controller.abnormalEffect("removed"));
		assertSame(forced, controller.abnormalEffect("forced"));
		assertSame(unrelated, controller.abnormalEffect("unrelated"));

		assertEquals(1, controller.removeEffectsByDispel(effect -> effect.getSkillId() == 12, 1, 100, 10));
		assertNull(controller.abnormalEffect("forced"));
	}

	@Test
	void referenceLongDurationSkillRemainsDispellable() {
		TestEffectController controller = new TestEffectController();
		TestEffect effect = abnormalEffect(controller, "long", 21438, 1, 1, 86400000);
		setField(effect.getSkillTemplate(), "dispelCategory", DispelCategoryType.NPC_BUFF);
		controller.addEffect(effect);

		controller.removeEffectByDispelCat(DispelCategoryType.NPC_BUFF, SkillTargetSlot.BUFF, 1, 5, 50);

		assertNull(controller.abnormalEffect("long"));
	}

	@Test
	void npcDispelsMatchRetailCategoriesAndLevel() {
		TestEffectController controller = new TestEffectController();
		TestEffect physical = effect(controller, "physical", 10, 1, 1, ActivationAttribute.ACTIVE, SkillTargetSlot.DEBUFF);
		TestEffect mental = effect(controller, "mental", 11, 2, 1, ActivationAttribute.ACTIVE, SkillTargetSlot.DEBUFF);
		TestEffect playerDebuff = effect(controller, "player", 12, 3, 1, ActivationAttribute.ACTIVE, SkillTargetSlot.DEBUFF);
		TestEffect npcBuff = effect(controller, "npc-buff", 13, 4, 1, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
		TestEffect allBuff = effect(controller, "all-buff", 14, 5, 1, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
		setField(physical.getSkillTemplate(), "dispelCategory", DispelCategoryType.NPC_DEBUFF_PHYSICAL);
		setField(mental.getSkillTemplate(), "dispelCategory", DispelCategoryType.NPC_DEBUFF_MENTAL);
		setField(playerDebuff.getSkillTemplate(), "dispelCategory", DispelCategoryType.DEBUFF_PHYSICAL);
		setField(npcBuff.getSkillTemplate(), "dispelCategory", DispelCategoryType.NPC_BUFF);
		setField(npcBuff.getSkillTemplate(), "reqDispelLevel", 6);
		setField(allBuff.getSkillTemplate(), "dispelCategory", DispelCategoryType.ALL);
		controller.addEffect(physical);
		controller.addEffect(mental);
		controller.addEffect(playerDebuff);
		controller.addEffect(npcBuff);
		controller.addEffect(allBuff);

		controller.removeEffectByDispelCat(DispelCategoryType.NPC_DEBUFF, SkillTargetSlot.DEBUFF, 2, 5, 100);
		controller.removeEffectByDispelCat(DispelCategoryType.NPC_BUFF, SkillTargetSlot.BUFF, 1, 5, 100);
		controller.removeEffectByDispelCat(DispelCategoryType.BUFF, SkillTargetSlot.BUFF, 1, 5, 100);

		assertNull(controller.abnormalEffect("physical"));
		assertNull(controller.abnormalEffect("mental"));
		assertSame(playerDebuff, controller.abnormalEffect("player"));
		assertSame(npcBuff, controller.abnormalEffect("npc-buff"));
		assertNull(controller.abnormalEffect("all-buff"));

		controller.removeEffectByDispelCat(DispelCategoryType.NPC_BUFF, SkillTargetSlot.BUFF, 1, 6, 100);
		assertNull(controller.abnormalEffect("npc-buff"));
	}

	@Test
	void otherLongDurationSkillsRemainProtectedFromDispel() {
		TestEffectController controller = new TestEffectController();
		TestEffect effect = abnormalEffect(controller, "long", 21439, 1, 1, 86400000);
		controller.addEffect(effect);

		controller.removeEffectByDispelCat(DispelCategoryType.BUFF, SkillTargetSlot.BUFF, 1, 5, 50);

		assertSame(effect, controller.abnormalEffect("long"));
	}

	@Test
	void removeAllEffectsToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect firstEffect = abnormalEffect(controller, "first", 10, 1, 1);
		TestEffect secondEffect = abnormalEffect(controller, "second", 11, 2, 1);

		controller.addEffect(firstEffect);
		controller.addEffect(secondEffect);

		assertDoesNotThrow(() -> controller.removeAllEffects());
		assertNull(controller.abnormalEffect("first"));
		assertNull(controller.abnormalEffect("second"));
	}

	@Test
	void priorityStigmaChecksLaterEffectIdsBeforeAcceptingConflict() {
		TestEffectController controller = new TestEffectController();
		TestEffect existingEffect = stigmaEffect(controller, "existing", 10, StigmaType.BASIC, 10);
		TestEffect nextEffect = stigmaEffect(controller, "next", 11, StigmaType.ADVANCED, 20, 10);

		controller.addEffect(existingEffect);
		controller.addEffect(nextEffect);

		assertTrue(existingEffect.ended());
		assertSame(nextEffect, controller.abnormalEffect("next"));
	}

	@Test
	void addSavedEffectIgnoresMissingSkillTemplate() {
		SkillData originalSkillData = DataManager.SKILL_DATA;
		try {
			DataManager.SKILL_DATA = new SkillData();
			DataManager.SKILL_DATA.setSkillTemplates(Collections.emptyList());

			assertDoesNotThrow(() -> new PlayerEffectController(null).addSavedEffect(999999, 1, 1000,
					System.currentTimeMillis() + 1000));
		} finally {
			DataManager.SKILL_DATA = originalSkillData;
		}
	}

	@Test
	void restoredEffectHonorsRetailLogoutPersistenceFlags() {
		long now = 1_000_000;
		SkillTemplate noSave = new SkillTemplate();
		setField(noSave, "noSaveOnLogout", true);
		assertEquals(-1, PlayerEffectController.remainingTimeAfterLogout(noSave, 60_000, now + 30_000, now));

		SkillTemplate spending = new SkillTemplate();
		setField(spending, "spendTimeOnLogout", true);
		assertEquals(30_000,
			PlayerEffectController.remainingTimeAfterLogout(spending, 60_000, now + 30_000, now));

		assertEquals(60_000,
			PlayerEffectController.remainingTimeAfterLogout(new SkillTemplate(), 60_000, now - 1, now));
	}

	private static TestEffect abnormalEffect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel) {
		return abnormalEffect(controller, stack, skillId, effectId, basicLevel, 0);
	}

	private static TestEffect abnormalEffect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel, int duration) {
		return effect(controller, stack, skillId, effectId, basicLevel, ActivationAttribute.ACTIVE,
				SkillTargetSlot.BUFF, duration);
	}

	private static TestEffect passiveEffect(TestEffectController controller, String stack, int effectId, int basicLevel) {
		return passiveEffect(controller, stack, effectId, effectId, basicLevel);
	}

	private static TestEffect passiveEffect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel) {
		return effect(controller, stack, skillId, effectId, basicLevel, ActivationAttribute.PASSIVE, SkillTargetSlot.BUFF);
	}

	private static TestEffect noshowEffect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel) {
		return effect(controller, stack, skillId, effectId, basicLevel, ActivationAttribute.TOGGLE, SkillTargetSlot.NOSHOW);
	}

	private static TestEffect stigmaEffect(TestEffectController controller, String stack, int skillId,
			StigmaType stigmaType, int... effectIds) {
		SkillTemplate skillTemplate = skillTemplate(stack, skillId, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
		setField(skillTemplate, "stigmaType", stigmaType);
		setField(skillTemplate, "effects", effects(effectIds));
		return new TestEffect(controller, skillTemplate);
	}

	private static TestEffect effect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel, ActivationAttribute activationAttribute, SkillTargetSlot targetSlot) {
		return effect(controller, stack, skillId, effectId, basicLevel, activationAttribute, targetSlot, 0);
	}

	private static TestEffect effect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel, ActivationAttribute activationAttribute, SkillTargetSlot targetSlot, int duration) {
		SkillTemplate skillTemplate = skillTemplate(stack, skillId, activationAttribute, targetSlot);
		setField(skillTemplate, "effects", effects(effectId, basicLevel));
		return new TestEffect(controller, skillTemplate, duration);
	}

	private static SkillTemplate skillTemplate(String stack, int skillId, ActivationAttribute activationAttribute,
			SkillTargetSlot targetSlot) {
		SkillTemplate skillTemplate = new SkillTemplate();
		setField(skillTemplate, "skillId", skillId);
		setField(skillTemplate, "stack", stack);
		setField(skillTemplate, "activationAttribute", activationAttribute);
		setField(skillTemplate, "subType", SkillSubType.NONE);
		setField(skillTemplate, "targetSlot", targetSlot);
		setField(skillTemplate, "dispelCategory", DispelCategoryType.BUFF);
		return skillTemplate;
	}

	private static Effects effects(int effectId, int basicLevel) {
		Effects effects = new Effects();
		effects.getEffects().add(new TestEffectTemplate(effectId, basicLevel));
		return effects;
	}

	private static Effects effects(int... effectIds) {
		Effects effects = new Effects();
		for (int effectId : effectIds) {
			effects.getEffects().add(new TestEffectTemplate(effectId, 1));
		}
		return effects;
	}

	private static Effects effects(EffectTemplate effectTemplate) {
		Effects effects = new Effects();
		effects.getEffects().add(effectTemplate);
		return effects;
	}

	private static SkillTemplate skillTemplateWithEffects(String stack, int skillId, EffectTemplate effectTemplate) {
		SkillTemplate skillTemplate = skillTemplate(stack, skillId, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
		setField(skillTemplate, "effects", effects(effectTemplate));
		return skillTemplate;
	}

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class TestEffectController extends EffectController {

		private TestEffectController() {
			this(null);
		}

		private TestEffectController(Creature owner) {
			super(owner);
		}

		private Effect passiveEffect(String stack) {
			return passiveEffectMap.get(stack);
		}

		private Effect noshowEffect(String stack) {
			return noshowEffects.get(stack);
		}

		private Effect abnormalEffect(String stack) {
			return abnormalEffectMap.get(stack);
		}

		@Override
		public void broadCastEffects() {
		}
	}

	private static final class DelayedStartEffect extends Effect {

		private final CountDownLatch startEntered;
		private final CountDownLatch continueStart;

		private DelayedStartEffect(Creature creature, SkillTemplate skillTemplate, CountDownLatch startEntered,
				CountDownLatch continueStart) {
			super(creature, creature, skillTemplate, 1, 0);
			this.startEntered = startEntered;
			this.continueStart = continueStart;
		}

		@Override
		public void startEffect(boolean restored) {
			startEntered.countDown();
			try {
				continueStart.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError(e);
			}
			super.startEffect(restored);
		}
	}

	private static final class TestEffect extends Effect {

		private final TestEffectController controller;
		private boolean ended;
		private boolean started;
		private boolean useRealPower;
		private Runnable afterClear = () -> {
		};

		private TestEffect(TestEffectController controller, SkillTemplate skillTemplate) {
			this(controller, skillTemplate, 0);
		}

		private TestEffect(TestEffectController controller, SkillTemplate skillTemplate, int duration) {
			super(null, null, skillTemplate, 1, duration);
			this.controller = controller;
		}

		@Override
		public synchronized void endEffect() {
			ended = true;
			clearFromController();
			afterClear.run();
		}

		@Override
		public void startEffect(boolean restored) {
			started = true;
		}

		@Override
		public int removePower(int power) {
			return useRealPower ? super.removePower(power) : 0;
		}

		private TestEffect withPower(int power) {
			setPower(power);
			useRealPower = true;
			return this;
		}

		private TestEffect afterClear(Runnable action) {
			afterClear = action;
			return this;
		}

		private boolean ended() {
			return ended;
		}

		private boolean started() {
			return started;
		}

		private void clearFromController() {
			controller.clearEffect(this);
		}
	}

	private static final class TestEffectTemplate extends EffectTemplate {

		private TestEffectTemplate(int effectId, int basicLevel) {
			this.effectid = effectId;
			this.basicLvl = basicLevel;
		}

		@Override
		public void applyEffect(Effect effect) {
		}
	}

	private static final class LifecycleEffectTemplate extends EffectTemplate {

		private final AtomicInteger starts = new AtomicInteger();

		private LifecycleEffectTemplate(int effectId) {
			this.effectid = effectId;
			this.basicLvl = 1;
		}

		@Override
		public void applyEffect(Effect effect) {
		}

		@Override
		public void startEffect(Effect effect) {
			starts.incrementAndGet();
		}

		private int startCount() {
			return starts.get();
		}
	}

	private static final class TestCreature extends Creature {

		private TestCreature() {
			super(1, new CreatureController<>() {}, null, new TestVisibleObjectTemplate(), new WorldPosition(1));
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}

	private static final class TestVisibleObjectTemplate extends VisibleObjectTemplate {

		@Override
		public int getTemplateId() {
			return 1;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public int getNameId() {
			return 1;
		}
	}
}
