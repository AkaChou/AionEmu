package com.aionemu.gameserver.questEngine.model;

import lombok.NoArgsConstructor;

/**
 * 任务变量集合，将最多 6 个 6-bit 子变量打包为一个整型值存储。
 * Quest variable set packing up to six 6-bit sub-variables into a single integer value.
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
		return questVars[id];
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
			var |= questVars[i];
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
			questVars[i] = var & 0x3F;
			var >>= 0x06;
		}
	}
}
