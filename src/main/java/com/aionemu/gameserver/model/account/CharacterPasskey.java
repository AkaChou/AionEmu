package com.aionemu.gameserver.model.account;

import lombok.Getter;
import lombok.Setter;

/**
 * 角色安全密码，用于账号相关逻辑。
 * Character Passkey for account logic.
 * @author cura
 */
@Getter
@Setter
public class CharacterPasskey {

	private int objectId;
	private int wrongCount = 0;
	private boolean isPass = false;
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
