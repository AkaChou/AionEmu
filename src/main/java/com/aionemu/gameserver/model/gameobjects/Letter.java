package com.aionemu.gameserver.model.gameobjects;

import java.sql.Timestamp;
import lombok.Getter;

/**
 * 信件游戏对象。
 * Letter game object.
 */

public class Letter extends AionObject {
	/** 返回收件人 ID / Returns the recipient id */
	@Getter
	private final int recipientId;
	/** 返回附带的物品 / Returns the attached item */
	@Getter
	private Item attachedItem;
	private long attachedKinahCount;
	private long attachedAPCount;
	/** 返回发件人名称 / Returns the sender name */
	@Getter
	private final String senderName;
	/** 获取称号。 / Returns the title. */
	@Getter
	private final String title;
	/** 获取消息。 / Returns the message. */
	@Getter
	private final String message;
	/** 是否未读 / Whether unread */
	@Getter
	private boolean unread;
	/**
	 * 是否急件。
	 * Whether the letter is express.
	 *
	 * @return 是否急件 / whether express
	 */
	@Getter
	private boolean express;
	/** 返回时间戳 / Returns the time stamp. */
	@Getter
	private final Timestamp timeStamp;
	private PersistentState persistentState;
	/** 获取信件类型。 / Returns the letter type. */
	@Getter
	private LetterType letterType;

	public Letter(int objId, int recipientId, Item attachedItem, long attachedKinahCount, long attachedAPCount,
			String title, String message, String senderName, Timestamp timeStamp, boolean unread,
			LetterType letterType) {
		super(objId);
		this.express = letterType == LetterType.EXPRESS || letterType == LetterType.BLACKCLOUD;
		this.recipientId = recipientId;
		this.attachedItem = attachedItem;
		this.attachedKinahCount = attachedKinahCount;
		this.attachedAPCount = attachedAPCount;
		this.title = title;
		this.message = message;
		this.senderName = senderName;
		this.timeStamp = timeStamp;
		this.unread = unread;
		this.persistentState = PersistentState.NEW;
		this.letterType = letterType;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return String.valueOf(attachedItem.getItemTemplate().getNameId());
	}

	/** 返回附带的基纳 / Returns the attached kinah */
	public long getAttachedKinah() {
		return attachedKinahCount;
	}

	/** 返回附带的 AP / Returns the attached ap */
	public long getAttachedAp() {
		return attachedAPCount;
	}

	/** 标记为已读 / Marks the letter as read */
	public void setReadLetter() {
		this.unread = false;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/** 设置是否急件 / Sets whether express */
	public void setExpress(boolean express) {
		this.express = express;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/** 设置信件类型。 / Sets the letter type. */
	public void setLetterType(LetterType letterType) {
		this.letterType = letterType;
		this.express = letterType == LetterType.EXPRESS || letterType == LetterType.BLACKCLOUD;
	}

	/** 返回信件持久化状态 / Returns the letter persistent state */
	public PersistentState getLetterPersistentState() {
		return persistentState;
	}

	/** 移除附带的物品 / Removes the attached item */
	public void removeAttachedItem() {
		this.attachedItem = null;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/** 移除附带的基纳 / Removes the attached kinah */
	public void removeAttachedKinah() {
		this.attachedKinahCount = 0;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/** 移除附带的 AP / Removes the attached ap */
	public void removeAttachedAP() {
		this.attachedAPCount = 0;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/**
	 * 恢复信件附件与持久化状态（用于数据修复场景）。
	 * Restores the letter's attachments and persistent state (used for data repair).
	 *
	 * @param item 附带的物品 / attached item
	 * @param kinah 附带的基纳 / attached Kinah
	 * @param ap 附带的 AP / attached AP
	 * @param state 持久化状态 / persistent state
	 */
	public void restoreAttachments(Item item, long kinah, long ap, PersistentState state) {
		this.attachedItem = item;
		this.attachedKinahCount = kinah;
		this.attachedAPCount = ap;
		this.persistentState = state;
	}

	/** 删除。 / Delete. */
	public void delete() {
		this.persistentState = PersistentState.DELETED;
	}

	/** 设置持久化状态 / Sets the persist state */
	public void setPersistState(PersistentState state) {
		this.persistentState = state;
	}
}
