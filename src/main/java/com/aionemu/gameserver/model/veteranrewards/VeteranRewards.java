package com.aionemu.gameserver.model.veteranrewards;

/**
 * 老兵奖励，用于 veteranrewards 相关逻辑。
 * Veteran Rewards for veteranrewards logic.
 */

public class VeteranRewards {
	private int id;
	private String Player;
	private int type;
	private int item;
	private int count;
	private int kinah;
	private String Sender;
	private String Title;
	private String Message;

	public VeteranRewards(String Player, int type, int item, int count, int kinah, String Sender, String Title,
			String Message) {
		this.Player = Player;
		this.type = type;
		this.item = item;
		this.count = count;
		this.kinah = kinah;
		this.Sender = Sender;
		this.Title = Title;
		this.Message = Message;
	}

	public VeteranRewards(int id, String Player, int type, int item, int count, int kinah, String Sender, String Title,
			String Message) {
		this.id = id;
		this.Player = Player;
		this.type = type;
		this.item = item;
		this.count = count;
		this.kinah = kinah;
		this.Sender = Sender;
		this.Title = Title;
		this.Message = Message;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		if (id != 0) {
			return id;
		} else {
			return -1;
		}
	}

	/** 获取玩家。 / Returns the player. */
	public String getPlayer() {
		return Player;
	}

	/** 获取类型。 / Returns the type. */
	public int getType() {
		return type;
	}

	/** 获取物品。 / Returns the item. */
	public int getItem() {
		return item;
	}

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}

	/** 获取基纳。 / Returns the kinah. */
	public int getKinah() {
		return kinah;
	}

	/** 返回 sender / Returns the sender */
	public String getSender() {
		return Sender;
	}

	/** 获取称号。 / Returns the title. */
	public String getTitle() {
		return Title;
	}

	/** 获取消息。 / Returns the message. */
	public String getMessage() {
		return Message;
	}
}
