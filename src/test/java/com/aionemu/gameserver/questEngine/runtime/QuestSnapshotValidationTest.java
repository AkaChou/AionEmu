package com.aionemu.gameserver.questEngine.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * {@link QuestSnapshot} 容器校验与冻结契约。
 * Container validation and freeze contract for {@link QuestSnapshot}.
 * <p>背景：紧凑构造器对背包/货币等容器做“不可变化 + 逐条目校验”，但每个 {@code withXxx} 都会重新进入构造器，
 * 逐次遍历会产生大量 {@code KeyValueHolder}。现在只在入参还不是不可变副本时校验（即首建路径校验一次），
 * 因此这里锁定“可变入参仍会被校验、快照结果仍被冻结”两条契约。 Background: the canonical constructor copies and
 * element-wise validates the containers, but every {@code withXxx} re-enters it; the repeated iteration allocated a
 * {@code KeyValueHolder} per entry. Validation now runs only for inputs that are not already immutable copies (i.e. the
 * first construction), so this test pins both "mutable input is still validated" and "the snapshot is frozen".</p>
 */
class QuestSnapshotValidationTest {

	private static final int PLAYER_ID = 1;
	private static final int QUEST_ID = 2;

	/** 可变背包里的非法条目必须在首建时被拒绝。 / Invalid entries in a mutable inventory must be rejected on first construction. */
	@Test
	void mutableInventoryWithNonPositiveItemIdIsRejected() {
		Map<Integer, Integer> inventory = new HashMap<>();
		inventory.put(0, 1);

		assertThrows(IllegalArgumentException.class,
				() -> new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, inventory));
	}

	/** 负数数量同样被拒绝。 / A negative count is rejected as well. */
	@Test
	void mutableInventoryWithNegativeCountIsRejected() {
		Map<Integer, Integer> inventory = new HashMap<>();
		inventory.put(100, -1);

		assertThrows(IllegalArgumentException.class,
				() -> new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, inventory));
	}

	/**
	 * 快照持有的容器必须是不可变副本：这样 {@code withXxx} 重入构造器时 {@code Map.copyOf}/{@code Set.copyOf} 会返回同一实例，
	 * 从而走“已校验，跳过逐条目校验”的快路径（消除 KeyValueHolder 分配）。
	 * The containers held by a snapshot must be immutable copies, so that {@code Map.copyOf}/{@code Set.copyOf} on the
	 * {@code withXxx} re-entry return the same instance and take the already-validated fast path.
	 */
	@Test
	void storedContainersAreImmutableCopies() {
		Map<Integer, Integer> inventory = new HashMap<>();
		inventory.put(100, 3);

		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, inventory);

		assertSame(snapshot.inventory(), Map.copyOf(snapshot.inventory()));
		assertSame(snapshot.currencies(), Map.copyOf(snapshot.currencies()));
	}

	/** 合法输入被复制并冻结：后续修改原 Map 不影响快照，快照本身不可写。 / A valid input is copied and frozen. */
	@Test
	void validInventoryIsCopiedAndFrozen() {
		Map<Integer, Integer> inventory = new HashMap<>();
		inventory.put(100, 3);

		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, inventory);
		inventory.put(200, 1);

		assertEquals(Map.of(100, 3), snapshot.inventory());
		assertThrows(UnsupportedOperationException.class, () -> snapshot.inventory().put(300, 1));
	}
}
