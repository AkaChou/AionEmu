package com.aionemu.gameserver.model.team.legion;

import java.sql.Timestamp;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 军团加入申请。
 * Legion join request.
 */
@Getter
@Setter
public class LegionJoinRequest {
	private int legionId = 0;
	private int playerId = 0;
	private String playerName = "";
	private int playerClass = 0;
	private int race = 0;
	private int level = 0;
	private int genderId = 0;
	private String msg = "";
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
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

	/** 获取申请时间。 / Returns the request date. */
	public Timestamp getDate() {
		return timestamp;
	}

	/** 设置申请时间。 / Sets the request date. */
	public void setDate(Timestamp timestamp) {
		this.timestamp = timestamp;
	}
}
