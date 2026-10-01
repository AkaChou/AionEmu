package com.aionemu.gameserver.questEngine.tablelane;

import java.util.List;

/**
 * 真端 DataDriven 进度算术（计划 §10.2「P7 DataDriven」步 2；证据 `ScriptDLL64.c` 逐指令：
 * `FUN_180c46020` Hunt / `FUN_180c46980` PvP / `FUN_180c478e0` TalkFOBJ / `FUN_180c466a0` Talk
 * / `FUN_180c47bf0` EnterArea / `FUN_180c467b0` EnterWorld）。
 * <p>
 * DD 行**不走相机**（真端 DD handler 内联算术，无 `fun_731.cpp:5306` 那种相机 vtable 调用），
 * 其 raw vars 布局 = 「6 位步号（bit0-5）+ 每 6 位一个组槽（bit6..）」：
 * <ul>
 *   <li>步号守卫：`(prog & 0x3F) != expectedStep` ⇒ 该事件不属于当前步，零动作；</li>
 *   <li>命中自增：字面 `prog += (1 << shift)`——**不做 6 位掩码**，因此计数 63 再 +1 会进位污染下一组槽
 *       （真端 80817 的 100 杀即此形：槽回绕后 `counter < target` 永不成立 ⇒ 真端自身不可完成，
 *       本类按 §10.3-#5 裁定**原样复刻**，禁止改成 10 位相机或显式禁用）；</li>
 *   <li>自增**只在 `counter < target` 时发生**（真端 `FUN_180c46020`：`if (uVar11 &lt; target) { vars += …; }`
 *       ——计满的组槽不再自增，因此不会溢出污染下一组；全部组槽已达标时仍按收口步进）；</li>
 *   <li>本步收口：仅当**该步声明的全部组槽**都在新字里达标时才步进，步进形 = `(prog & 0x3F) + 1`
 *       （真端 `prog = (prog & 0x3F) + 1` ⇒ 组槽清零、守卫位一并丢弃）；</li>
 *   <li>区分中间步（真端 `SetQuestProgress`，继续走下一步）与末步（真端 `SetQuestSuccess`，转待领奖）。</li>
 * </ul>
 * 用到组槽的类别：Hunt（多组，每组一个子目标）、TalkFOBJ（同形）、CollectItem 与 PvP（单组）；
 * Talk / EnterArea / EnterWorld / ItemPlay 无组槽（直接步进）。持久化与客户端同步属 state port，不在本类。
 * <p>
 * Retail DataDriven progress arithmetic (plan §10.2 "P7 DataDriven" step 2; instruction-level evidence).
 * DD rows never use a camera — the retail handler inlines the arithmetic — and pack raw vars as a 6-bit step
 * number (bits 0-5) plus one 6-bit group slot per counter (bits 6..). The increment is the literal
 * {@code prog += (1 << shift)} with **no 6-bit masking**, so a saturated group carries into the next group
 * (the retail 80817 shape: 100 kills over a 63-cap slot can never圈 the step, deliberately reproduced).
 * A step closes only when every declared group of that step is satisfied; closing writes {@code (prog & 0x3F) + 1},
 * which drops all group counters. Persistence and client sync belong to the state port, not to this class.
 */
public final class DataDrivenProgress {

	/** 步号掩码（真端 `(prog & 0x3F) == expectedStep`）。 / The step-number mask. */
	public static final int STEP_MASK = 0x3F;
	/** 单组槽最大合法计数（6 位）。 / The largest legal per-group counter (6 bits). */
	public static final int GROUP_MASK = 0x3F;
	/** 组槽数量（bit6..29，四组；真端 Hunt 声明 5 组，但第 5 组落在 bit30/31 守卫区，数据零使用）。 */
	public static final int MAX_GROUPS = 4;

	/** 一次事件的裁决。 / The verdict of one progress event. */
	public enum Outcome {
		/** 守卫未过（非当前步 / 位形异常 / 组未声明）：零动作。 / Guard failed: no action. */
		NO_ACTION,
		/** 组槽自增（真端 `SetQuestProgress` 中间写）。 / Group counter incremented (retail mid-step write). */
		COUNTER_INCREMENT,
		/** 本步收口且仍有后续步：步号 +1、组槽清零。 / Step closed with further steps left. */
		STEP_ADVANCE,
		/** 本步收口且为末步：真端 `SetQuestSuccess`（转待领奖）。 / Final step closed (retail {@code SetQuestSuccess}). */
		STEP_COMPLETE
	}

	/** 裁决 + 新 vars（零动作时原样返回）。 / Verdict plus the new vars word (unchanged on NO_ACTION). */
	public record Result(Outcome outcome, int newVars) {
	}

	/** 一个组槽声明：组号（1..4）+ 目标计数。 / One declared group slot: 1-based group + target count. */
	public record Slot(int group, int target) {
		public Slot {
			if (group < 1 || group > MAX_GROUPS) {
				throw new IllegalArgumentException("DATA_DRIVEN_GROUP_INVALID: group " + group);
			}
			if (target < 1) {
				throw new IllegalArgumentException("DATA_DRIVEN_TARGET_INVALID: target " + target);
			}
		}
	}

	private DataDrivenProgress() {
	}

	/** 读步号（bit0-5）。 / Reads the step number (bits 0-5). */
	public static int step(int vars) {
		return vars & STEP_MASK;
	}

	/** 读组槽计数（组 1..4 ⇒ bit6/12/18/24 起 6 位）。 / Reads a group counter. */
	public static int counter(int vars, int group) {
		if (group < 1 || group > MAX_GROUPS) {
			throw new IllegalArgumentException("DATA_DRIVEN_GROUP_INVALID: group " + group);
		}
		return (vars >>> (6 * group)) & GROUP_MASK;
	}

	/**
	 * 位形守卫（本服 fail-closed 策略）：真端 DD handler 不查守卫位，但真端 DD 行也不会产生
	 * bit30/31 或负值；本服遇到异常位形一律零动作（计划 §11「raw vars 位形异常 → fail-closed」）。
	 * Defensive guard: the retail DD handler does not test bits 30/31, but never produces them either;
	 * this server fails closed on such shapes (plan §11).
	 */
	public static boolean guardClear(int vars) {
		return RawQuestVarsCodec.guardClear(vars);
	}

	/** 命中自增（真端字面 `prog += (1 << shift)`，无掩码 ⇒ 饱和后进位）。 / Literal retail increment (carries once saturated). */
	public static int increment(int vars, int group) {
		if (group < 1 || group > MAX_GROUPS) {
			throw new IllegalArgumentException("DATA_DRIVEN_GROUP_INVALID: group " + group);
		}
		return vars + (1 << (6 * group));
	}

	/** 该步声明的全部组槽是否都已达标。 / Whether every declared group of the step is already satisfied. */
	public static boolean allSatisfied(int vars, List<Slot> slots) {
		for (Slot slot : slots) {
			if (counter(vars, slot.group()) < slot.target()) {
				return false;
			}
		}
		return true;
	}

	/** 步进写：真端 `prog = (prog & 0x3F) + 1`（组槽清零、守卫位丢弃）。 / Retail step advance write. */
	public static int advance(int vars) {
		return (vars & STEP_MASK) + 1;
	}

	/**
	 * 处理一次「命中当前步某组槽」的事件。
	 * Handles one event hitting one declared group of the current step.
	 *
	 * @param vars         当前 raw vars / current raw vars word
	 * @param expectedStep 该步的步号（真端注册期写入 `expectedStep`）/ the registered step number
	 * @param slots        该步声明的组槽（真端 `data+8` 子目标数组）/ the step's declared group slots
	 * @param group        本次命中的组（1..4）/ the hit group
	 * @param lastStep     是否为该行最后一步 / whether this is the row's final step
	 */
	public static Result hit(int vars, int expectedStep, List<Slot> slots, int group, boolean lastStep) {
		if (!guardClear(vars) || slots == null || slots.isEmpty()) {
			return new Result(Outcome.NO_ACTION, vars);
		}
		if (step(vars) != expectedStep) {
			return new Result(Outcome.NO_ACTION, vars);
		}
		boolean declared = false;
		for (Slot slot : slots) {
			if (slot.group() == group) {
				declared = true;
				break;
			}
		}
		if (!declared) {
			return new Result(Outcome.NO_ACTION, vars);
		}
		int target = -1;
		for (Slot slot : slots) {
			if (slot.group() == group) {
				target = slot.target();
				break;
			}
		}
		// 真端只在本组未达标时自增（`FUN_180c46020`/`FUN_180c46980` 的 `counter < target` 守卫）：
		// 已满组槽的超杀零写（不得进位污染下一组），但「全部组槽已达标」仍按收口步进。
		// The retail increment is guarded by counter < target; an over-target hit writes nothing,
		// while an all-satisfied shape still closes the step.
		int newVars = vars;
		boolean incremented = false;
		if (counter(vars, group) < target) {
			newVars = increment(vars, group);
			if (!guardClear(newVars)) {
				// 进位越过守卫位（仅饱和污染可达）⇒ 零动作，绝不写坏位形。
				return new Result(Outcome.NO_ACTION, vars);
			}
			incremented = true;
		}
		if (!allSatisfied(newVars, slots)) {
			return incremented ? new Result(Outcome.COUNTER_INCREMENT, newVars)
				: new Result(Outcome.NO_ACTION, vars);
		}
		int advanced = advance(newVars);
		return new Result(lastStep ? Outcome.STEP_COMPLETE : Outcome.STEP_ADVANCE, advanced);
	}

	/** 单组槽步（CollectItem/PvP 形）：组 1、给定目标。 / Single-group step (the CollectItem/PvP shape). */
	public static Result hitSingle(int vars, int expectedStep, int target, boolean lastStep) {
		return hit(vars, expectedStep, List.of(new Slot(1, target)), 1, lastStep);
	}
}
