package com.aionemu.gameserver.model.account;

import lombok.Getter;
import lombok.Setter;

/**
 * 角色安全密码，用于账号相关逻辑。
 * Character Passkey for account logic.
 *
 * @author cura
 */
public class CharacterPasskey {

	@Getter
	@Setter
	private int objectId;
	@Getter
	@Setter
	private int wrongCount = 0;
	@Getter
	private boolean isPass = false;
	@Getter
	@Setter
	private ConnectType connectType;

	/**
	 * @param isPass the isPass to set
	 */
	public void setIsPass(boolean isPass) {
		this.isPass = isPass;
	}

	public enum ConnectType {
		ENTER, DELETE
	}
}
