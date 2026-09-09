package com.aionemu.gameserver.model.account;

import lombok.Getter;

/**
 * 角色封禁信息。
 * Character ban information.
 *
 * @author nrg
 */
@Getter
public class CharacterBanInfo {

	private final int playerId;
	private final long start;
	private final long end;
	private final String reason;

	public CharacterBanInfo(int playerId, long start, long duration, String reason) {
		this.playerId = playerId;
		this.start = start;
		this.end = duration + start;
		this.reason = (reason.equals("") ? "You are suspected to have violated the server's rules" : reason);
	}

}
