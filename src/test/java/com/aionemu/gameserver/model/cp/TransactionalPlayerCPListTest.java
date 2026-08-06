package com.aionemu.gameserver.model.cp;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionalPlayerCPListTest {
	@Test
	void snapshotRestoresMembershipAndPersistentState() throws Exception {
		PlayerCPEntry entry = new PlayerCPEntry(407, 12, PersistentState.UPDATED);
		PlayerCPList cp = new PlayerCPList(List.of(entry));
		var snapshot = cp.transactionSnapshot();

		Field field = PlayerCPList.class.getDeclaredField("entry");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<Integer, PlayerCPEntry> entries = (Map<Integer, PlayerCPEntry>) field.get(cp);
		entries.clear();
		entry.setPersistentState(PersistentState.DELETED);

		snapshot.restore();

		assertTrue(cp.getBasicCP().length == 1);
		assertEquals(entry, cp.getBasicCP()[0]);
		assertEquals(PersistentState.UPDATED, entry.getPersistentState());
	}
}
