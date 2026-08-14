package com.aionemu.gameserver.questEngine.model;

/**
 * 任务变量集合，将最多 6 个 6-bit 子变量打包为一个整型值存储。
 * Quest variable set packing up to six 6-bit sub-variables into a single integer value.
 *
 * @author MrPoke
 */
public class QuestVars {

	/** 6 个任务子变量（每个 0–63）。 Six quest sub-variables (each 0–63). */
	private Integer[] questVars = new Integer[6];

	/**
	 * 创建空任务变量集合。
	 * Creates an empty quest variable set.
	 */
	public QuestVars() {
	}

	/**
	 * 使用打包整型值初始化任务变量。
	 * Initializes quest variables from a packed integer value.
	 *
	 * @param var 打包的任务变量值 / Packed quest-var value
	 */
	public QuestVars(int var) {
		setVar(var);
	}

	/**
	 * 按索引获取任务子变量。
	 * Returns the quest sub-variable at the given index.
	 *
	 * @param id 子变量索引（0–5） / Sub-variable index (0–5)
	 * @return 子变量值 / Sub-variable value
	 */
	public int getVarById(int id) {
		return questVars[id];
	}

	/**
	 * 按索引设置任务子变量。
	 * Sets the quest sub-variable at the given index.
	 *
	 * @param id 子变量索引（0–5） / Sub-variable index (0–5)
	 * @param var 子变量值 / Sub-variable value
	 */
	public void setVarById(int id, int var) {
		questVars[id] = var;
	}

	/**
	 * 将全部子变量打包为一个整型：Sum(value_i * 64^i)。
	 * Packs all sub-variables into one int: Sum(value_i * 64^i).
	 *
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
	 *
	 * @param var 打包的任务变量值 / Packed quest-var value
	 */
	public void setVar(int var) {
		for (int i = 0; i <= 5; i++) {
			questVars[i] = var & 0x3F;
			var >>= 0x06;
		}
	}
}
