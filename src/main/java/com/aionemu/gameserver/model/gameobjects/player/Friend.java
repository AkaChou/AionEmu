package com.aionemu.gameserver.model.gameobjects.player;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.FriendList.Status;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * Friend 游戏对象。
 * Friend game object.
 */
@Slf4j

public class Friend {
	private PlayerCommonData pcd;
	private String friendNote = "";

	public Friend(PlayerCommonData pcd) {
		this.pcd = pcd;
	}

	/** 获取状态。 / Returns the status. */
	public Status getStatus() {
		if (pcd.getPlayer() == null || !pcd.isOnline()) {
			return FriendList.Status.OFFLINE;
		}
		return pcd.getPlayer().getFriendList().getStatus();
	}

	/** 设置 pcd / Sets the pcd */
	public void setPCD(PlayerCommonData pcd) {
		this.pcd = pcd;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return pcd.getName();
	}

	/** 获取等级。 / Returns the level. */
	public int getLevel() {
		return pcd.getLevel();
	}

	/** 返回 note / Returns the note */
	public String getNote() {
		return pcd.getNote();
	}

	/** 获取玩家职业。 / Returns the player class. */
	public PlayerClass getPlayerClass() {
		return pcd.getPlayerClass();
	}

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		WorldPosition position = pcd.getPosition();
		if (position == null) {
			log.warn(I18n.get("log.479062c40d5a", pcd.getPlayerObjId()));
			return 0;
		}
		return position.getMapId();
	}

	/** 返回 last online time / Returns the last online time */
	public int getLastOnlineTime() {
		if (pcd.getLastOnline() == null || isOnline()) {
			return 0;
		}
		return (int) (pcd.getLastOnline().getTime() / 1000);
	}

	/** 返回 oid / Returns the oid */
	public int getOid() {
		return pcd.getPlayerObjId();
	}

	/** 获取玩家。 / Returns the player. */
	public Player getPlayer() {
		return pcd.getPlayer();
	}

	/** 是否在线。 / Whether Online. */
	public boolean isOnline() {
		return pcd.isOnline();
	}

	/** 返回 friend note / Returns the friend note */
	public String getFriendNote() {
		return friendNote;
	}

	/** 设置 note / Sets the note */
	public void setNote(String note) {
		friendNote = note;
	}
}
