package com.aionemu.gameserver.model.gameobjects;

import java.sql.Timestamp;

/**
 * 信件游戏对象。
 * Letter game object.
 */

public class Letter extends AionObject {
	private int recipientId;
	private Item attachedItem;
	private long attachedKinahCount;
	private long attachedAPCount;
	private String senderName;
	private String title;
	private String message;
	private boolean unread;
	private boolean express;
	private Timestamp timeStamp;
	private PersistentState persistentState;
	private LetterType letterType;

	public Letter(int objId, int recipientId, Item attachedItem, long attachedKinahCount, long attachedAPCount,
			String title, String message, String senderName, Timestamp timeStamp, boolean unread,
			LetterType letterType) {
		super(objId);
		if (letterType == LetterType.EXPRESS || letterType == LetterType.BLACKCLOUD) {
			this.express = true;
		} else {
			this.express = false;
		}
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

	/** 返回 recipient id / Returns the recipient id */
	public int getRecipientId() {
		return recipientId;
	}

	/** 返回 attached item / Returns the attached item */
	public Item getAttachedItem() {
		return attachedItem;
	}

	/** 返回 attached kinah / Returns the attached kinah */
	public long getAttachedKinah() {
		return attachedKinahCount;
	}

	/** 返回 attached ap / Returns the attached ap */
	public long getAttachedAp() {
		return attachedAPCount;
	}

	/** 获取称号。 / Returns the title. */
	public String getTitle() {
		return title;
	}

	/** 获取消息。 / Returns the message. */
	public String getMessage() {
		return message;
	}

	/** 返回 sender name / Returns the sender name */
	public String getSenderName() {
		return senderName;
	}

	/** 获取信件类型。 / Returns the letter type. */
	public LetterType getLetterType() {
		return letterType;
	}

	/** Whether 未读 / Whether unread */
	public boolean isUnread() {
		return unread;
	}

	/** 设置 read letter / Sets the read letter */
	public void setReadLetter() {
		this.unread = false;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/**
	 * @return Whether express
	 */
	public boolean isExpress() {
		return express;
	}

	/** 设置 express / Sets the express */
	public void setExpress(boolean express) {
		this.express = express;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/** 设置信件类型。 / Sets the letter type. */
	public void setLetterType(LetterType letterType) {
		this.letterType = letterType;
		if (letterType == LetterType.EXPRESS || letterType == LetterType.BLACKCLOUD) {
			this.express = true;
		} else {
			this.express = false;
		}
	}

	/** 返回 letter persistent state / Returns the letter persistent state */
	public PersistentState getLetterPersistentState() {
		return persistentState;
	}

	/** 移除附件物品 / Removes attached item */
	public void removeAttachedItem() {
		this.attachedItem = null;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/** 移除 attached kinah / Removes attached kinah */
	public void removeAttachedKinah() {
		this.attachedKinahCount = 0;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/** 移除 attached ap / Removes attached ap */
	public void removeAttachedAP() {
		this.attachedAPCount = 0;
		this.persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/** 删除。 / Delete. */
	public void delete() {
		this.persistentState = PersistentState.DELETED;
	}

	/** 设置 persist state / Sets the persist state */
	public void setPersistState(PersistentState state) {
		this.persistentState = state;
	}

	/** 返回时间盖章 / Returns the time stamp*/
	public Timestamp getTimeStamp() {
		return timeStamp;
	}
}
