package com.aionemu.gameserver.model.team.legion;

import java.sql.Timestamp;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 军团 Join 请求，用于团队相关逻辑。
 * Legion Join Request for team logic.
 */

public class LegionJoinRequest {
	private int legionId = 0;
	private int playerId = 0;
	private String playerName = "";
	private int playerClass = 0;
	private int race = 0;
	private int level = 0;
	private int genderId = 0;
	private String msg = "";
	private Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	public LegionJoinRequest(int legionId, Player player, String msg) {
		this.legionId = legionId;
		this.playerId = player.getObjectId();
		this.playerName = player.getName();
		this.playerClass = player.getPlayerClass().ordinal();
		this.race = player.getRace().getRaceId();
		this.level = player.getLevel();
		this.genderId = player.getGender().getGenderId();
		this.msg = msg;
	}

	public LegionJoinRequest() {
	}

	/** 返回军团 ID / Returns the legion id */
	public int getLegionId() {
		return legionId;
	}

	/** 设置军团 ID / Sets the legion id */
	public void setLegionId(int legionId) {
		this.legionId = legionId;
	}

	/** 返回玩家 ID / Returns the player id */
	public int getPlayerId() {
		return playerId;
	}

	/** 设置 player id / Sets the player id */
	public void setPlayerId(int playerId) {
		this.playerId = playerId;
	}

	/** 获取玩家名称。 / Returns the player name. */
	public String getPlayerName() {
		return playerName;
	}

	/** 设置玩家名称。 / Sets the player name. */
	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	/** 获取玩家职业。 / Returns the player class. */
	public int getPlayerClass() {
		return playerClass;
	}

	/** 设置玩家职业。 / Sets the player class. */
	public void setPlayerClass(int playerClass) {
		this.playerClass = playerClass;
	}

	/** 获取种族。 / Returns the race. */
	public int getRace() {
		return race;
	}

	/** 设置种族。 / Sets the race. */
	public void setRace(int race) {
		this.race = race;
	}

	/** 获取等级。 / Returns the level. */
	public int getLevel() {
		return level;
	}

	/** 设置等级。 / Sets the level. */
	public void setLevel(int level) {
		this.level = level;
	}

	/** 返回 gender id / Returns the gender id */
	public int getGenderId() {
		return genderId;
	}

	/** 设置 gender id / Sets the gender id */
	public void setGenderId(int genderId) {
		this.genderId = genderId;
	}

	/** 返回消息 / Returns the msg */
	public String getMsg() {
		return msg;
	}

	/** 设置 msg / Sets the msg */
	public void setMsg(String msg) {
		this.msg = msg;
	}

	/** 返回日期 / Returns the date*/
	public Timestamp getDate() {
		return timestamp;
	}

	/** 设置 date / Sets the date */
	public void setDate(Timestamp timestamp) {
		this.timestamp = timestamp;
	}
}
