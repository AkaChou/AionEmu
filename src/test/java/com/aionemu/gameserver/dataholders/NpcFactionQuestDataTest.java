package com.aionemu.gameserver.dataholders;

import java.nio.file.Path;
import java.util.Calendar;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.xml.bind.JAXBContext;

import com.aionemu.gameserver.model.templates.factions.NpcFactionTemplate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcFactionQuestDataTest {
	private static final Path DATA = Path.of(
		"src/main/resources/aion/data/static_data/npc_factions/npc_factions_quest.xml");

	@Test
	void retailSnapshotContainsAll425QuestWeekdayEntries() throws Exception {
		assertEquals(425, load().size());
	}

	@Test
	void disabledQuestsKeepAllWeekdayBitsAtZero() throws Exception {
		NpcFactionQuestData data = load();
		for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
			assertFalse(data.isActiveOn(37000, day), "37000 must stay disabled on " + day);
			assertFalse(data.isActiveOn(37500, day), "37500 must stay disabled on " + day);
		}
	}

	@Test
	void weekendOnlyQuestActivatesOnlyOnSaturdayAndSunday() throws Exception {
		NpcFactionQuestData data = load();
		assertTrue(data.isActiveOn(36545, Calendar.SATURDAY));
		assertTrue(data.isActiveOn(36545, Calendar.SUNDAY));
		assertFalse(data.isActiveOn(36545, Calendar.MONDAY));
		assertFalse(data.isActiveOn(36545, Calendar.WEDNESDAY));
	}

	@Test
	void mondayOnlyQuestActivatesOnlyOnMonday() throws Exception {
		NpcFactionQuestData data = load();
		assertTrue(data.isActiveOn(9521, Calendar.MONDAY));
		for (int day : new int[] { Calendar.SUNDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
				Calendar.FRIDAY, Calendar.SATURDAY }) {
			assertFalse(data.isActiveOn(9521, day), "9521 must stay inactive on " + day);
		}
	}

	@Test
	void unlistedQuestsRemainActiveOnEveryDayForBackwardCompatibility() throws Exception {
		NpcFactionQuestData data = load();
		for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
			assertTrue(data.isActiveOn(1002, day), "unlisted quest must stay active on " + day);
		}
	}

	@Test
	void calendarDayOfWeekMappingFollowsCalendarConstants() throws Exception {
		NpcFactionQuestData data = load();
		// 1=SUNDAY..7=SATURDAY 映射到 sun..mon；星期位以零售快照为准
		assertTrue(data.isActiveOn(36545, Calendar.SUNDAY));
		assertTrue(data.isActiveOn(9521, Calendar.MONDAY));
	}

	@Test
	void retailWeekdaySnapshotCoversEveryFactionPresentInNpcFactionsData() throws Exception {
		NpcFactionQuestData questData = load();
		NpcFactionsData factionsData = (NpcFactionsData) JAXBContext.newInstance(NpcFactionsData.class)
			.createUnmarshaller().unmarshal(
				Path.of("src/main/resources/aion/data/static_data/npc_factions/npc_factions.xml").toFile());
		Set<Integer> knownFactionIds = factionsData.getNpcFactionsData().stream()
			.map(NpcFactionTemplate::getId).collect(Collectors.toSet());
		Set<Integer> referencedFactionIds = questData.getEntries().stream()
			.map(NpcFactionQuestData.NpcFactionQuestEntry::getFactionId).collect(Collectors.toSet());
		// 真实快照还含 10-13（无 AionEmu 模板的开发势力，其任务永远不会被发放），
		// 因此只要求快照覆盖 AionEmu 全部已注册势力。
		assertTrue(referencedFactionIds.containsAll(knownFactionIds));
	}

	private static NpcFactionQuestData load() throws Exception {
		return (NpcFactionQuestData) JAXBContext.newInstance(NpcFactionQuestData.class)
			.createUnmarshaller().unmarshal(DATA.toFile());
	}
}
