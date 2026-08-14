package com.aionemu.gameserver.model.team.legion;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 军团 MemberEx，用于团队相关逻辑。
 * Legion Member Ex for team logic.
 *
 * @author Simple
 */
@Slf4j
public class LegionMemberEx extends LegionMember {


	private String name;
	private PlayerClass playerClass;
	private int level;
	private Timestamp lastOnline;
	private int worldId;
	private boolean online = false;

	/**
	 * 立即使用玩家对象数据构造。
	 * Constructs immediately from the player object.
	 */
	public LegionMemberEx(Player player, LegionMember legionMember, boolean online) {
		super(player.getObjectId(), legionMember.getLegion(), legionMember.getRank());
		this.nickname = legionMember.getNickname();
		this.selfIntro = legionMember.getSelfIntro();
		this.name = player.getName();
		this.playerClass = player.getPlayerClass();
		this.level = player.getLevel();
		this.lastOnline = player.getCommonData().getLastOnline();
		this.worldId = player.getPosition().getMapId();
		this.online = online;
	}

	/**
	 * 若稍后定义玩家则调用此构造。 / If player is defined later on this constructor is called
	 */
	public LegionMemberEx(int playerObjId) {
		super(playerObjId);
	}

	/**
	 * 若稍后定义玩家则调用此构造。 / If player is defined later on this constructor is called
	 */
	public LegionMemberEx(String name) {
		super();
		this.name = name;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 设置名称。 / Sets the name. */
	public void setName(String name) {
		this.name = name;
	}

	/** 获取玩家职业。 / Returns the player class. */
	public PlayerClass getPlayerClass() {
		return playerClass;
	}

	/** 设置玩家职业。 / Sets the player class. */
	public void setPlayerClass(PlayerClass playerClass) {
		this.playerClass = playerClass;
	}

	/** 返回最后在线时间。 / Returns the last online time. */
	public int getLastOnline() {
		if (lastOnline == null || isOnline())
			return 0;
		return (int) (lastOnline.getTime() / 1000);
	}

	/** 设置最后在线时间。 / Sets the last online time. */
	public void setLastOnline(Timestamp timestamp) {
		lastOnline = timestamp;
	}

	/** 获取等级。 / Returns the level. */
	public int getLevel() {
		return level;
	}

	/**
	 * 按经验值计算并设置等级。
	 * Sets the level based on the exp value.
	 *
	 * @param exp 经验值 / exp value
	 */
	public void setExp(long exp) {
		// maxLevel 为 51，但游戏中 50 级应显示满经验条 / maxLevel is 51 but in game 50 should be shown with full XP bar
		int maxLevel = DataManager.PLAYER_EXPERIENCE_TABLE.getMaxLevel();

		if (getPlayerClass() != null && getPlayerClass().isStartingClass())
			maxLevel = 10;

		long maxExp = DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(maxLevel);
		int level = 1;

		if (exp > maxExp) {
			exp = maxExp;
		}

		// 确保等级永不大于 maxLevel-1 / make sure level is never larger than maxLevel-1
		while ((level + 1) != maxLevel && exp >= DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(level + 1)) {
			level++;
		}

		this.level = level;
	}

	/** 返回世界 ID。 / Returns the world id. */
	public int getWorldId() {
		return worldId;
	}

	/** 设置世界 ID。 / Sets the world id. */
	public void setWorldId(int worldId) {
		this.worldId = worldId;
	}

	/**
	 * @param online the online to set
	 */
	public void setOnline(boolean online) {
		this.online = online;
	}

	/**
	 * @return the online
	 */
	public boolean isOnline() {
		return online;
	}

	/** 是否为相同对象 ID。 / Whether the object ids match. */
	public boolean sameObjectId(int objectId) {
		return getObjectId() == objectId;
	}

	/**
	 * 检查 LegionMemberEx 是否有效。
	 * Checks whether this LegionMemberEx is valid.
	 *
	 * @return 若有效则为 true / true if valid
	 */
	public boolean isValidLegionMemberEx() {
		if (getObjectId() < 1) {
			log.error(I18n.get("log.61b5cdc2a429"));
		} else if (getName() == null) {
			log.error(I18n.get("log.1d1c27f5e58f", getObjectId()));
		} else if (getPlayerClass() == null) {
			log.error(I18n.get("log.0a53d0645d0e", getObjectId()));
		} else if (getLevel() < 1) {
			log.error(I18n.get("log.645e827cf120", getObjectId()));
		} else if (getLastOnline() == 0) {
			log.error(I18n.get("log.ab2c10f95931", getObjectId()));
		} else if (getWorldId() < 1) {
			log.error(I18n.get("log.8d41955bf31f", getObjectId()));
		} else if (getLegion() == null) {
			log.error(I18n.get("log.37f2ca6d52c9", getObjectId()));
		} else if (getRank() == null) {
			log.error(I18n.get("log.420d947ebea9", getObjectId()));
		} else if (getNickname() == null) {
			log.error(I18n.get("log.3127f18a4594", getObjectId()));
		} else if (getSelfIntro() == null) {
			log.error(I18n.get("log.57219d9a4038", getObjectId()));
		} else {
			return true;
		}
		return false;
	}
}
