package com.aionemu.gameserver.model.account;

/**
 * 角色封禁信息，用于账号相关逻辑。
 * Character Ban Info for account logic.
 *
 * @author nrg
 */
public class CharacterBanInfo {

	private int playerId;
	private long start;
	private long end;
	private String reason;

	public CharacterBanInfo(int playerId, long start, long duration, String reason) {
		this.playerId = playerId;
		this.start = start;
		this.end = duration + start;
		this.reason = (reason.equals("") ? "You are suspected to have violated the server's rules" : reason);
	}

	/**
	 * @return the playerId
	 */
	public int getPlayerId() {
		return playerId;
	}

	/**
	 * @return the start
	 */
	public long getStart() {
		return start;
	}

	/**
	 * @return the end
	 */
	public long getEnd() {
		return end;
	}

	/**
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}
}
