package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Spliterator;

import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOT_STATUS.Status;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import org.junit.jupiter.api.Test;

class SM_LOOT_STATUSTest {

	@Test
	void readsDropItemsUnderTheirMutationLock() {
		int targetObjectId = Integer.MIN_VALUE;
		var dropMap = DropRegistrationService.getInstance().getCurrentDropMap();
		dropMap.put(targetObjectId, new LockCheckingSet());
		try {
			new SM_LOOT_STATUS(targetObjectId, Status.LOOT_ENABLE);
		} finally {
			dropMap.remove(targetObjectId);
		}
	}

	private static final class LockCheckingSet extends HashSet<DropItem> {
		@Override
		public Spliterator<DropItem> spliterator() {
			assertTrue(Thread.holdsLock(this), "drop items must be read under the same lock used by pickup writes");
			return super.spliterator();
		}
	}
}
