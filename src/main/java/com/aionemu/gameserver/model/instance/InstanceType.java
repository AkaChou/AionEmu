package com.aionemu.gameserver.model.instance;

/**
 * 副本类型枚举。
 * Instance Type enumeration.
 */

public enum InstanceType {
	/** 黑暗普埃塔副本 / Dark Poeta (LF1). */
	LF1,
	/** 计分副本 / Score instance. */
	SCORE,
	/** 竞技场 / Arena instance. */
	ARENA,
	/** 普通副本 / Normal instance. */
	NORMAL,
	/** 入侵副本 / Invasion instance. */
	INVASION,
	/** 德雷德吉翁副本 / Dredgion instance. */
	DREADGION,
	/** PvP 竞技场 / Arena PvP instance. */
	ARENA_PVP,
	/** 锦标赛 / Tournament instance. */
	TOURNAMENT,
	/** 团队竞技场 / Arena team instance. */
	ARENA_TEAM,
	/** 限时挑战 / Time attack instance. */
	TIME_ATTACK,
	/** 战场 / Battlefield instance. */
	BATTLEFIELD;

	/**
	 * @return 是否黑暗普埃塔副本 / Whether dark poeta instance
	 */
	public boolean isDarkPoetaInstance() {
		return this.equals(InstanceType.LF1);
	}

	/**
	 * @return 是否计分副本 / Whether score instance
	 */
	public boolean isScoreInstance() {
		return this.equals(InstanceType.SCORE);
	}

	/** 是否竞技场实例。 / Whether arena instance. */
	public boolean isArenaInstance() {
		return this.equals(InstanceType.ARENA);
	}

	/**
	 * @return 是否普通副本 / Whether normal instance
	 */
	public boolean isNormalInstance() {
		return this.equals(InstanceType.NORMAL);
	}

	/**
	 * @return 是否入侵副本 / Whether invasion instance
	 */
	public boolean isInvasionInstance() {
		return this.equals(InstanceType.INVASION);
	}

	/**
	 * @return 是否德雷德吉翁副本 / Whether dreadgion instance
	 */
	public boolean isDreadgionInstance() {
		return this.equals(InstanceType.DREADGION);
	}

	/**
	 * @return 是否 PvP 竞技场 / Whether arena pvp instance
	 */
	public boolean isArenaPvPInstance() {
		return this.equals(InstanceType.ARENA_PVP);
	}

	/**
	 * @return 是否锦标赛 / Whether tournament instance
	 */
	public boolean isTournamentInstance() {
		return this.equals(InstanceType.TOURNAMENT);
	}

	/**
	 * @return 是否团队竞技场 / Whether arena team instance
	 */
	public boolean isArenaTeamInstance() {
		return this.equals(InstanceType.ARENA_TEAM);
	}

	/**
	 * @return 是否限时挑战 / Whether time attack instance
	 */
	public boolean isTimeAttackInstance() {
		return this.equals(InstanceType.TIME_ATTACK);
	}

	/**
	 * @return 是否战场 / Whether battlefield instance
	 */
	public boolean isBattlefieldInstance() {
		return this.equals(InstanceType.BATTLEFIELD);
	}
}
