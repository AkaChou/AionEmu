package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.DataDrivenProgress.Outcome;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenProgress.Result;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenProgress.Slot;

/**
 * P7 步 2 门：DD 原生进度算术（真端 `DataDrivenQuestLoader` + DD handler 逐指令算术）。
 * <p>
 * 冻结点：① 步号 = `vars & 0x3F`、组槽 = bit6/12/18/24 起 6 位；② 命中自增是**无掩码**的
 * `vars += (1 << shift)` ⇒ 计满 63 再 +1 进位污染下一组槽（80817 的 100 杀在真端自身不可完成，
 * 原样复刻）；③ 本步收口要求**全部**声明组槽达标，步进写 = `(vars & 0x3F) + 1`（组槽清零）；
 * ④ 非当前步 / 未声明组 / 守卫位异常 ⇒ 零动作。
 * <p>
 * P7 step 2 gate for the DataDriven progress arithmetic: step number, group counters, the unmasked
 * increment that carries into the next group (the 80817 saturation shape), the all-groups-satisfied
 * closure write, and the zero-action guards.
 */
class DataDrivenProgressTest {

	@Test
	void readsTheRetailLayout() {
		int vars = 0x3 | (5 << 6) | (7 << 12) | (63 << 18) | (1 << 24);
		assertEquals(3, DataDrivenProgress.step(vars), "步号 = bit0-5");
		assertEquals(5, DataDrivenProgress.counter(vars, 1), "组 1 = bit6 起");
		assertEquals(7, DataDrivenProgress.counter(vars, 2), "组 2 = bit12 起");
		assertEquals(63, DataDrivenProgress.counter(vars, 3), "组 3 = bit18 起");
		assertEquals(1, DataDrivenProgress.counter(vars, 4), "组 4 = bit24 起");
	}

	@Test
	void singleGroupStepClosesExactlyAtTarget() {
		List<Slot> slots = List.of(new Slot(1, 3));
		Result first = DataDrivenProgress.hit(0, 0, slots, 1, true);
		assertEquals(Outcome.COUNTER_INCREMENT, first.outcome());
		assertEquals(1 << 6, first.newVars(), "命中一次 = 组 1 +1（真端 prog += 0x40）");
		Result second = DataDrivenProgress.hit(first.newVars(), 0, slots, 1, true);
		assertEquals(Outcome.COUNTER_INCREMENT, second.outcome());
		Result third = DataDrivenProgress.hit(second.newVars(), 0, slots, 1, true);
		assertEquals(Outcome.STEP_COMPLETE, third.outcome(), "达标 = 末步 SetQuestSuccess");
		assertEquals(1, third.newVars(), "步进写 = (vars & 0x3F) + 1（组槽清零）");
	}

	@Test
	void multiGroupStepWaitsForEveryDeclaredGroup() {
		List<Slot> slots = List.of(new Slot(1, 2), new Slot(2, 3));
		int vars = 0;
		vars = DataDrivenProgress.hit(vars, 0, slots, 1, true).newVars();
		Result second = DataDrivenProgress.hit(vars, 0, slots, 1, true);
		assertEquals(Outcome.COUNTER_INCREMENT, second.outcome(), "组 1 满但组 2 未满 ⇒ 仅自增");
		vars = second.newVars();
		vars = DataDrivenProgress.hit(vars, 0, slots, 2, true).newVars();
		vars = DataDrivenProgress.hit(vars, 0, slots, 2, true).newVars();
		Result last = DataDrivenProgress.hit(vars, 0, slots, 2, true);
		assertEquals(Outcome.STEP_COMPLETE, last.outcome(), "全部组槽达标才收口");
		assertEquals(1, last.newVars());
	}

	@Test
	void ladderWalksEveryStepThenCompletes() {
		int vars = 0;
		boolean completed = false;
		for (int step = 0; step < 3; step++) {
			boolean last = step == 2;
			for (int hit = 0; hit < 2; hit++) {
				Result result = DataDrivenProgress.hitSingle(vars, step, 2, last);
				vars = result.newVars();
				if (result.outcome() == Outcome.STEP_COMPLETE) {
					completed = true;
				}
				if (!last) {
					assertEquals(hit == 1 ? Outcome.STEP_ADVANCE : Outcome.COUNTER_INCREMENT, result.outcome(),
						"非末步达标 = 步号 +1（继续下一步）");
				}
			}
		}
		assertTrue(completed, "末步行阶梯必须收口到 SetQuestSuccess");
		assertEquals(3, vars, "收口后步号 = 行数");
	}

	@Test
	void offStepAndUndeclaredGroupAreZeroActions() {
		List<Slot> slots = List.of(new Slot(1, 2), new Slot(3, 1));
		assertEquals(Outcome.NO_ACTION, DataDrivenProgress.hit(5, 0, slots, 1, false).outcome(),
			"非当前步（真端 prog&0x3F != expectedStep）⇒ 零动作");
		assertEquals(Outcome.NO_ACTION, DataDrivenProgress.hit(0, 0, slots, 2, false).outcome(),
			"未声明的组槽 ⇒ 零动作（不得误自增）");
		assertEquals(Outcome.NO_ACTION, DataDrivenProgress.hit(0, 0, List.of(), 1, false).outcome(),
			"无组槽步不走本算术");
	}

	@Test
	void saturatedCounterCarriesIntoTheNextGroupAndNeverCloses() {
		// 80817 形：目标 100 > 6 位组槽上限 63（真端自身不可完成，§10.3-#5 原样复刻）。
		int vars = 0;
		boolean closed = false;
		for (int kill = 0; kill < 200; kill++) {
			Result result = DataDrivenProgress.hitSingle(vars, 0, 100, true);
			if (result.outcome() == Outcome.STEP_COMPLETE || result.outcome() == Outcome.STEP_ADVANCE) {
				closed = true;
			}
			vars = result.newVars();
			assertTrue(DataDrivenProgress.counter(vars, 1) < 100, "组 1 计数永不达 100（6 位回绕）");
		}
		assertTrue(!closed, "80817 在真端算术下不可完成（禁止改成 10 位相机或显式禁用）");
		assertTrue(DataDrivenProgress.counter(vars, 2) > 0, "饱和进位污染下一组槽（真端原样行为）");
		assertEquals(0, DataDrivenProgress.step(vars), "步号永不推进");
	}

	@Test
	void guardShapesFailClosed() {
		assertEquals(Outcome.NO_ACTION,
			DataDrivenProgress.hit(0x40000000, 0, List.of(new Slot(1, 1)), 1, true).outcome(),
			"守卫位异常 ⇒ 零动作（本服 fail-closed 策略，真端不会产生该位形）");
		assertEquals(Outcome.NO_ACTION,
			DataDrivenProgress.hit(-1, 0, List.of(new Slot(1, 1)), 1, true).outcome(), "负值 ⇒ 零动作");
	}

	@Test
	void advanceDropsCountersAndGuardRegion() {
		int vars = 5 | (63 << 6) | (7 << 12);
		assertEquals(6, DataDrivenProgress.advance(vars), "步进写只保留步号位并 +1");
		assertEquals(6, DataDrivenProgress.hit(5 | (1 << 6) | (2 << 12), 5, List.of(new Slot(1, 1)), 1, true).newVars());
	}
}
