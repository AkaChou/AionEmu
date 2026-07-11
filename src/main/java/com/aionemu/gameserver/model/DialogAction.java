package com.aionemu.gameserver.model;

/**
 * 对话动作枚举。
 * Dialog Action enumeration.
 */

public enum DialogAction {
	ERROR(-1), NULL(1), BUY(2), SELL(3), OPEN_STIGMA_WINDOW(4), CREATE_LEGION(5), DISPERSE_LEGION(6),
	RECREATE_LEGION(7), RESURRECT_PET(19), RETRIEVE_CHAR_WAREHOUSE(25), // 4.3
	DEPOSIT_CHAR_WAREHOUSE(26), // 4.3
	RETRIEVE_ACCOUNT_WAREHOUSE(27), // 4.3
	DEPOSIT_ACCOUNT_WAREHOUSE(28), // 4.3
	OPEN_VENDOR(33), // 4.3
	RESURRECT_BIND(34), // 4.3
	RECOVERY(35), // 4.3
	ENTER_PVP(36), // 4.3
	LEAVE_PVP(37), // 4.3
	OPEN_POSTBOX(38), // 4.3
	DIC(40), // 4.3
	GIVE_ITEM_PROC(41), // 4.3
	REMOVE_MANASTONE(42), // 4.3
	CHANGE_ITEM_SKIN(43), // 4.3
	AIRLINE_SERVICE(44), // 4.3
	GATHER_SKILL_LEVELUP(45), // 4.3
	COMBINE_SKILL_LEVELUP(46), // 4.3
	EXTEND_INVENTORY(47), // 4.3
	EXTEND_CHAR_WAREHOUSE(48), // 4.3
	EXTEND_ACCOUNT_WAREHOUSE(49), // 4.3
	LEGION_LEVELUP(50), // 4.3
	LEGION_CREATE_EMBLEM(51), // 4.3
	LEGION_CHANGE_EMBLEM(52), // 4.3
	OPEN_LEGION_WAREHOUSE(53), // 4.3
	OPEN_PERSONAL_WAREHOUSE(54), // 4.3
	BUY_BY_AP(55), // 4.3
	CLOSE_LEGION_WAREHOUSE(56), // 4.3
	PASS_DOORMAN(57), // 4.3
	/** 制作。 / Craft. */
	CRAFT(58), // 4.3
	/** 兑换硬币 / Exchange Coin */
	EXCHANGE_COIN(59), // 4.3
	/** 播放动画 / Show Movie*/
	SHOW_MOVIE(60), // 4.3
	/** 编辑角色 / Edit Character*/
	EDIT_CHARACTER(61), // 4.3
	/** 编辑性别 / Edit Gender*/
	EDIT_GENDER(62), // 4.3
	/** 红娘 / Match Maker*/
	MATCH_MAKER(63), // 4.3
	/** 成为佣兵 / Make Mercenary*/
	MAKE_MERCENARY(64), // 4.3
	/** 副本条目。 / Instance Entry. */
	INSTANCE_ENTRY(65), // 4.3
	/** 合成武器 / Compound Weapon*/
	COMPOUND_WEAPON(66), // 4.3
	/** 武器拆解 / Decompound Weapon*/
	DECOMPOUND_WEAPON(67), // 4.3
	/** 势力加入 / Faction Join*/
	FACTION_JOIN(68), // 4.3
	/** 势力分离 / Faction Separate*/
	FACTION_SEPARATE(69), // 4.3
	/** 购买再次 / Buy Again*/
	BUY_AGAIN(70), // 4.3
	/** 宠物收养 / Pet Adopt*/
	PET_ADOPT(71), // 4.3
	/** 宠物遗弃 / Pet Abandon*/
	PET_ABANDON(72), // 4.3
	/** 房屋建造 / Housing Build*/
	HOUSING_BUILD(73), // 4.3
	/** 房屋拆除 / Housing Destruct*/
	HOUSING_DESTRUCT(74), // 4.3
	/** 充能物品单个 / Charge Item Single*/
	CHARGE_ITEM_SINGLE(75), // 4.3
	/** 充能物品多个 / Charge Item Multi*/
	CHARGE_ITEM_MULTI(76), // 4.3
	/** 副本组队匹配 / Instance Party Match*/
	INSTANCE_PARTY_MATCH(77), // 4.3
	/** 交易 / Trade In*/
	TRADE_IN(78), // 4.3
	/** 放弃制作专家 / Giveup Craft Expert*/
	GIVEUP_CRAFT_EXPERT(79), // 4.3
	/** 放弃制作大师 / Giveup Craft Master*/
	GIVEUP_CRAFT_MASTER(80), // 4.3
	/** 房屋好友列表 / Housing Friendlist*/
	HOUSING_FRIENDLIST(81), // 4.3
	/** 房屋随机传送 / Housing Random Teleport*/
	HOUSING_RANDOM_TELEPORT(82), // 4.3
	/** 房屋个人副本传送 / Housing Personal Ins Teleport*/
	HOUSING_PERSONAL_INS_TELEPORT(83), // 4.3
	/** 房屋个人拍卖 / Housing Personal Auction*/
	HOUSING_PERSONAL_AUCTION(84), // 4.3
	/** 房屋支付租金 / Housing Pay Rent*/
	HOUSING_PAY_RENT(85), // 4.3
	/** 房屋驱逐 / Housing Kick*/
	HOUSING_KICK(86), // 4.3
	/** 房屋改建 / Housing Change Building*/
	HOUSING_CHANGE_BUILDING(87), // 4.3
	/** 住房配置。 / Housing Config. */
	HOUSING_CONFIG(88), // 4.3
	/** 放弃房屋 / Housing Giveup*/
	HOUSING_GIVEUP(89), // 4.3
	/** 取消放弃房屋 / Housing Cancel Giveup*/
	HOUSING_CANCEL_GIVEUP(90), // 4.3
	/** 创建房屋个人副本 / Housing Create Personal Ins*/
	HOUSING_CREATE_PERSONAL_INS(91), // 4.3
	/** 收养跟班 / Sidekick Adopt*/
	SIDEKICK_ADOPT(92), // 4.3
	/** 遗弃跟班 / Sidekick Abandon*/
	SIDEKICK_ABANDON(93), // 4.3
	/** 注能物品单个 / Augment Item Single*/
	AUGMENT_ITEM_SINGLE(94), // 4.3
	/** 注能物品多个 / Augment Item Multi*/
	AUGMENT_ITEM_MULTI(95), // 4.3
	/** 房屋工作室 / Housing Studio*/
	HOUSING_STUDIO(96), // 4.3
	/** 房屋点赞 / Housing Like */
	HOUSING_LIKE(97), // 4.3
	/** 房屋脚本 / Housing Script */
	HOUSING_SCRIPT(98), // 4.3
	/** 房屋留言簿 / Housing Guestbook */
	HOUSING_GUESTBOOK(99), // 4.3
	/** 任务公告板 / Quest Board*/
	QUEST_BOARD(100), // 4.3
	/** 欧比斯点出售 / Ap Selling */
	AP_SELLING(101), // 4.3
	/** 购买列表 / Purchase List */
	PURCHASE_LIST(103), // 4.3
	/** 简单传送 / Teleport Simple */
	TELEPORT_SIMPLE(104), // 4.3
	/** 打开副本招募 / Open Instance Recruit */
	OPEN_INSTANCE_RECRUIT(105), // 4.3
	/** 转移物品外观 / Move Item Skin*/
	MOVE_ITEM_SKIN(106), // 4.7
	/** 交易升级 / Trade In Upgrade*/
	TRADE_IN_UPGRADE(107), // 4.7
	/** 自动奖励。 / Auto Reward. */
	AUTO_REWARD(108), // 4.7
	/** 物品升级 / Item Upgrade*/
	ITEM_UPGRADE(109), // 4.7

	// 选择 Boss 等级 4.7 / Select Boss Level 4.7
	/** 查询首领等级1 / Select Boss Level 1*/
	SELECT_BOSS_LEVEL_1(20006), SELECT_BOSS_LEVEL_2(20007), SELECT_BOSS_LEVEL_3(20008), SELECT_BOSS_LEVEL_4(20009),
	/** 查询首领等级5 / Select Boss Level 5*/
	SELECT_BOSS_LEVEL_5(20010),

	// 任务自动奖励 4.7 / Quest Auto Reward 4.7
	/** 任务自动奖励 1 / Quest Auto Reward 1 */
	QUEST_AUTO_REWARD_1(110), QUEST_AUTO_REWARD_2(111), QUEST_AUTO_REWARD_3(112), QUEST_AUTO_REWARD_4(113),
	/** 任务自动奖励 5 / Quest Auto Reward 5 */
	QUEST_AUTO_REWARD_5(114), QUEST_AUTO_REWARD_6(115), QUEST_AUTO_REWARD_7(116), QUEST_AUTO_REWARD_8(117),
	/** 任务自动奖励 9 / Quest Auto Reward 9 */
	QUEST_AUTO_REWARD_9(118), QUEST_AUTO_REWARD_10(119), QUEST_AUTO_REWARD_11(120), QUEST_AUTO_REWARD_12(121),
	/** 任务自动奖励 13 / Quest Auto Reward 13 */
	QUEST_AUTO_REWARD_13(122), QUEST_AUTO_REWARD_14(123), QUEST_AUTO_REWARD_15(124), OPEN_STIGMA_ENCHANT(125), // 4.8
	/** 物品替换 / Item Replace*/
	ITEM_REPLACE(126), // 5.6
	/** 任务基纳奖励 / Quest Gold Reward*/
	QUEST_GOLD_REWARD(127), // 5.6
	/** Recovery 2 / Recovery 2 */
	RECOVERY_2(128); // 5.8

	private int id;

	private DialogAction(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int id() {
		return id;
	}
}
