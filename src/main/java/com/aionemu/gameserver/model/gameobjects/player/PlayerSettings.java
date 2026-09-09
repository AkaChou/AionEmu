package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 玩家 Settings 游戏对象。
 * Player Settings game object.
 *
 * @author ATracer
 */
@NoArgsConstructor
public class PlayerSettings {

	/**
	 * @return the persistentState
	 */
	@Getter
	@Setter
	private PersistentState persistentState;

	/**
	 * @return the uiSettings
	 */
	@Getter
	private byte[] uiSettings;
	/**
	 * @return the shortcuts
	 */
	@Getter
	private byte[] shortcuts;
	/**
	 * @return the houseBuddies
	 */
	@Getter
	private byte[] houseBuddies;
	/**
	 * @return the deny
	 */
	@Getter
	private int deny = 0;
	/**
	 * @return the display
	 */
	@Getter
	private int display = 0;

	public PlayerSettings(byte[] uiSettings, byte[] shortcuts, byte[] houseBuddies, int deny, int display) {
		this.uiSettings = uiSettings;
		this.shortcuts = shortcuts;
		this.houseBuddies = houseBuddies;
		this.deny = deny;
		this.display = display;
	}

	/**
	 * @param uiSettings the uiSettings to set
	 */
	public void setUiSettings(byte[] uiSettings) {
		this.uiSettings = uiSettings;
		persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/**
	 * @param shortcuts the shortcuts to set
	 */
	public void setShortcuts(byte[] shortcuts) {
		this.shortcuts = shortcuts;
		persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/**
	 * @param houseBuddies the houseBuddies to set
	 */
	public void setHouseBuddies(byte[] houseBuddies) {
		this.houseBuddies = houseBuddies;
		persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/**
	 * @param display the display to set
	 */
	public void setDisplay(int display) {
		this.display = display;
		persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/**
	 * @param deny the deny to set
	 */
	public void setDeny(int deny) {
		this.deny = deny;
		persistentState = PersistentState.UPDATE_REQUIRED;
	}

	/**
	 * @param deny 是否处于指定拒绝状态。 / Whether in denied status
	  */
	public boolean isInDeniedStatus(DeniedStatus deny) {
		int isDeniedStatus = this.deny & deny.getId();

		return isDeniedStatus == deny.getId();
	}
}
