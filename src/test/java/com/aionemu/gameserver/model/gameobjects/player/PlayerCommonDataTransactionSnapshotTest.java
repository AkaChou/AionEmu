package com.aionemu.gameserver.model.gameobjects.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务事务快照能够回滚经验升级连带修改的高阶守护者标记。
 * Verifies that quest transaction snapshots roll back the ArchDaeva flag changed as a side effect of EXP leveling.
 */
class PlayerCommonDataTransactionSnapshotTest {
	@Test
	void rollbackRestoresArchDaevaFlag() {
		PlayerCommonData commonData = new PlayerCommonData(7);
		PlayerCommonData.TransactionSnapshot snapshot = commonData.transactionSnapshot();

		commonData.setArchDaeva(true);
		assertTrue(commonData.isArchDaeva());
		snapshot.restore();

		assertFalse(commonData.isArchDaeva());
	}
}
