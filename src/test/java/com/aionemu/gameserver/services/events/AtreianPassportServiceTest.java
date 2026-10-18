package com.aionemu.gameserver.services.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.AttendType;
import com.aionemu.gameserver.model.templates.event.AtreianPassport;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT;
import com.aionemu.gameserver.persistence.entity.PlayerPassportPo;

class AtreianPassportServiceTest {

	@Test
	void recordCountComesFromStoredStampsNotFromTheClaimIntent() throws Exception {
		// 单格签到已领满后再次点击：未命中奖励格不写库，回发计数必须仍是库里的 1
		// A single-slot passport already claimed: a miss writes nothing, so the echoed count must stay 1
		AtreianPassport single = passport(9, AttendType.ANNIVERSARY);
		PlayerPassportPo row = row(9, 1, Timestamp.valueOf(LocalDate.now().atTime(9, 0)));

		List<SM_ATREIAN_PASSPORT.Record> records = AtreianPassportService.buildRecords(List.of(single), Map.of(9, row));

		assertEquals(1, records.size());
		assertEquals(1, records.get(0).countCollected());
		assertTrue(records.get(0).hasCollected());
	}

	@Test
	void passportWithoutStoredRowReportsZeroAndNotClaimed() throws Exception {
		// 行缺失（例如插入失败）时不得显示成「今日已领」
		// A missing row (e.g. a failed insert) must not render as already claimed
		List<SM_ATREIAN_PASSPORT.Record> records =
			AtreianPassportService.buildRecords(List.of(passport(9, AttendType.ANNIVERSARY)), Map.of());

		assertEquals(0, records.get(0).countCollected());
		assertFalse(records.get(0).hasCollected());
	}

	@Test
	void unclaimedDayCarriesTheStoredCountAndOpenFlag() throws Exception {
		AtreianPassport basic = passport(8, AttendType.BASIC);
		PlayerPassportPo row = row(8, 12, Timestamp.valueOf(LocalDate.now().minusDays(1).atTime(23, 0)));

		List<SM_ATREIAN_PASSPORT.Record> records = AtreianPassportService.buildRecords(List.of(basic), Map.of(8, row));

		assertEquals(12, records.get(0).countCollected());
		assertFalse(records.get(0).hasCollected());
		assertEquals(AttendType.BASIC.getId(), records.get(0).attendType());
	}

	@Test
	void attendTypeIdsMatchTheRetailLoginEventEnum() {
		assertEquals(0, AttendType.ANNIVERSARY.getId());
		assertEquals(1, AttendType.BASIC.getId());
		assertEquals(2, AttendType.PC_BASIC.getId());
	}

	private static AtreianPassport passport(int id, AttendType type) throws ReflectiveOperationException {
		AtreianPassport template = new AtreianPassport();
		setField(template, "id", id);
		setField(template, "attendType", type);
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

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
