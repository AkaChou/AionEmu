package com.aionemu.gameserver.controllers.effect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RepeatedAbnormalStatusImmuneData;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 固化写入端语义：成功施加后按读取端步数 +1 落账、被拒/无标记不落账、非玩家 owner 无副作用。
 * Pins the recording-phase semantics: a successful application lands step + 1 from the resist-phase
 * read, rejected/unmarked effects never land, and non-player owners are side-effect free.
 */
class EffectControllerRepeatedStatusTest {

	private static final Path PRODUCTION_XML = Path
			.of("src/main/resources/aion/data/static_data/repeated_abnormal_status_immune.xml");
	private static final long WINDOW = 60_000;

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private RepeatedAbnormalStatusImmuneData previousTable;

	@BeforeEach
	void installProductionTable() throws Exception {
		previousTable = DataManager.REPEATED_ABNORMAL_STATUS_IMMUNE_DATA;
		DataManager.REPEATED_ABNORMAL_STATUS_IMMUNE_DATA = (RepeatedAbnormalStatusImmuneData) JAXBContext
				.newInstance(RepeatedAbnormalStatusImmuneData.class).createUnmarshaller().unmarshal(PRODUCTION_XML.toFile());
	}

	@AfterEach
	void restoreTable() {
		DataManager.REPEATED_ABNORMAL_STATUS_IMMUNE_DATA = previousTable;
	}

	@Test
	void recordsStepAfterSuccessfulApplication() {
		Player player = newPlayer();
		TestEffectController controller = new TestEffectController(player);
		TestEffect effect = effect(controller, "SLEEP_STACK", 1001, 1, 1, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
		setField(effect, Effect.class, "repeatedImmuneStatus", AbnormalState.SLEEP);
		setField(effect, Effect.class, "repeatedImmuneStep", 2);

		assertTrue(controller.addEffect(effect));
		// 读取端步数 2 → 成功施加后落账为 3 / resist-phase step 2 lands as step 3
		assertEquals(3, player.getRepeatedAbnormalStep(AbnormalState.SLEEP, System.currentTimeMillis(), WINDOW));
	}

	@Test
	void doesNotRecordWithoutMark() {
		Player player = newPlayer();
		TestEffectController controller = new TestEffectController(player);
		TestEffect effect = effect(controller, "PLAIN_STACK", 1002, 1, 1, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);

		assertTrue(controller.addEffect(effect));
		assertEquals(0, player.getRepeatedAbnormalStep(AbnormalState.SLEEP, System.currentTimeMillis(), WINDOW));
	}

	@Test
	void doesNotRecordWhenRejected() {
		Player player = newPlayer();
		TestEffectController controller = new TestEffectController(player);
		// 高 basicLvl 的既有同 effectId 效果占位 / an existing same-effectId effect with a higher basicLvl
		TestEffect keeper = effect(controller, "KEEPER", 2001, 7, 5, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
		assertTrue(controller.addEffect(keeper));
		TestEffect rejected = effect(controller, "REJECTED", 2002, 7, 1, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
		setField(rejected, Effect.class, "repeatedImmuneStatus", AbnormalState.PARALYZE);
		setField(rejected, Effect.class, "repeatedImmuneStep", 1);

		assertFalse(controller.addEffect(rejected));
		assertEquals(0, player.getRepeatedAbnormalStep(AbnormalState.PARALYZE, System.currentTimeMillis(), WINDOW));
	}

	@Test
	void npcOwnerIsIgnoredWithoutFailure() {
		TestEffectController controller = new TestEffectController(new TestCreature());
		TestEffect effect = effect(controller, "NPC_STACK", 1003, 1, 1, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
		setField(effect, Effect.class, "repeatedImmuneStatus", AbnormalState.FEAR);
		setField(effect, Effect.class, "repeatedImmuneStep", 2);

		assertDoesNotThrow(() -> assertTrue(controller.addEffect(effect)));
	}

	// ===== 替身与工具 / stubs and helpers =====

	private Player newPlayer() {
		return objenesis.newInstance(TestPlayer.class);
	}

	private static TestEffect effect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel, ActivationAttribute activationAttribute, SkillTargetSlot targetSlot) {
		SkillTemplate skillTemplate = skillTemplate(stack, skillId, activationAttribute, targetSlot);
		setField(skillTemplate, SkillTemplate.class, "effects", effects(effectId, basicLevel));
		return new TestEffect(controller, skillTemplate);
	}

	private static SkillTemplate skillTemplate(String stack, int skillId, ActivationAttribute activationAttribute,
			SkillTargetSlot targetSlot) {
		SkillTemplate skillTemplate = new SkillTemplate();
		setField(skillTemplate, SkillTemplate.class, "skillId", skillId);
		setField(skillTemplate, SkillTemplate.class, "stack", stack);
		setField(skillTemplate, SkillTemplate.class, "activationAttribute", activationAttribute);
		setField(skillTemplate, SkillTemplate.class, "subType", SkillSubType.NONE);
		setField(skillTemplate, SkillTemplate.class, "targetSlot", targetSlot);
		setField(skillTemplate, SkillTemplate.class, "dispelCategory", DispelCategoryType.BUFF);
		return skillTemplate;
	}

	private static Effects effects(int effectId, int basicLevel) {
		Effects effects = new Effects();
		effects.getEffects().add(new TestEffectTemplate(effectId, basicLevel));
		return effects;
	}

	private static void setField(Object target, Class<?> owner, String name, Object value) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class TestEffectController extends EffectController {

		private TestEffectController(Creature owner) {
			super(owner);
		}

		@Override
		public void broadCastEffects() {
		}
	}

	private static final class TestEffect extends Effect {

		private final TestEffectController controller;

		private TestEffect(TestEffectController controller, SkillTemplate skillTemplate) {
			super(null, null, skillTemplate, 1, 0);
			this.controller = controller;
		}

		@Override
		public synchronized void endEffect() {
			controller.clearEffect(this);
		}

		@Override
		public void startEffect(boolean restored) {
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

	private static final class TestPlayer extends Player {

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public byte isPlayer() {
			return 1;
		}

		@Override
		public byte getLevel() {
			return 1;
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
