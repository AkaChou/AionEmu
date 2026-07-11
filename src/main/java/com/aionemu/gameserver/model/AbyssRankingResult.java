package com.aionemu.gameserver.model;

/**
 * 欧比斯排行结果。
 * Abyss Ranking Result model.
 *
 * @author zdead
 */
public class AbyssRankingResult {

	private String playerName;
	private int playerAbyssRank;
	private int oldRankPos;
	private int rankPos;
	private int ap;
	private int gp;
	private int title;
	private PlayerClass playerClass;
	private int playerLevel;
	private int playerId;

	private String legionName;
	private long cp;
	private int legionId;
	private int legionLevel;
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

	/** 获取玩家名称。 / Returns the player name. */
	public String getPlayerName() {
		return playerName;
	}

	/** 返回玩家 ID / Returns the player id */
	public int getPlayerId() {
		return playerId;
	}

	/** 获取玩家欧比斯军阶。 / Returns the player abyss rank. */
	public int getPlayerAbyssRank() {
		return playerAbyssRank;
	}

	/**
	 * @return the oldRankPos
	 */
	public int getOldRankPos() {
		return oldRankPos;
	}

	/** 返回排名位置 / Returns the rank pos */
	public int getRankPos() {
		return rankPos;
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

	/** 获取玩家等级。 / Returns the player level. */
	public int getPlayerLevel() {
		return playerLevel;
	}

	/** 获取玩家职业。 / Returns the player class. */
	public PlayerClass getPlayerClass() {
		return playerClass;
	}

	/** 获取军团名称。 / Returns the legion name. */
	public String getLegionName() {
		return legionName;
	}

	/** 获取军团创造点。 / Returns the legion cp. */
	public long getLegionCP() {
		return cp;
	}

	/** 返回军团 ID / Returns the legion id */
	public int getLegionId() {
		return legionId;
	}

	/** 获取军团等级。 / Returns the legion level. */
	public int getLegionLevel() {
		return legionLevel;
	}

	/** 返回军团成员数 / Returns the legion members */
	public int getLegionMembers() {
		return legionMembers;
	}
}
