package com.aionemu.gameserver.model.skill;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.skill.linked_skill.EquippedStigmasEntry;
import com.aionemu.gameserver.model.skill.linked_skill.PlayerEquippedStigmaList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionalStigmaStateSnapshotTest {
	@Test
	void equippedStigmaSnapshotRestoresMembershipAndPersistentState() {
		EquippedStigmasEntry entry = new EquippedStigmasEntry(140000003, "quest-stigma",
			PersistentState.UPDATED);
		PlayerEquippedStigmaList stigmas = new PlayerEquippedStigmaList(List.of(entry));
		var snapshot = stigmas.transactionSnapshot();

		assertTrue(stigmas.removeInTransaction(140000003));
		assertFalse(stigmas.isItemPresent(140000003));
		assertEquals(PersistentState.DELETED, entry.getPersistentState());
		assertEquals(1, stigmas.getDeletedItems().length);

		snapshot.restore();
		assertTrue(stigmas.isItemPresent(140000003));
		assertEquals(PersistentState.UPDATED, entry.getPersistentState());
		assertEquals(0, stigmas.getDeletedItems().length);
	}

	@Test
	void skillSnapshotRestoresMapsLevelsAndDeletedEntries() {
		PlayerSkillEntry entry = new PlayerSkillEntry(1001, false, false, 1, 0,
			null, 0, false, PersistentState.UPDATED);
		PlayerSkillList skills = new PlayerSkillList(List.of(entry));
		var snapshot = skills.transactionSnapshot();

		entry.setSkillLvl(2);
		assertTrue(skills.removeSkill(1001));
		assertFalse(skills.isSkillPresent(1001));
		assertEquals(PersistentState.DELETED, entry.getPersistentState());

		snapshot.restore();
		assertTrue(skills.isSkillPresent(1001));
		assertEquals(1, skills.getSkillLevel(1001));
		assertEquals(PersistentState.UPDATED, entry.getPersistentState());
		assertEquals(0, skills.getDeletedSkills().length);
	}
}
