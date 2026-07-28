package com.aionemu.gameserver.questEngine.graph.runtime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AnchoredCooldownRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DailyRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.NoRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatPrivilegeMode;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.WeeklyRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

/**
 * 根据显式策略、事件时间和服务器时区确定性计算下次可重复时间。
 * Deterministically calculates the next repeat time from an explicit policy, event time, and server zone.
 */
public final class QuestRepeatDeadlineCalculator {

	/** 禁止实例化纯静态计算器。 / Prevents instantiation of this static calculator. */
	private QuestRepeatDeadlineCalculator() {
	}

	/**
	 * 复现当前 Handler 的 daily、weekly 和 anchored cooldown 时间计算。
	 * Reproduces the current Handler's daily, weekly, and anchored-cooldown calculation.
	 *
	 * @return 可持久化的 deadline、权限绕过或不适用结果 / persistable deadline, privileged-bypass, or not-applicable result
	 */
	public static RepeatDeadlineResolution calculate(RepeatDeadlinePolicy policy, long occurredAt, ZoneId serverZoneId, int accessLevel) {
		Objects.requireNonNull(policy, "policy");
		Objects.requireNonNull(serverZoneId, "serverZoneId");
		if (occurredAt <= 0 || accessLevel < 0) {
			throw new IllegalArgumentException("Repeat deadline event time/access level is invalid");
		}
		if (policy == NoRepeatDeadlinePolicy.INSTANCE) {
			return RepeatDeadlineResolution.NOT_APPLICABLE;
		}
		if (accessLevel > 0 && policy.privilegeMode() == RepeatPrivilegeMode.BYPASS_FOR_PRIVILEGED) {
			return RepeatDeadlineResolution.PRIVILEGED_BYPASS;
		}
		ZonedDateTime now = Instant.ofEpochMilli(occurredAt).atZone(serverZoneId);
		ZonedDateTime deadline = switch (policy) {
			case DailyRepeatDeadlinePolicy daily -> daily(now, daily.resetHour());
			case WeeklyRepeatDeadlinePolicy weekly -> weekly(now, weekly);
			case AnchoredCooldownRepeatDeadlinePolicy cooldown -> anchor(now, cooldown.anchorHour())
				.plusSeconds(cooldown.cooldownSeconds());
			case NoRepeatDeadlinePolicy ignored -> throw new IllegalStateException("No-repeat policy was not handled");
		};
		return RepeatDeadlineResolution.deadline(deadline.toInstant().toEpochMilli());
	}

	/** 计算每日固定小时 deadline。 / Calculates a fixed-hour daily deadline. */
	private static ZonedDateTime daily(ZonedDateTime now, int resetHour) {
		ZonedDateTime deadline = anchor(now, resetHour);
		return now.isAfter(deadline) ? deadline.plusHours(24) : deadline;
	}

	/** 按旧 QuestService 的 weekday rollover 规则计算 deadline。 / Calculates a deadline with the legacy QuestService weekday rollover rules. */
	private static ZonedDateTime weekly(ZonedDateTime now, WeeklyRepeatDeadlinePolicy policy) {
		ZonedDateTime deadline = anchor(now, policy.resetHour());
		int daysToAdd = 7;
		int startDay = 7;
		for (var weekday : policy.weekdays()) {
			int dayValue = weekday.dayOfWeek();
			int difference = dayValue - deadline.getDayOfWeek().getValue();
			if (difference > 0 && difference < daysToAdd) {
				daysToAdd = difference;
			}
			if (startDay > dayValue) {
				startDay = dayValue;
			}
		}
		if (startDay == daysToAdd) {
			daysToAdd = 7;
		} else if (daysToAdd == 7 && startDay < 7) {
			daysToAdd = 7 - deadline.getDayOfWeek().getValue() + startDay;
		}
		return deadline.plusDays(daysToAdd);
	}

	/** 返回事件所在本地日期的固定小时锚点。 / Returns the fixed-hour anchor on the event's local date. */
	private static ZonedDateTime anchor(ZonedDateTime now, int hour) {
		return now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
	}
}
