package com.aionemu.gameserver.model.gameobjects.player.equipmentsetting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

class EquipmentSettingListTest {

	@Test
	void addLoadedSettingKeepsItAvailableWithoutMarkingDirty() {
		EquipmentSettingList list = new EquipmentSettingList(null);

		list.add(1, 2, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116,
				117, 118, 119, 120, 121, false);

		Collection<EquipmentSetting> settings = list.getEquipmentSetting();
		EquipmentSetting setting = settings.iterator().next();

		assertEquals(1, settings.size());
		assertEquals(1, setting.getSlot());
		assertEquals(2, setting.getDisplay());
		assertEquals(101, setting.getmHand());
		assertEquals(121, setting.getBracelet());
		assertEquals(PersistentState.UPDATED, setting.getPersistentState());
	}

	@Test
	void addSettingForSameSlotReplacesPreviousValue() {
		EquipmentSettingList list = new EquipmentSettingList(null);

		list.add(1, 0, 101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
		list.add(1, 0, 202, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);

		Collection<EquipmentSetting> settings = list.getEquipmentSetting();
		EquipmentSetting setting = settings.iterator().next();

		assertEquals(1, settings.size());
		assertEquals(202, setting.getmHand());
	}

	@Test
	void addNewSettingMarksItForStorage() {
		EquipmentSettingList list = new EquipmentSettingList(null);

		EquipmentSetting setting = list.add(2, 1, 301, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, true);

		assertEquals(2, setting.getSlot());
		assertEquals(PersistentState.UPDATE_REQUIRED, setting.getPersistentState());
	}
}
