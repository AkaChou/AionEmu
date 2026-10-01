package com.aionemu.gameserver.questEngine.tablelane;

/**
 * 真端家族任务进度相机（计划 §6.2；P0a 证据：fun_731.cpp:5306/5356 与双通道副作用矩阵）。
 * <p>
 * 纯函数：三重守卫（status==START、vars 守卫区、槽计数未满）不满足即 {@link Outcome#NO_ACTION}
 * （超杀/未接取零副作用）；满足则 {@code vars += 1<<shift}（一次事件只加一），且仅在
 * {@code flag && newVars==fullValue} 时走推进通道 {@link Outcome#ADVANCE_WRITE}，否则
 * {@link Outcome#NORMAL_WRITE}。持久化与客户端同步不在本类——那是 state port（家族切换批）的事。
 * <p>
 * The retail family-quest progress camera (plan §6.2; P0a evidence fun_731.cpp:5306/5356 plus the
 * dual-channel side-effect matrix). Pure function: when any of the three guards fails (status not
 * START, vars in the guard region, slot already full) the outcome is {@link Outcome#NO_ACTION}
 * (overkills are side-effect free); otherwise {@code vars += 1<<shift} (one event, plus one), and
 * only {@code flag && newVars==fullValue} routes to the advance channel {@link Outcome#ADVANCE_WRITE},
 * everything else to {@link Outcome#NORMAL_WRITE}. Persistence and client sync live in the state
 * port (family-switch batch), not here.
 */
public final class ProgressCamera {

	/** 真端任务状态枚举（P0a §4.2 行为证据）。 / Retail quest status codes (P0a §4.2 behavioral evidence). */
	public enum Status {
		/** 无/未接。 / none. */
		NONE(0),
		/** 已接取/进行中（相机唯一动作态）。 / acquired — the only state the camera acts in. */
		START(3),
		/** 已完成待交付。 / success, awaiting turn-in. */
		SUCCESS(4),
		/** 已领奖/终结。 / rewarded, terminal. */
		REWARDED(5),
		/** 待接取。 / waiting for pickup. */
		WAITING(6);

		private final int code;

		Status(int code) {
			this.code = code;
		}

		/** 真端状态码。 / The retail status code. */
		public int code() {
			return code;
		}
	}

	/** 相机裁决。 / Camera verdict. */
	public enum Outcome {
		/** 守卫未过：零副作用。 / A guard failed: no side effect. */
		NO_ACTION,
		/** 普通写入（真端 vtable +0xf0）。 / normal write (retail vtable +0xf0). */
		NORMAL_WRITE,
		/** 推进写入（真端 vtable +0x100，status 3→4 由 state port 执行）。 / advance write (retail +0x100; the state port performs status 3→4). */
		ADVANCE_WRITE
	}

	/** 相机结果：裁决 + 新 vars。 / Camera result: verdict plus the new vars word. */
	public record Result(Outcome outcome, int newVars) {
	}

	private ProgressCamera() {
	}

	/**
	 * 相机推进一步。
	 * Advances the camera by one event.
	 * @param status 玩家该任务当前状态 / current quest status
	 * @param vars 当前 raw vars / current raw vars word
	 * @param width 槽宽型别 / slot width kind
	 * @param slot 槽（1 基）/ slot (1-based)
	 * @param required 该槽所需计数 / required count for the slot
	 * @param fullValue 整行满值（各槽满值按位组合）/ full-row value
	 * @param flag 表行 flag 位（真端 2463 个调用点全为 1）/ table-row flag bit (all 2463 retail call sites pass 1)
	 */
	public static Result advance(Status status, int vars, RawQuestVarsCodec.Width width, int slot, int required,
			int fullValue, boolean flag) {
		if (status != Status.START || !RawQuestVarsCodec.guardClear(vars)) {
			return new Result(Outcome.NO_ACTION, vars);
		}
		if (RawQuestVarsCodec.slotValue(width, vars, slot) >= required) {
			return new Result(Outcome.NO_ACTION, vars);
		}
		int shift = width.shift(slot);
		int newVars = vars + (1 << shift);
		if (newVars < 0 || newVars >= RawQuestVarsCodec.GUARD_BITS) {
			// 理论不可达（required<=mask 已保证），仍按 fail-closed 处理。
			// Unreachable while required<=mask, but stay fail-closed regardless.
			return new Result(Outcome.NO_ACTION, vars);
		}
		if (flag && newVars == fullValue) {
			return new Result(Outcome.ADVANCE_WRITE, newVars);
		}
		return new Result(Outcome.NORMAL_WRITE, newVars);
	}

	/**
	 * 基于已验证相机行推进一步。
	 * Advances the camera by one event using a validated camera row.
	 */
	public static Result advance(Status status, int vars, CameraRegistry.CameraRow row, int slot, boolean flag) {
		int required = row.required(slot);
		if (required <= 0) {
			return new Result(Outcome.NO_ACTION, vars);
		}
		return advance(status, vars, row.width(), slot, required, row.fullValue(), flag);
	}
}
