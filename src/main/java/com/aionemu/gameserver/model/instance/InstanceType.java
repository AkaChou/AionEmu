package com.aionemu.gameserver.model.instance;

/**
 * 副本类型枚举。
 * Instance Type enumeration.
 */

public enum InstanceType {
	/** Lf1 / Lf1 */
	LF1, SCORE, ARENA, NORMAL, INVASION, DREADGION, ARENA_PVP, TOURNAMENT, ARENA_TEAM, TIME_ATTACK, BATTLEFIELD;

	/**
	 * @return Whether dark poeta instance
	 */
	public boolean isDarkPoetaInstance() {
		return this.equals(InstanceType.LF1);
	}

	/**
	 * @return Whether score instance
	 */
	public boolean isScoreInstance() {
		return this.equals(InstanceType.SCORE);
	}

	/** 是否竞技场实例 / Whether arena instance*/
	public boolean isArenaInstance() {
		return this.equals(InstanceType.ARENA);
	}

	/**
	 * @return Whether normal instance
	 */
	public boolean isNormalInstance() {
		return this.equals(InstanceType.NORMAL);
	}

	/**
	 * @return Whether invasion instance
	 */
	public boolean isInvasionInstance() {
		return this.equals(InstanceType.INVASION);
	}

	/**
	 * @return Whether dreadgion instance
	 */
	public boolean isDreadgionInstance() {
		return this.equals(InstanceType.DREADGION);
	}

	/**
	 * @return Whether arena pv p instance
	 */
	public boolean isArenaPvPInstance() {
		return this.equals(InstanceType.ARENA_PVP);
	}

	/**
	 * @return Whether tournament instance
	 */
	public boolean isTournamentInstance() {
		return this.equals(InstanceType.TOURNAMENT);
	}

	/**
	 * @return Whether arena team instance
	 */
	public boolean isArenaTeamInstance() {
		return this.equals(InstanceType.ARENA_TEAM);
	}

	/**
	 * @return Whether time attack instance
	 */
	public boolean isTimeAttackInstance() {
		return this.equals(InstanceType.TIME_ATTACK);
	}

	/**
	 * @return Whether battlefield instance
	 */
	public boolean isBattlefieldInstance() {
		return this.equals(InstanceType.BATTLEFIELD);
	}
}
