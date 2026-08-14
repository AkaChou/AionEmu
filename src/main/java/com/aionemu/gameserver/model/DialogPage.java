package com.aionemu.gameserver.model;

/**
 * 对话页面枚举。
 * Dialog page enumeration.
 */
public enum DialogPage {
	/** 空 / Null */
	NULL(DialogAction.NULL, 0),
	/** 烙印之石窗口 / Stigma window */
	STIGMA(DialogAction.OPEN_STIGMA_WINDOW, 1),
	/** 创建军团 / Create legion*/
	CREATE_LEGION(DialogAction.CREATE_LEGION, 2),
	/** 商店 / Vendor */
	VENDOR(DialogAction.OPEN_VENDOR, 13),
	/** 取出角色仓库 / Retrieve character warehouse */
	RETRIEVE_CHAR_WAREHOUSE(DialogAction.RETRIEVE_CHAR_WAREHOUSE, 14),
	/** 存入角色仓库 / Deposit character warehouse */
	DEPOSIT_CHAR_WAREHOUSE(DialogAction.DEPOSIT_CHAR_WAREHOUSE, 15),
	/** 取出账号仓库 / Retrieve account warehouse*/
	RETRIEVE_ACCOUNT_WAREHOUSE(DialogAction.RETRIEVE_ACCOUNT_WAREHOUSE, 16),
	/** 存入账号仓库 / Deposit account warehouse*/
	DEPOSIT_ACCOUNT_WAREHOUSE(DialogAction.DEPOSIT_ACCOUNT_WAREHOUSE, 17),
	/** 邮箱 / Mail */
	MAIL(DialogAction.OPEN_POSTBOX, 18),
	/** 更换物品外观 / Change item skin*/
	CHANGE_ITEM_SKIN(DialogAction.CHANGE_ITEM_SKIN, 19),
	/** 移除魔石 / Remove manastone */
	REMOVE_MANASTONE(DialogAction.REMOVE_MANASTONE, 20),
	/** 镶嵌神石 / Give item proc (godstone) */
	GIVE_ITEM_PROC(DialogAction.GIVE_ITEM_PROC, 21),
	/** 采集技能升级 / Gather skill level-up */
	GATHER_SKILL_LEVELUP(DialogAction.GATHER_SKILL_LEVELUP, 23),
	/** 拾取 / Loot */
	LOOT(DialogAction.NULL, 24),
	/** 军团仓库 / Legion warehouse */
	LEGION_WAREHOUSE(DialogAction.OPEN_LEGION_WAREHOUSE, 25),
	/** 个人仓库 / Personal warehouse*/
	PERSONAL_WAREHOUSE(DialogAction.OPEN_PERSONAL_WAREHOUSE, 26),
	/** 武器合成 / Compound weapon */
	COMPOUND_WEAPON(DialogAction.COMPOUND_WEAPON, 29),
	/** 武器拆解 / Decompound weapon*/
	DECOMPOUND_WEAPON(DialogAction.DECOMPOUND_WEAPON, 30),
	/** 房屋标记 / Housing marker */
	HOUSING_MARKER(DialogAction.NULL, 32),
	/** 房屋时限 / Housing lifetime*/
	HOUSING_LIFETIME(DialogAction.NULL, 33),
	/** 物品充能 / Charge item */
	CHARGE_ITEM(DialogAction.NULL, 35),
	/** 房屋好友列表 / Housing friend list */
	HOUSING_FRIENDLIST(DialogAction.HOUSING_FRIENDLIST, 36),
	/** 房屋邮筒 / Housing post */
	HOUSING_POST(DialogAction.NULL, 37),
	/** 房屋拍卖 / Housing auction*/
	HOUSING_AUCTION(DialogAction.HOUSING_PERSONAL_AUCTION, 38),
	/** 房屋缴租 / Housing pay rent */
	HOUSING_PAY_RENT(DialogAction.HOUSING_PAY_RENT, 39),
	/** 房屋驱逐 / Housing kick*/
	HOUSING_KICK(DialogAction.HOUSING_KICK, 40),
	/** 房屋配置 / Housing config */
	HOUSING_CONFIG(DialogAction.HOUSING_CONFIG, 41),
	/** 城镇挑战任务 / Town challenge task*/
	TOWN_CHALLENGE_TASK(DialogAction.QUEST_BOARD, 43),
	/** 转移物品外观 / Move item skin */
	MOVE_ITEM_SKIN(DialogAction.MOVE_ITEM_SKIN, 51),
	/** 物品升级 / Item upgrade*/
	ITEM_UPGRADE(DialogAction.ITEM_UPGRADE, 52),
	/** 打开烙印之石强化 / Open stigma enchant */
	OPEN_STIGMA_ENCHANT(DialogAction.OPEN_STIGMA_ENCHANT, 53);

	private int id;
	private DialogAction action;

	private DialogPage(DialogAction action, int id) {
		this.id = id;
		this.action = action;
	}

	/**
	 * 返回页面 ID。
	 * Returns the page id.
	 *
	 * @return 页面 ID / page id
	 */
	public int id() {
		return id;
	}

	/**
	 * 返回对话动作 ID。
	 * Returns the dialog action id.
	 *
	 * @return 对话动作 ID / action id
	 */
	public int actionId() {
		return action.id();
	}

	/**
	 * 按对话动作 ID 查找页面；找不到返回 {@link #NULL}。
	 * Finds page by dialog action id; returns {@link #NULL} if absent.
	 *
	 * @param dialogId 对话动作 ID / dialog action id
	 * @return 对话页面 / dialog page
	 */
	public static DialogPage getPageByAction(int dialogId) {
		for (DialogPage page : values()) {
			if (page.actionId() == dialogId) {
				return page;
			}
		}
		return NULL;
	}
}
