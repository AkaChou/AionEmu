package com.aionemu.gameserver.questEngine.retail;

import java.util.List;

/**
 * 真端 SimpleHunt 击杀计数器布局（ScriptDLL64 {@code FUN_180cb13b0} 反编译语义）。
 * <p>
 * 一个 32 位进度字段按 6 位一组打包计数器：第 N 个槽位占第 {@code 6*(N-1)} 位起 6 位；
 * 击杀时的判定是 {@code (packed >> shift) & 0x3F < countN}，通过则 {@code packed += 1 << shift}；
 * 当累加值达到整条任务的"完成值" {@code Σ countN << shift} 时走完成态写入（真端 0x100），否则普通写入（0xf0）。
 * <p>
 * Retail kill counter layout recovered from the decompiled registration helper.
 */
public final class RetailHuntCounterLayout {

	/** 每个计数器的位宽。 / Bit width of one counter. */
	public static final int SECTION_BITS = 6;

	/** 计数器取值掩码（真端上限 63）。 / Counter mask (retail caps a counter at 63). */
	public static final int SECTION_MASK = (1 << SECTION_BITS) - 1;

	private RetailHuntCounterLayout() {
	}

	/** 计数器槽位（1 起）→ 位偏移。 / 1-based counter slot to bit offset. */
	public static int shiftFor(int slot) {
		if (slot < 1) {
			throw new IllegalArgumentException("counter slot must be 1-based, was " + slot);
		}
		return (slot - 1) * SECTION_BITS;
	}

	/** 读取某个计数器的当前值。 / Current value of one counter. */
	public static int counterValue(int packed, int slot) {
		return (packed >>> shiftFor(slot)) & SECTION_MASK;
	}

	/** 真端 `(packed >> shift) & 0x3F < countN` 判定。 / Retail "can still count" check. */
	public static boolean canIncrement(int packed, int slot, int required) {
		return counterValue(packed, slot) < Math.min(required, SECTION_MASK + 1);
	}

	/** 真端 `packed + (1 << shift)`。 / Retail increment for one counter. */
	public static int increment(int packed, int slot) {
		return packed + (1 << shiftFor(slot));
	}

	/** 整条任务的完成值 `Σ countN << 6(N-1)`（真端写法是 OR，槽位不重叠时等价）。 / Retail goal value. */
	public static int goal(List<RetailSimpleHuntTable.Counter> counters) {
		int goal = 0;
		for (RetailSimpleHuntTable.Counter counter : counters) {
			goal |= (counter.required() & SECTION_MASK) << shiftFor(counter.slot());
		}
		return goal;
	}

	/** 是否达到完成值。 / Whether the packed progress reached the quest goal. */
	public static boolean isComplete(int packed, List<RetailSimpleHuntTable.Counter> counters) {
		return packed == goal(counters);
	}
}
