package com.aionemu.gameserver.services.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.AttendType;
import com.aionemu.gameserver.model.templates.event.AtreianPassport;
import com.aionemu.gameserver.model.templates.event.AtreianPassportRewards;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT;
import com.aionemu.gameserver.persistence.entity.PlayerPassportPo;

class AtreianPassportServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
	private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 20, 10, 0, 0, 0, ZONE);

	@Test
	void completedSingleSlotPassportStaysClaimedAcrossPeriods() throws Exception {
		// 单格签到已永久完成：库内 last_stamp 即使早已跨日，也不能重新点亮领取按钮
		// A completed single-slot passport must remain unavailable even when its stored stamp is old
		AtreianPassport single = passport(9, AttendType.ANNIVERSARY, 1);
		PlayerPassportPo row = row(9, 1, timestamp(NOW.minusDays(2)));

		List<SM_ATREIAN_PASSPORT.Record> records =
			AtreianPassportService.buildRecords(List.of(single), Map.of(9, row), NOW);

		assertEquals(1, records.size());
		assertEquals(1, records.get(0).countCollected());
		assertTrue(records.get(0).hasCollected());
	}

	@Test
	void passportWithoutStoredRowReportsZeroAndNotClaimed() throws Exception {
		// 行缺失（例如插入失败）时不得显示成「今日已领」
		// A missing row (e.g. a failed insert) must not render as already claimed
		List<SM_ATREIAN_PASSPORT.Record> records =
			AtreianPassportService.buildRecords(List.of(passport(9, AttendType.ANNIVERSARY, 1)), Map.of(), NOW);

		assertEquals(0, records.get(0).countCollected());
		assertFalse(records.get(0).hasCollected());
	}

	@Test
	void unclaimedDayCarriesTheStoredCountAndOpenFlag() throws Exception {
		AtreianPassport basic = passport(8, AttendType.BASIC, 28);
		PlayerPassportPo row = row(8, 12, timestamp(NOW.minusDays(1)));

		List<SM_ATREIAN_PASSPORT.Record> records =
			AtreianPassportService.buildRecords(List.of(basic), Map.of(8, row), NOW);

		assertEquals(12, records.get(0).countCollected());
		assertFalse(records.get(0).hasCollected());
		assertEquals(AttendType.BASIC.getId(), records.get(0).attendType());
	}

	@Test
	void attendancePeriodChangesAtLocalNine() {
		ZonedDateTime beforeBoundary = ZonedDateTime.of(2026, 10, 21, 8, 59, 0, 0, ZONE);
		ZonedDateTime atBoundary = ZonedDateTime.of(2026, 10, 21, 9, 0, 0, 0, ZONE);
		Timestamp afterMidnightClaim = timestamp(ZonedDateTime.of(2026, 10, 21, 2, 0, 0, 0, ZONE));
		Timestamp morningClaim = timestamp(ZonedDateTime.of(2026, 10, 20, 10, 0, 0, 0, ZONE));

		assertTrue(AtreianPassportService.isClaimedInPeriod(afterMidnightClaim, beforeBoundary));
		assertFalse(AtreianPassportService.isClaimedInPeriod(afterMidnightClaim, atBoundary));
		assertTrue(AtreianPassportService.isClaimedInPeriod(morningClaim, beforeBoundary));
		assertFalse(AtreianPassportService.isClaimedInPeriod(morningClaim, atBoundary));
		assertFalse(AtreianPassportService.isClaimedInPeriod(
			timestamp(ZonedDateTime.of(2026, 10, 20, 23, 0, 0, 0, ZONE)), atBoundary));
	}

	@Test
	void previousPeriodSentinelIsOutsideTheCurrentNineOClockPeriod() {
		ZonedDateTime beforeBoundary = ZonedDateTime.of(2026, 10, 21, 8, 59, 0, 0, ZONE);
		ZonedDateTime afterBoundary = ZonedDateTime.of(2026, 10, 21, 9, 0, 0, 0, ZONE);

		assertFalse(AtreianPassportService.isClaimedInPeriod(
			AtreianPassportService.previousPeriodStamp(beforeBoundary), beforeBoundary));
		assertFalse(AtreianPassportService.isClaimedInPeriod(
			AtreianPassportService.previousPeriodStamp(afterBoundary), afterBoundary));
	}

	@Test
	void nextRewardStopsAtTheLastConfiguredSlot() throws Exception {
		AtreianPassport single = passport(9, AttendType.ANNIVERSARY, 1);
		AtreianPassportRewards first = single.getAtreianPassportRewards().get(0);

		assertSame(first, AtreianPassportService.nextReward(single, 0));
		assertNull(AtreianPassportService.nextReward(single, 1));
	}

	@Test
	void attendTypeIdsMatchTheRetailLoginEventEnum() {
		assertEquals(0, AttendType.ANNIVERSARY.getId());
		assertEquals(1, AttendType.BASIC.getId());
		assertEquals(2, AttendType.PC_BASIC.getId());
	}

	private static AtreianPassport passport(int id, AttendType type, int rewardCount) throws ReflectiveOperationException {
		AtreianPassport template = new AtreianPassport();
		setField(template, "id", id);
		setField(template, "attendType", type);
		List<AtreianPassportRewards> rewards = new ArrayList<>(rewardCount);
		for (int slot = 1; slot <= rewardCount; slot++) {
			AtreianPassportRewards reward = new AtreianPassportRewards();
			setField(reward, "rewardItem", 188053423);
			setField(reward, "rewardItemCount", 1);
			setField(reward, "rewardItemNum", slot);
			rewards.add(reward);
		}
		setField(template, "atreian_passport_reward", rewards);
		return template;
	}

	private static PlayerPassportPo row(int passportId, int stamps, Timestamp lastStamp) {
		PlayerPassportPo po = new PlayerPassportPo();
		po.setAccountId(1);
		po.setPassportId(passportId);
		po.setStamps(stamps);
		po.setLastStamp(lastStamp);
		return po;
	}

	private static Timestamp timestamp(ZonedDateTime dateTime) {
		return Timestamp.from(dateTime.toInstant());
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
