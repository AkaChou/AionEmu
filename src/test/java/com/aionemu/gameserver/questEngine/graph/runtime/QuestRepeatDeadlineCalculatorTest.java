package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AnchoredCooldownRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DailyRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.NoRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatTimeBasis;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatWeekday;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.WeeklyRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

/**
 * 验证 repeat deadline 计算与当前 QuestService 的时间、时区和权限语义一致。
 * Verifies repeat-deadline parity with the current QuestService time, zone, and privilege semantics.
 */
class QuestRepeatDeadlineCalculatorTest {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DailyRepeatDeadlinePolicy DAILY = new DailyRepeatDeadlinePolicy(RepeatTimeBasis.SERVER_LOCAL, 9);

	/** 验证每日 09:00 前、整点和整点后的边界。 / Verifies daily boundaries before, at, and after 09:00. */
	@Test
	void dailyUsesLegacyNineOClockBoundary() {
		assertDeadline("2026-07-28T09:00+08:00[Asia/Shanghai]", DAILY, "2026-07-28T08:59+08:00[Asia/Shanghai]", SHANGHAI);
		assertDeadline("2026-07-28T09:00+08:00[Asia/Shanghai]", DAILY, "2026-07-28T09:00+08:00[Asia/Shanghai]", SHANGHAI);
		assertDeadline("2026-07-29T09:00+08:00[Asia/Shanghai]", DAILY, "2026-07-28T09:00:00.001+08:00[Asia/Shanghai]",
			SHANGHAI);
	}

	/** 验证 weekly 使用当前 Handler 的下一目标星期 rollover。 / Verifies the current Handler's next-target-weekday rollover. */
	@Test
	void weeklyUsesLegacyRollover() {
		WeeklyRepeatDeadlinePolicy wednesday = new WeeklyRepeatDeadlinePolicy(RepeatTimeBasis.SERVER_LOCAL, Set.of(RepeatWeekday.WED), 9);
		WeeklyRepeatDeadlinePolicy monday = new WeeklyRepeatDeadlinePolicy(RepeatTimeBasis.SERVER_LOCAL, Set.of(RepeatWeekday.MON), 9);
		assertDeadline("2026-07-29T09:00+08:00[Asia/Shanghai]", wednesday, "2026-07-28T12:00+08:00[Asia/Shanghai]", SHANGHAI);
		assertDeadline("2026-08-05T09:00+08:00[Asia/Shanghai]", wednesday, "2026-07-29T08:00+08:00[Asia/Shanghai]", SHANGHAI);
		assertDeadline("2026-08-03T09:00+08:00[Asia/Shanghai]", monday, "2026-08-02T12:00+08:00[Asia/Shanghai]", SHANGHAI);
	}

	/** 验证 1 小时、30 天和 60 天 cooldown 都从当日 09:00 锚点增加。 / Verifies 1-hour, 30-day, and 60-day cooldowns from the day's 09:00 anchor. */
	@Test
	void anchoredCooldownUsesLocalDayAnchor() {
		String event = "2026-07-28T20:00+08:00[Asia/Shanghai]";
		assertDeadline("2026-07-28T10:00+08:00[Asia/Shanghai]", cooldown(3_600), event, SHANGHAI);
		assertDeadline("2026-08-27T09:00+08:00[Asia/Shanghai]", cooldown(2_592_000), event, SHANGHAI);
		assertDeadline("2026-09-26T09:00+08:00[Asia/Shanghai]", cooldown(5_184_000), event, SHANGHAI);
	}

	/** 验证显式 ZoneId 及 DST 跨越仍复现 plusHours(24) 行为。 / Verifies explicit ZoneId and legacy plusHours(24) behavior across DST. */
	@Test
	void dailyUsesExplicitZoneAcrossDst() {
		ZoneId newYork = ZoneId.of("America/New_York");
		assertDeadline("2026-03-08T10:00-04:00[America/New_York]", DAILY,
			"2026-03-07T12:00-05:00[America/New_York]", newYork);
		assertDeadline("2026-03-08T09:00+08:00[Asia/Shanghai]", DAILY,
			"2026-03-07T12:00+08:00[Asia/Shanghai]", SHANGHAI);
	}

	/** 验证 daily/weekly 对 GM 绕过，而 cooldown 继续强制。 / Verifies GM bypass for daily/weekly while cooldown remains enforced. */
	@Test
	void privilegedBehaviorMatchesHandlerPrecedence() {
		long occurredAt = time("2026-07-28T12:00+08:00[Asia/Shanghai]");
		assertEquals(RepeatDeadlineResolution.PRIVILEGED_BYPASS,
			QuestRepeatDeadlineCalculator.calculate(DAILY, occurredAt, SHANGHAI, 1));
		assertEquals(RepeatDeadlineResolution.NOT_APPLICABLE,
			QuestRepeatDeadlineCalculator.calculate(NoRepeatDeadlinePolicy.INSTANCE, occurredAt, SHANGHAI, 1));
		assertDeadline("2026-08-27T09:00+08:00[Asia/Shanghai]", cooldown(2_592_000),
			"2026-07-28T12:00+08:00[Asia/Shanghai]", SHANGHAI, 1);
	}

	/** 验证算术溢出会显式失败。 / Verifies arithmetic overflow fails explicitly. */
	@Test
	void overflowFailsExplicitly() {
		assertThrows(RuntimeException.class, () -> QuestRepeatDeadlineCalculator.calculate(
			new AnchoredCooldownRepeatDeadlinePolicy(RepeatTimeBasis.SERVER_LOCAL, Long.MAX_VALUE, 9),
			time("2026-07-28T12:00+08:00[Asia/Shanghai]"), SHANGHAI, 0));
	}

	/** 创建 anchored cooldown 策略。 / Creates an anchored-cooldown policy. */
	private static AnchoredCooldownRepeatDeadlinePolicy cooldown(long seconds) {
		return new AnchoredCooldownRepeatDeadlinePolicy(RepeatTimeBasis.SERVER_LOCAL, seconds, 9);
	}

	/** 使用普通玩家权限断言绝对 deadline。 / Asserts an absolute deadline with normal-player privileges. */
	private static void assertDeadline(String expected, com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatDeadlinePolicy policy,
		String event, ZoneId zone) {
		assertDeadline(expected, policy, event, zone, 0);
	}

	/** 使用显式权限断言绝对 deadline。 / Asserts an absolute deadline with an explicit access level. */
	private static void assertDeadline(String expected, com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatDeadlinePolicy policy,
		String event, ZoneId zone, int accessLevel) {
		assertEquals(RepeatDeadlineResolution.deadline(time(expected)),
			QuestRepeatDeadlineCalculator.calculate(policy, time(event), zone, accessLevel));
	}

	/** 将带时区文本转换为 Unix 毫秒。 / Converts zoned text into Unix milliseconds. */
	private static long time(String value) {
		return ZonedDateTime.parse(value).toInstant().toEpochMilli();
	}
}
