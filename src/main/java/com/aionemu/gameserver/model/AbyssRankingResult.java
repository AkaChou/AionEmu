package com.aionemu.gameserver.model;

import lombok.Getter;

/**
 * 欧比斯排行结果。
 * Abyss Ranking Result model.
 *
 * @author zdead
 */
public class AbyssRankingResult {

	@Getter
	private String playerName;
	@Getter
	private int playerAbyssRank;
	@Getter
	private int oldRankPos;
	@Getter
	private int rankPos;
	private int ap;
	private int gp;
	private int title;
	@Getter
	private PlayerClass playerClass;
	@Getter
	private int playerLevel;
	@Getter
	private int playerId;

	@Getter
	private String legionName;
	private long cp;
	@Getter
	private int legionId;
	@Getter
	private int legionLevel;
	@Getter
	private int legionMembers;

	public AbyssRankingResult(String playerName, int playerAbyssRank, int playerId, int ap, int gp, int title, PlayerClass playerClass, int playerLevel, String legionName, int oldRankPos, int rankPos) {
		this.playerName = playerName;
		this.playerAbyssRank = playerAbyssRank;
		this.playerId = playerId;
		this.ap = ap;
		this.gp = gp;
		this.title = title;
		this.playerClass = playerClass;
		this.playerLevel = playerLevel;
		this.legionName = legionName;
		this.oldRankPos = oldRankPos;
		this.rankPos = rankPos;
	}

	public AbyssRankingResult(long cp, String legionName, int legionId, int legionLevel, int legionMembers, int oldRankPos, int rankPos) {
		this.oldRankPos = oldRankPos;
		this.rankPos = rankPos;
		this.cp = cp;
		this.legionName = legionName;
		this.legionId = legionId;
		this.legionLevel = legionLevel;
		this.legionMembers = legionMembers;
	}

	/** 获取玩家欧比斯点数。 / Returns the player ap. */
	public int getPlayerAP() {
		return ap;
	}

	/** 获取玩家荣耀点数。 / Returns the player gp. */
	public int getPlayerGP() {
		return gp;
	}

	/** 获取玩家称号。 / Returns the player title. */
	public int getPlayerTitle() {
		return title;
	}

	/** 获取军团创造点。 / Returns the legion cp. */
	public long getLegionCP() {
		return cp;
	}

}
