package com.aionemu.gameserver.model.account;

import java.sql.Timestamp;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionMember;

/**
 * 玩家账号数据，用于账号相关逻辑。
 * Player Account Data for account logic.
 *
 * @author Luno
 */
public class PlayerAccountData {

	private CharacterBanInfo cbi;
	private PlayerCommonData playerCommonData;
	private PlayerAppearance appereance;
	private List<Item> equipment;
	private Timestamp creationDate;
	private Timestamp deletionDate;
	private LegionMember legionMember;

	public PlayerAccountData(PlayerCommonData playerCommonData, CharacterBanInfo cbi, PlayerAppearance appereance, List<Item> equipment, LegionMember legionMember) {
		this.playerCommonData = playerCommonData;
		this.cbi = cbi;
		this.appereance = appereance;
		this.equipment = equipment;
		this.legionMember = legionMember;
	}

	/** 返回 char ban info / Returns the char ban info */
	public CharacterBanInfo getCharBanInfo() {
		return cbi;
	}

	/** 返回 creation date / Returns the creation date */
	public Timestamp getCreationDate() {
		return creationDate;
	}

	/**
	 * 设置 deletiondate。
	 * Sets deletion date
	 *
	 * @param deletionDate
	 */
	public void setDeletionDate(Timestamp deletionDate) {
		this.deletionDate = deletionDate;
	}

	/**
	 * 获取 deletiondate。
	 * Get deletion date
	 *
	 * @return 角色应被删除的时间戳 / Timestamp date when char should be deleted
	 */
	public Timestamp getDeletionDate() {
		return deletionDate;
	}

	/**
	 * 获取角色删除时间的 Unix 秒级时间戳。
	 * Returns the character deletion time as Unix epoch seconds.
	 *
	 * @return 删除时间戳；未设置时返回 0 / deletion timestamp, or 0 when not scheduled
	 */
	public int getDeletionTimeInSeconds() {
		if (deletionDate == null) {
			return 0;
		}
		return (int) (deletionDate.getTime() / 1000L);
	}

	/**
	 * @return 角色公共数据 / the playerCommonData
	 */
	public PlayerCommonData getPlayerCommonData() {
		return playerCommonData;
	}

	/**
	 * @param playerCommonData the playerCommonData to set
	 */
	public void setPlayerCommonData(PlayerCommonData playerCommonData) {
		this.playerCommonData = playerCommonData;
	}

	/**
	 * 返回外观。
	 * Returns the appearance
	 */
	public PlayerAppearance getAppereance() {
		return appereance;
	}

	/**
	 * @param creationDate 创建时间 / creation date
	 */
	public void setCreationDate(Timestamp creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * @return 军团成员 / the legionMember
	 */
	public Legion getLegion() {
		return legionMember.getLegion();
	}

	/**
	 * 返回该角色是否为军团成员。
	 * Returns true if player is a legion member
	 *
	 * @return true 或 false / true or false
	 */
	public boolean isLegionMember() {
		return legionMember != null;
	}

	/**
	 * @return 装备列表 / the equipment
	 */
	public List<Item> getEquipment() {
		return equipment;
	}

	/**
	 * @param equipment the equipment to set
	 */
	public void setEquipment(List<Item> equipment) {
		this.equipment = equipment;
	}
}
