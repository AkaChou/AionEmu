package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RepeatedAbnormalStatusImmuneData;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.NpcGameStats;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * 固化读取端语义：原版重复异常递减链在 PvP 下的档位（100/90/85/80/0% 与 0/0/200/400/1000）、
 * 出窗重置、NPC 来源只记不调、NPC 目标/ noresist / STUN 不参与，以及时长百分比的工程防护。
 * Pins the resist-phase semantics: retail tiers under PvP (100/90/85/80/0% and 0/0/200/400/1000),
 * window expiry reset, NPC casters record-only, NPC targets / noresist / STUN excluded, and the
 * engineering guard on the duration percent.
 */
class RepeatedAbnormalStatusImmuneEffectTest {

	private static final Path PRODUCTION_XML = Path
			.of("src/main/resources/aion/data/static_data/repeated_abnormal_status_immune.xml");
	/** 窗口基准时长（毫秒）：60 秒，远大于测试执行间隔 / window base duration: 60 s, far above test latency. */
	private static final int BASE_DURATION = 60_000;

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
	void freshChainMarksTheEffectWithoutAdjusting() {
		Player caster = newPlayer();
		Player target = newPlayer();
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		assertEquals(0, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertEquals(AbnormalState.SLEEP, effect.getRepeatedImmuneStatus());
		assertEquals(0, effect.getRepeatedImmuneStep());
		assertEquals(100, effect.getRepeatedImmuneDurationPercent());
	}

	@Test
	void firstTierKeepsFullDurationWithoutAddedResist() {
		Player caster = newPlayer();
		Player target = newPlayer();
		target.recordRepeatedAbnormalHit(AbnormalState.SLEEP, 1, System.currentTimeMillis());
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		assertEquals(0, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertEquals(1, effect.getRepeatedImmuneStep());
		assertEquals(100, effect.getRepeatedImmuneDurationPercent());
	}

	@Test
	void thirdTierShortensToNinetyPercent() {
		Player caster = newPlayer();
		Player target = newPlayer();
		target.recordRepeatedAbnormalHit(AbnormalState.SLEEP, 2, System.currentTimeMillis());
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		assertEquals(0, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertEquals(2, effect.getRepeatedImmuneStep());
		assertEquals(90, effect.getRepeatedImmuneDurationPercent());
	}

	@Test
	void fourthTierAddsTwoHundredResistAtEightyFivePercent() {
		Player caster = newPlayer();
		Player target = newPlayer();
		target.recordRepeatedAbnormalHit(AbnormalState.SLEEP, 3, System.currentTimeMillis());
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		assertEquals(200, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertEquals(85, effect.getRepeatedImmuneDurationPercent());
	}

	@Test
	void cappedTierAddsOneThousandResistAtZeroPercent() {
		Player caster = newPlayer();
		Player target = newPlayer();
		target.recordRepeatedAbnormalHit(AbnormalState.SLEEP, 5, System.currentTimeMillis());
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		assertEquals(1000, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertEquals(0, effect.getRepeatedImmuneDurationPercent());
	}

	@Test
	void freshChainAlwaysLandsThroughTheResistCheck() {
		Player caster = newPlayer();
		Player target = newPlayer();
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		// 抵抗值 0 → chance 0 → Rnd.get(1,1000) > 0 恒真 / zero resistance always passes the roll
		assertTrue(template.calculateEffectResistRate(effect, StatEnum.SLEEP_RESISTANCE));
	}

	@Test
	void cappedTierAlwaysResistsThroughTheResistCheck() {
		Player caster = newPlayer();
		Player target = newPlayer();
		target.recordRepeatedAbnormalHit(AbnormalState.SLEEP, 5, System.currentTimeMillis());
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		// +1000 追加抵抗 → chance clamp 到 1000 → 掷骰恒被抵抗 / +1000 clamps the chance to 1000, always resisted
		assertFalse(template.calculateEffectResistRate(effect, StatEnum.SLEEP_RESISTANCE));
	}

	@Test
	void expiredWindowRestartsTheChain() {
		Player caster = newPlayer();
		Player target = newPlayer();
		// 2 分钟前命中：超过 60 秒窗口 → 视为全新链 / hit two minutes ago: outside the 60 s window
		target.recordRepeatedAbnormalHit(AbnormalState.SLEEP, 3, System.currentTimeMillis() - 120_000);
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		assertEquals(0, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertEquals(0, effect.getRepeatedImmuneStep());
		assertEquals(100, effect.getRepeatedImmuneDurationPercent());
	}

	@Test
	void fearSharesTheChainButAddsItsHoldingTime() {
		Player caster = newPlayer();
		Player target = newPlayer();
		long now = System.currentTimeMillis();
		FearEffect template = new FearEffect();
		setField(template, EffectTemplate.class, "duration2", BASE_DURATION);

		// FEAR 窗口 = 60 秒 + 2 秒；90 秒前的命中对 FEAR 已出窗 / FEAR window is 60 s + 2 s; a 90 s old hit is out
		target.recordRepeatedAbnormalHit(AbnormalState.FEAR, 3, now - 90_000);
		Effect expired = new Effect(caster, target, skillWith(template), 1, 0);
		assertEquals(0, template.applyRepeatedAbnormalStatusImmune(expired, StatEnum.FEAR_RESISTANCE));
		assertEquals(0, expired.getRepeatedImmuneStep());

		// 30 秒前的命中仍在窗口内 / a 30 s old hit is still inside the window
		target.recordRepeatedAbnormalHit(AbnormalState.FEAR, 3, now - 30_000);
		Effect chained = new Effect(caster, target, skillWith(template), 1, 0);
		assertEquals(200, template.applyRepeatedAbnormalStatusImmune(chained, StatEnum.FEAR_RESISTANCE));
		assertEquals(85, chained.getRepeatedImmuneDurationPercent());
	}

	@Test
	void npcCasterRecordsButNeverAdjusts() {
		Npc caster = newNpc();
		Player target = newPlayer();
		target.recordRepeatedAbnormalHit(AbnormalState.SLEEP, 3, System.currentTimeMillis());
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		// NPC 来源不打折也不加抵抗，但要打标记（记录端不看施法者）/ NPC caster neither shortens nor adds resist, yet marks the effect
		assertEquals(0, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertEquals(3, effect.getRepeatedImmuneStep());
		assertEquals(100, effect.getRepeatedImmuneDurationPercent());
		assertEquals(AbnormalState.SLEEP, effect.getRepeatedImmuneStatus());
	}

	@Test
	void npcTargetTakesNoPart() {
		Player caster = newPlayer();
		Npc target = newNpc();
		SleepEffect template = sleepTemplate();
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		assertEquals(0, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertNull(effect.getRepeatedImmuneStatus());
	}

	@Test
	void noResistTemplateTakesNoPart() {
		Player caster = newPlayer();
		Player target = newPlayer();
		target.recordRepeatedAbnormalHit(AbnormalState.SLEEP, 3, System.currentTimeMillis());
		SleepEffect template = sleepTemplate();
		setField(template, EffectTemplate.class, "noResist", true);
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		assertEquals(0, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertNull(effect.getRepeatedImmuneStatus());
	}

	@Test
	void stunResistanceTakesNoPart() {
		Player caster = newPlayer();
		Player target = newPlayer();
		StunEffect template = new StunEffect();
		setField(template, EffectTemplate.class, "duration2", BASE_DURATION);
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);

		assertEquals(0, template.applyRepeatedAbnormalStatusImmune(effect, StatEnum.STUN_RESISTANCE));
		assertNull(effect.getRepeatedImmuneStatus());
	}

	@Test
	void sameCastKeepsTheFirstTrackedTemplateOnly() {
		Player caster = newPlayer();
		Player target = newPlayer();
		target.recordRepeatedAbnormalHit(AbnormalState.SLEEP, 3, System.currentTimeMillis());
		target.recordRepeatedAbnormalHit(AbnormalState.PARALYZE, 3, System.currentTimeMillis());
		SleepEffect sleep = sleepTemplate();
		ParalyzeEffect paralyze = new ParalyzeEffect();
		setField(paralyze, EffectTemplate.class, "duration2", BASE_DURATION);
		Effect effect = new Effect(caster, target, skillWith(sleep), 1, 0);

		assertEquals(200, sleep.applyRepeatedAbnormalStatusImmune(effect, StatEnum.SLEEP_RESISTANCE));
		assertEquals(0, paralyze.applyRepeatedAbnormalStatusImmune(effect, StatEnum.PARALYZE_RESISTANCE));
		assertEquals(AbnormalState.SLEEP, effect.getRepeatedImmuneStatus());
		assertEquals(85, effect.getRepeatedImmuneDurationPercent());
	}

	@Test
	void durationPercentMultipliesTheComputedDuration() {
		Player caster = newPlayer();
		Player target = newPlayer();
		SleepEffect template = new SleepEffect();
		setField(template, EffectTemplate.class, "duration2", 3_000);
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);
		effect.addAllEffectToSucess();

		setField(effect, "repeatedImmuneDurationPercent", 85);

		assertEquals(2_550, effect.getEffectsDuration());
	}

	@Test
	void zeroPercentTierKeepsOneMillisecondLifetime() {
		Player caster = newPlayer();
		Player target = newPlayer();
		SleepEffect template = new SleepEffect();
		setField(template, EffectTemplate.class, "duration2", 3_000);
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);
		effect.addAllEffectToSucess();

		setField(effect, "repeatedImmuneDurationPercent", 0);

		// 0% 档钳到 1ms：避免 startEffect 的 duration==0 早退留下永久异常 / 0% tier clamps to 1 ms
		assertEquals(1, effect.getEffectsDuration());
	}

	@Test
	void zeroBaseDurationStaysZero() {
		Player caster = newPlayer();
		Player target = newPlayer();
		SleepEffect template = new SleepEffect();
		setField(template, EffectTemplate.class, "duration2", 0);
		Effect effect = new Effect(caster, target, skillWith(template), 1, 0);
		effect.addAllEffectToSucess();

		setField(effect, "repeatedImmuneDurationPercent", 0);

		assertEquals(0, effect.getEffectsDuration());
	}

	// ===== 替身与工具 / stubs and helpers =====

	private Player newPlayer() {
		Player player = objenesis.newInstance(TestPlayer.class);
		player.setGameStats(new TestPlayerGameStats(player));
		return player;
	}

	private Npc newNpc() {
		Npc npc = objenesis.newInstance(Npc.class);
		npc.setGameStats(new NpcGameStats(npc));
		return npc;
	}

	private static SleepEffect sleepTemplate() {
		SleepEffect template = new SleepEffect();
		setField(template, EffectTemplate.class, "duration2", BASE_DURATION);
		return template;
	}

	private static SkillTemplate skillWith(EffectTemplate template) {
		SkillTemplate skill = new SkillTemplate();
		// 防 getEffectsDuration 的 subType 判空 NPE / avoid the subType switch NPE in getEffectsDuration
		setField(skill, SkillTemplate.class, "subType", SkillSubType.NONE);
		Effects effects = new Effects();
		effects.getEffects().add(template);
		setField(skill, SkillTemplate.class, "effects", effects);
		return skill;
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

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
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

	private static final class TestPlayerGameStats extends PlayerGameStats {

		private TestPlayerGameStats(Player owner) {
			super(owner);
		}

		@Override
		protected void onStatsChange() {
		}
	}
}
