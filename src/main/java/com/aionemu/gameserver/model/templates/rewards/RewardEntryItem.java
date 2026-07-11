package com.aionemu.gameserver.model.templates.rewards;

/**
 * 奖励条目物品模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author KID
 */
public class RewardEntryItem {
	public RewardEntryItem(int unique, int item_id, long count) {
		this.unique = unique;
		this.id = item_id;
		this.count = count;
	}

	public int id, unique;
	public long count;
}
