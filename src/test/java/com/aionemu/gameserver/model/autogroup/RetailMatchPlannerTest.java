package com.aionemu.gameserver.model.autogroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.autogroup.RetailMatchPlanner.Assignment;
import com.aionemu.gameserver.model.autogroup.RetailMatchPlanner.Member;
import com.aionemu.gameserver.model.autogroup.RetailMatchPlanner.Party;
import com.aionemu.gameserver.model.autogroup.RetailMatchPlanner.Plan;

class RetailMatchPlannerTest {
	private static RetailInstanceData previous;

	@BeforeAll
	static void loadRetailData() {
		previous = DataManager.RETAIL_INSTANCE_DATA;
		DataManager.RETAIL_INSTANCE_DATA = RetailInstanceData.load(
				new File("src/main/resources/aion/definitions/compact/instance"),
				new File("src/main/resources/aion/definitions/schemas/retail-instance-data.xsd"));
	}

	@AfterAll
	static void restoreRetailData() {
		DataManager.RETAIL_INSTANCE_DATA = previous;
	}

	@Test
	void keepsFifoOrderForCompatibleSoloPlayers() {
		MatchDefinition definition = MatchDefinition.getByMaskId(40);
		Plan plan = RetailMatchPlanner.draft(definition, List.of(
				party(1, 0, EntryRequestType.NEW_GROUP_ENTRY, member(1, PlayerClass.GLADIATOR, Race.ELYOS)),
				party(2, 1, EntryRequestType.NEW_GROUP_ENTRY, member(2, PlayerClass.RANGER, Race.ASMODIANS)),
				party(3, 2, EntryRequestType.NEW_GROUP_ENTRY, member(3, PlayerClass.CLERIC, Race.ELYOS))),
				List.of(), 10, true);

		assertEquals(List.of(1, 2), plan.assignments().stream().map(assignment -> assignment.member().playerId()).toList());
	}

	@Test
	void assignsAtomicFullGroupsToRetailRaceSides() {
		MatchDefinition definition = MatchDefinition.getByMaskId(41);
		Party elyos = party(1, 0, EntryRequestType.GROUP_ENTRY,
				member(1, PlayerClass.GLADIATOR, Race.ELYOS), member(2, PlayerClass.CLERIC, Race.ELYOS),
				member(3, PlayerClass.RANGER, Race.ELYOS));
		Party asmodians = party(2, 0, EntryRequestType.GROUP_ENTRY,
				member(4, PlayerClass.GLADIATOR, Race.ASMODIANS), member(5, PlayerClass.CLERIC, Race.ASMODIANS),
				member(6, PlayerClass.RANGER, Race.ASMODIANS));

		Plan plan = RetailMatchPlanner.draft(definition, List.of(elyos, asmodians), List.of(), 10, true);

		assertEquals(6, plan.assignments().size());
		assertTrue(plan.assignments().stream().filter(assignment -> assignment.member().race() == Race.ELYOS)
				.allMatch(assignment -> assignment.side() == 0));
		assertTrue(plan.assignments().stream().filter(assignment -> assignment.member().race() == Race.ASMODIANS)
				.allMatch(assignment -> assignment.side() == 1));
	}

	@Test
	void enforcesClassMaximumAndExpandsShufflePool() {
		MatchDefinition definition = MatchDefinition.getByMaskId(40);
		List<Party> queue = List.of(
				party(1, 0, EntryRequestType.NEW_GROUP_ENTRY, member(1, PlayerClass.SONGWEAVER, Race.ELYOS)),
				party(2, 0, EntryRequestType.NEW_GROUP_ENTRY, member(2, PlayerClass.GLADIATOR, Race.ELYOS)),
				party(3, 0, EntryRequestType.NEW_GROUP_ENTRY, member(3, PlayerClass.RANGER, Race.ELYOS)));

		assertTrue(RetailMatchPlanner.draft(definition, queue, List.of(), 29_999, true).isEmpty());
		Plan expanded = RetailMatchPlanner.draft(definition, queue, List.of(), 30_000, true);
		assertEquals(List.of(2, 3), expanded.assignments().stream()
				.map(assignment -> assignment.member().playerId()).toList());
	}

	@Test
	void relaxesRequiredClassOnlyAfterAgeRequisite() {
		MatchDefinition definition = MatchDefinition.getByMaskId(1);
		List<Party> queue = new ArrayList<>();
		int id = 1;
		for (Race race : List.of(Race.ELYOS, Race.ASMODIANS)) {
			for (PlayerClass playerClass : List.of(PlayerClass.GLADIATOR, PlayerClass.GLADIATOR,
					PlayerClass.TEMPLAR, PlayerClass.RANGER, PlayerClass.SORCERER, PlayerClass.SPIRIT_MASTER)) {
				queue.add(party(id, 0, EntryRequestType.NEW_GROUP_ENTRY, member(id, playerClass, race)));
				id++;
			}
		}

		assertTrue(RetailMatchPlanner.draft(definition, queue, List.of(), 1_799_999, true).isEmpty());
		assertFalse(RetailMatchPlanner.draft(definition, queue, List.of(), 1_800_000, true).isEmpty());
	}

	@Test
	void fillsExistingInstanceWithoutRequiringFreshMinimum() {
		MatchDefinition definition = MatchDefinition.getByMaskId(40);
		Member first = member(1, PlayerClass.GLADIATOR, Race.ELYOS);
		List<Assignment> existing = List.of(new Assignment(first, (byte) 0));
		Party quick = party(2, 0, EntryRequestType.FAST_GROUP_ENTRY, member(2, PlayerClass.RANGER, Race.ASMODIANS));

		Plan plan = RetailMatchPlanner.draft(definition, List.of(quick), existing, 10, false);

		assertEquals(1, plan.assignments().size());
		assertEquals(2, plan.assignments().getFirst().member().playerId());
	}

	private static Party party(long sequence, long registeredAt, EntryRequestType type, Member... members) {
		return new Party(sequence, registeredAt, type, type.isGroupEntry() ? (int) sequence : 0, List.of(members));
	}

	private static Member member(int playerId, PlayerClass playerClass, Race race) {
		return new Member(playerId, "p" + playerId, playerClass, race);
	}
}
