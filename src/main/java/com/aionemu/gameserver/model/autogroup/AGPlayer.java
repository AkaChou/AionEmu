package com.aionemu.gameserver.model.autogroup;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * AG 玩家，用于 autogroup 相关逻辑。
 * AG Player for autogroup logic.
 */

public class AGPlayer {
	private Integer objectId;
	private Race race;
	private PlayerClass playerClass;
	private String name;
	private boolean isInInstance;
	private boolean isOnline;
	private boolean isPressEnter;

	public AGPlayer(Player player) {
		objectId = player.getObjectId();
		race = player.getRace();
		playerClass = player.getPlayerClass();
		name = player.getName();
		isOnline = true;
	}

	/** 返回对象 ID / Returns the object id */
	public Integer getObjectId() {
		return objectId;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 获取玩家职业。 / Returns the player class. */
	public PlayerClass getPlayerClass() {
		return playerClass;
	}

	/** 设置实例 / Sets the in instance*/
	public void setInInstance(boolean result) {
		isInInstance = result;
	}

	/** 是否实例 / Whether in instance*/
	public boolean isInInstance() {
		return isInInstance;
	}

	/** 是否在线。 / Whether Online. */
	public boolean isOnline() {
		return isOnline;
	}

	/** 设置 online / Sets the online */
	public void setOnline(boolean result) {
		isOnline = result;
	}

	/**
	 * @return Whether pressed enter / Whether pressed enter
	 */
	public boolean isPressedEnter() {
		return isPressEnter;
	}

	/** 设置 press enter / Sets the press enter */
	public void setPressEnter(boolean result) {
		isPressEnter = result;
	}
}
