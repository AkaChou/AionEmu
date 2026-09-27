package com.aionemu.gameserver.questEngine.model;

import lombok.NoArgsConstructor;

/**
 * 任务变量集合，把最多 6 个子变量打包为一个 32 位整型值存储：槽 0..4 各 6 bit（bit0..29），
 * 第 5 槽只有 bit30..31（真端只把 var5 用作 1 bit 标志，见 QuestVarsTest）。
 * Quest variable set packing up to six sub-variables into a single 32-bit value: slots 0..4 own bits 0..29
 * and slot 5 keeps only bits 30..31 (retail uses var5 as a 1-bit flag only).
 * @author MrPoke
 */
@NoArgsConstructor
public class QuestVars {

	/** 任务子变量槽位数量。 Number of quest sub-variable slots. */
	private static final int VAR_COUNT = 6;

	/** 单个任务子变量的最大值（6 位）。 Maximum value of one quest sub-variable (6 bits). */
	private static final int MAX_VAR_VALUE = 0x3F;

	/** 6 个任务子变量（每个 0–63）。 Six quest sub-variables (each 0–63). */
	private final Integer[] questVars = new Integer[VAR_COUNT];

	/**
	 * 判断子变量索引是否在 0–5 范围内。
	 * Returns whether the sub-variable index is within 0–5.
	 * @param id 子变量索引 / Sub-variable index
	 * @return 有效则为 true / true when valid
	 */
	public static boolean isValidVarId(int id) {
		return id >= 0 && id < VAR_COUNT;
	}

	/**
	 * 判断单个子变量值是否可由 6 位完整表示。
	 * Returns whether a single sub-variable value fits into 6 bits.
	 * @param var 子变量值 / Sub-variable value
	 * @return 有效则为 true / true when valid
	 */
	public static boolean isValidVarValue(int var) {
		return var >= 0 && var <= MAX_VAR_VALUE;
	}

	private static void checkVarId(int id) {
		if (!isValidVarId(id)) {
			throw new IllegalArgumentException(
					"Quest variable index must be between 0 and " + (VAR_COUNT - 1) + ": " + id);
		}
	}

	private static void checkVarValue(int var) {
		if (!isValidVarValue(var)) {
			throw new IllegalArgumentException(
					"Quest variable value must be between 0 and " + MAX_VAR_VALUE + ": " + var);
		}
	}

	/**
	 * 使用打包整型值初始化任务变量。
	 * Initializes quest variables from a packed integer value.
	 * @param var 打包的任务变量值 / Packed quest-var value
	 */
	public QuestVars(int var) {
		setVar(var);
	}

	/**
	 * 按索引获取任务子变量。
	 * Returns the quest sub-variable at the given index.
	 * @param id 子变量索引（0–5） / Sub-variable index (0–5)
	 * @return 子变量值 / Sub-variable value
	 * @throws IllegalArgumentException 索引超出 0–5 时抛出 / when the index is outside 0–5
	 */
	public int getVarById(int id) {
		checkVarId(id);
		// 未显式赋值的槽位按 0 处理（无参构造 + 部分 setVarById 的组合不该抛 NPE）。
		// Unset slots read as zero so a partially populated instance never throws.
		Integer value = questVars[id];
		return value == null ? 0 : value;
	}

	/**
	 * 按索引设置任务子变量。
	 * Sets the quest sub-variable at the given index.
	 * @param id 子变量索引（0–5） / Sub-variable index (0–5)
	 * @param var 子变量值（0–63） / Sub-variable value (0–63)
	 * @throws IllegalArgumentException 索引或值超出 6-bit 子变量范围时抛出 /
	 *         when the index or value is outside the 6-bit sub-variable range
	 */
	public void setVarById(int id, int var) {
		checkVarId(id);
		checkVarValue(var);
		questVars[id] = var;
	}

	/**
	 * 将全部子变量打包为一个整型：Sum(value_i * 64^i)。
	 * Packs all sub-variables into one int: Sum(value_i * 64^i).
	 * @return 打包后的整型值 / Packed integer value
	 */
	public int getQuestVars() {
		int var = 0;
		for (int i = 5; i >= 0; i--) {
			var <<= 0x06;
			Integer value = questVars[i];
			var |= value == null ? 0 : value;
		}
		return var;
	}

	/**
	 * 用打包整型值填充子变量数组（每 6 bit 一个槽位）。
	 * Fills the sub-variable array from a packed integer (one slot per 6 bits).
	 * @param var 打包的任务变量值 / Packed quest-var value
	 */
	public void setVar(int var) {
		for (int i = 0; i <= 5; i++) {
			// 无符号移位：打包值的高位（第 5 槽 bit30..31）不能按符号位扩展成 62 之类的值。
			// Unsigned shifts keep the top slot read consistent with the packed bit layout.
			questVars[i] = (var >>> (6 * i)) & 0x3F;
		}
	}
}
