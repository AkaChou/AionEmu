package com.aionemu.gameserver.questEngine.model;

/**
 * 任务对话框/交互 ID 映射枚举，将客户端 dialog ID 对应到具名常量。
 * interaction id mapping enum that maps client dialog ids to named constants.
 * <p>
 * 涵盖：奖励选择、接/拒任务、步骤推进、SELECT_ACTION_* 脚本分支、特殊操作等。
 * Covers: reward selection, accept/refuse, step progression, SELECT_ACTION_* script branches, special actions, etc.
 */
@Deprecated(forRemoval = false)
public enum QuestDialog {
	/** 空/未知。 Null / unknown. */
	NULL(0),
	/** 使用对象交互。 Use-object interaction. */
	USE_OBJECT(-1),
	/** 选择奖励 1。 Selected quest reward 1. */
	SELECTED_QUEST_REWARD1(8),
	/** 选择奖励 2。 Selected quest reward 2. */
	SELECTED_QUEST_REWARD2(9),
	/** 选择奖励 3。 Selected quest reward 3. */
	SELECTED_QUEST_REWARD3(10),
	/** 选择奖励 4。 Selected quest reward 4. */
	SELECTED_QUEST_REWARD4(11),
	/** 选择奖励 5。 Selected quest reward 5. */
	SELECTED_QUEST_REWARD5(12),
	/** 选择奖励 6。 Selected quest reward 6. */
	SELECTED_QUEST_REWARD6(13),
	/** 选择奖励 7。 Selected quest reward 7. */
	SELECTED_QUEST_REWARD7(14),
	/** 选择奖励 8。 Selected quest reward 8. */
	SELECTED_QUEST_REWARD8(15),
	/** 选择奖励 9。 Selected quest reward 9. */
	SELECTED_QUEST_REWARD9(16),
	/** 选择奖励 10。 Selected quest reward 10. */
	SELECTED_QUEST_REWARD10(17),
	/** 选择奖励 11。 Selected quest reward 11. */
	SELECTED_QUEST_REWARD11(18),
	/** 选择奖励 12。 Selected quest reward 12. */
	SELECTED_QUEST_REWARD12(19),
	/** 选择奖励 13。 Selected quest reward 13. */
	SELECTED_QUEST_REWARD13(20),
	/** 选择奖励 14。 Selected quest reward 14. */
	SELECTED_QUEST_REWARD14(21),
	/** 选择奖励 15。 Selected quest reward 15. */
	SELECTED_QUEST_REWARD15(22),
	/** 选择无奖励（4.3）。 Select no reward (4.3). */
	SELECT_NO_REWARD(23), // 4.3
	/** 无权限。 No rights. */
	NO_RIGHTS(27),
	/** 开始对话框（4.3）。 Start dialog (4.3). */
	START_DIALOG(31), // 4.3
	/** 检查已收集物品（4.3）。 Check collected items (4.3). */
	CHECK_COLLECTED_ITEMS(39), // 4.3

	/** 接受任务。 Accept quest. */
	ACCEPT_QUEST(1002),
	/** 拒绝任务。 Refuse quest. */
	REFUSE_QUEST(1003),
	/** 拒绝任务（备选）。 Refuse quest (alternate). */
	REFUSE_QUEST_2(1004),
	/** 询问是否接受。 Ask for acceptance. */
	ASK_ACCEPTION(1007),
	/** 结束对话框。 Finish dialog. */
	FINISH_DIALOG(1008),
	/** 打开奖励选择。 Open reward selection. */
	SELECT_REWARD(1009),

	/** 简易接受任务。 Simple accept quest. */
	ACCEPT_QUEST_SIMPLE(20000),
	/** 简易拒绝任务。 Simple refuse quest. */
	REFUSE_QUEST_SIMPLE(20001),
	/** 简易检查已收集物品。 Simple check collected items. */
	CHECK_COLLECTED_ITEMS_SIMPLE(20002),
	/** 推进到下一步。 Set progress to next step. */
	SETPRO_NEXT(20003),
	/** 检查 AP。 Check AP. */
	CHECK_AP(20004),
	/** 检查基纳。 Check gold. */
	CHECK_GOLD(20005),

	/** 脚本动作选择（dialog id = 名称后缀）。 Script action select (dialog id = name suffix). */
	SELECT_ACTION_1011(1011), SELECT_ACTION_1012(1012), SELECT_ACTION_1013(1013), SELECT_ACTION_1014(1014),
	SELECT_ACTION_1097(1097), SELECT_ACTION_1182(1182), SELECT_ACTION_1352(1352), SELECT_ACTION_1353(1353),
	SELECT_ACTION_1354(1354), SELECT_ACTION_1355(1355), SELECT_ACTION_1356(1356), SELECT_ACTION_1375(1375),
	SELECT_ACTION_1396(1396), SELECT_ACTION_1438(1438), SELECT_ACTION_1439(1439), SELECT_ACTION_1609(1609),
	SELECT_ACTION_1693(1693), SELECT_ACTION_1694(1694), SELECT_ACTION_1695(1695), SELECT_ACTION_1696(1696),
	SELECT_ACTION_1697(1697), SELECT_ACTION_1779(1779), SELECT_ACTION_1780(1780), SELECT_ACTION_1864(1864),
	SELECT_ACTION_1865(1865), SELECT_ACTION_1949(1949), SELECT_ACTION_1950(1950), SELECT_ACTION_2034(2034),
	SELECT_ACTION_2035(2035), SELECT_ACTION_2036(2036), SELECT_ACTION_2037(2037), SELECT_ACTION_2038(2038),
	SELECT_ACTION_2120(2120), SELECT_ACTION_2292(2292), SELECT_ACTION_2375(2375), SELECT_ACTION_2376(2376),
	SELECT_ACTION_2377(2377), SELECT_ACTION_2378(2378), SELECT_ACTION_2379(2379), SELECT_ACTION_2461(2461),
	SELECT_ACTION_2546(2546), SELECT_ACTION_2716(2716), SELECT_ACTION_2717(2717), SELECT_ACTION_2718(2718),
	SELECT_ACTION_2720(2720), SELECT_ACTION_3058(3058), SELECT_ACTION_3143(3143), SELECT_ACTION_3399(3399),
	SELECT_ACTION_3400(3400), SELECT_ACTION_3739(3739), SELECT_ACTION_3740(3740), SELECT_ACTION_3741(3741),
	SELECT_ACTION_4081(4081), SELECT_ACTION_4166(4166), SELECT_ACTION_4763(4763), SELECT_ACTION_6501(6501),
	SELECT_ACTION_6503(6503), SELECT_ACTION_6842(6842), SELECT_ACTION_6844(6844), SELECT_ACTION_7183(7183),
	SELECT_ACTION_7524(7524),

	/** 推进到步骤 1。 Step to 1. */
	STEP_TO_1(10000),
	/** 推进到步骤 2。 Step to 2. */
	STEP_TO_2(10001),
	/** 推进到步骤 3。 Step to 3. */
	STEP_TO_3(10002),
	/** 推进到步骤 4。 Step to 4. */
	STEP_TO_4(10003),
	/** 推进到步骤 5。 Step to 5. */
	STEP_TO_5(10004),
	/** 推进到步骤 6。 Step to 6. */
	STEP_TO_6(10005),
	/** 推进到步骤 7。 Step to 7. */
	STEP_TO_7(10006),
	/** 推进到步骤 8。 Step to 8. */
	STEP_TO_8(10007),
	/** 推进到步骤 9。 Step to 9. */
	STEP_TO_9(10008),
	/** 推进到步骤 10。 Step to 10. */
	STEP_TO_10(10009),
	/** 推进到步骤 11。 Step to 11. */
	STEP_TO_11(10010),
	/** 推进到步骤 12。 Step to 12. */
	STEP_TO_12(10011),
	/** 推进到步骤 13。 Step to 13. */
	STEP_TO_13(10012),
	/** 推进到步骤 14。 Step to 14. */
	STEP_TO_14(10013),
	/** 推进到步骤 15。 Step to 15. */
	STEP_TO_15(10014),
	/** 推进到步骤 16。 Step to 16. */
	STEP_TO_16(10015),
	/** 推进到步骤 17。 Step to 17. */
	STEP_TO_17(10016),
	/** 推进到步骤 18。 Step to 18. */
	STEP_TO_18(10017),
	/** 推进到步骤 19。 Step to 19. */
	STEP_TO_19(10018),
	/** 推进到步骤 20。 Step to 20. */
	STEP_TO_20(10019),
	/** 推进到步骤 21。 Step to 21. */
	STEP_TO_21(10020),
	/** 推进到步骤 30。 Step to 30. */
	STEP_TO_30(10029),
	/** 推进到步骤 31。 Step to 31. */
	STEP_TO_31(10030),
	/** 推进到步骤 40。 Step to 40. */
	STEP_TO_40(10039),
	/** 推进到步骤 41。 Step to 41. */
	STEP_TO_41(10040),
	/** 设置为可领奖。 Set reward-ready. */
	SET_REWARD(10255),

	/** 兑换硬币（4.3）。 Exchange coin (4.3). */
	EXCHANGE_COIN(59); // 4.3

	/** 客户端对话框 ID。 Client dialog id. */
	private int id;

	/**
	 * 使用给定对话框 ID 构造常量。
	 * Constructs a constant with the given dialog id.
	 *
	 * @param id 对话框 ID / Dialog id
	 */
	private QuestDialog(int id) {
		this.id = id;
	}

	/**
	 * 返回客户端对话框 ID。
	 * Returns the client dialog id.
	 *
	 * @return 对话框 ID / Dialog id
	 */
	public int id() {
		return id;
	}
}
