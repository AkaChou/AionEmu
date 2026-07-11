package com.aionemu.gameserver.model.instance;

/**
 * Stage 类型枚举。
 * Stage Type enumeration.
 */

public enum StageType {
	/** 默认 / Default. */
	DEFAULT(0, 0), START_STAGE_1_ELEVATOR(35464, 1), START_STAGE_1_ROUND_1(35465, 1),
	/** Start Kaisinel Stage 1 Round 1 / Start Kaisinel Stage 1 Round 1 */
	START_KAISINEL_STAGE_1_ROUND_1(35465, 1), START_MARCHUTAN_STAGE_1_ROUND_1(35465, 1),
	/** Start Stage 1 Round 2 / Start Stage 1 Round 2 */
	START_STAGE_1_ROUND_2(35466, 1), START_STAGE_1_ROUND_3(35467, 1), START_STAGE_1_ROUND_4(35468, 1),
	/** Start Stage 1 Round 5 / Start Stage 1 Round 5 */
	START_STAGE_1_ROUND_5(35469, 1), START_STAGE_2_ELEVATOR(36464, 1), START_STAGE_2_ROUND_1(36465, 1),
	/** Start Stage 2 Round 2 / Start Stage 2 Round 2 */
	START_STAGE_2_ROUND_2(36466, 1), START_STAGE_2_ROUND_3(36467, 1), START_STAGE_2_ROUND_4(36468, 1),
	/** Start Stage 2 Round 5 / Start Stage 2 Round 5 */
	START_STAGE_2_ROUND_5(36469, 1), START_STAGE_3_ELEVATOR(37464, 1), START_STAGE_3_ROUND_1(37465, 1),
	/** Start Stage 3 Round 2 / Start Stage 3 Round 2 */
	START_STAGE_3_ROUND_2(37466, 1), START_STAGE_3_ROUND_3(37467, 1), START_STAGE_3_ROUND_4(37468, 1),
	/** Start Stage 3 Round 5 / Start Stage 3 Round 5 */
	START_STAGE_3_ROUND_5(37469, 1), START_STAGE_4_ELEVATOR(38464, 1), START_STAGE_4_ROUND_1(38465, 1),
	/** Start Haramel Stage 4 Round 1 / Start Haramel Stage 4 Round 1 */
	START_HARAMEL_STAGE_4_ROUND_1(38465, 1), START_KROMEDE_STAGE_4_ROUND_1(38465, 1), START_STAGE_4_ROUND_2(38466, 1),
	/** Start Stage 4 Round 3 / Start Stage 4 Round 3 */
	START_STAGE_4_ROUND_3(38467, 1), START_STAGE_4_ROUND_4(38468, 1), START_STAGE_4_ROUND_5(38469, 1),
	/** Start Stage 5 / Start Stage 5 */
	START_STAGE_5(8392, 3), START_STAGE_5_ROUND_1(8393, 3), START_AZOTURAN_STAGE_5_ROUND_1(8393, 3),
	/** Start Steel Rake Stage 5 Round 1 / Start Steel Rake Stage 5 Round 1 */
	START_STEEL_RAKE_STAGE_5_ROUND_1(8393, 3), START_STAGE_5_ROUND_2(8394, 3), START_STAGE_5_ROUND_3(8395, 3),
	/** Start Stage 5 Round 4 / Start Stage 5 Round 4 */
	START_STAGE_5_ROUND_4(8396, 3), START_STAGE_5_ROUND_5(8397, 3), START_STAGE_6(43856, 4),
	/** Start Stage 6 Round 1 / Start Stage 6 Round 1 */
	START_STAGE_6_ROUND_1(43857, 4), START_STAGE_6_ROUND_2(43858, 4), START_STAGE_6_ROUND_3(43859, 4),
	/** Start Stage 6 Round 4 / Start Stage 6 Round 4 */
	START_STAGE_6_ROUND_4(43860, 4), START_STAGE_6_ROUND_5(43861, 4), START_STAGE_7(13784, 6),
	/** Start Stage 7 Round 1 / Start Stage 7 Round 1 */
	START_STAGE_7_ROUND_1(13785, 6), START_STAGE_7_ROUND_2(13786, 6), START_STAGE_7_ROUND_3(13787, 6),
	/** Start Stage 7 Round 4 / Start Stage 7 Round 4 */
	START_STAGE_7_ROUND_4(13788, 6), START_STAGE_7_ROUND_5(13789, 6), START_STAGE_8(49248, 7),
	/** Start Stage 8 Round 1 / Start Stage 8 Round 1 */
	START_STAGE_8_ROUND_1(49249, 7), START_STAGE_8_ROUND_2(49250, 7), START_STAGE_8_ROUND_3(49251, 7),
	/** Start Stage 8 Round 4 / Start Stage 8 Round 4 */
	START_STAGE_8_ROUND_4(49252, 7), START_STAGE_8_ROUND_5(49253, 7), START_STAGE_9(19176, 9),
	/** Start Stage 9 Round 1 / Start Stage 9 Round 1 */
	START_STAGE_9_ROUND_1(19177, 9), START_STAGE_9_ROUND_2(19178, 9), START_STAGE_9_ROUND_3(19179, 9),
	/** Start Stage 9 Round 4 / Start Stage 9 Round 4 */
	START_STAGE_9_ROUND_4(19180, 9), START_STAGE_9_ROUND_5(19181, 9), START_STAGE_10(54640, 10),
	/** Start Stage 10 Round 1 / Start Stage 10 Round 1 */
	START_STAGE_10_ROUND_1(54641, 10), START_STAGE_10_ROUND_2(54642, 10), START_STAGE_10_ROUND_3(54643, 10),
	/** Start Stage 10 Round 4 / Start Stage 10 Round 4 */
	START_STAGE_10_ROUND_4(54644, 10), START_STAGE_10_ROUND_5(54645, 10), PASS_STAGE_1(35566, 1),
	/** Pass Stage 2 / Pass Stage 2 */
	PASS_STAGE_2(36565, 1), PASS_STAGE_3(37566, 1), PASS_STAGE_4(38566, 1), PASS_STAGE_5(39566, 1),
	/** Pass Stage 6 / Pass Stage 6 */
	PASS_STAGE_6(40565, 1), PASS_GROUP_STAGE_1(35569, 1), PASS_GROUP_STAGE_2(36569, 1), PASS_GROUP_STAGE_3(37569, 1),
	/** Pass 小队 Stage4 / Pass Group Stage 4 */
	PASS_GROUP_STAGE_4(38569, 1), PASS_GROUP_STAGE_5(8497, 3), PASS_GROUP_STAGE_6(43961, 4),
	/** Pass 小队 Stage7 / Pass Group Stage 7 */
	PASS_GROUP_STAGE_7(13789, 6), PASS_GROUP_STAGE_8(49253, 7), PASS_GROUP_STAGE_9(19181, 9),
	/** Pass 小队 Stage10 / Pass Group Stage 10 */
	PASS_GROUP_STAGE_10(54645, 10), START_BONUS_STAGE_2(36470, 1), START_BONUS_STAGE_3(37470, 1),
	/** Start Bonus Stage 6 / Start Bonus Stage 6 */
	START_BONUS_STAGE_6(43862, 4), START_BONUS_STAGE_4(38470, 1), PVP_STAGE_1(1, 0), PVP_STAGE_2(2, 0),
	/** Pvp Stage 3 / Pvp Stage 3 */
	PVP_STAGE_3(3, 0), PVP_STAGE_4(4, 0), PVP_STAGE_5(5, 0), PVP_STAGE_6(6, 0), PVP_STAGE_OVER(0, 0);

	private int id;
	private int type;

	private StageType(int id, int type) {
		this.id = id;
		this.type = type;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取类型。 / Returns the type. */
	public int getType() {
		return type;
	}
}
