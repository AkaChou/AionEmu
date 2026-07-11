package com.aionemu.gameserver.model.account;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;

import com.aionemu.gameserver.configs.main.GSConfig;
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
	 * @return Timestamp date when char should be deleted.
	 */
	public Timestamp getDeletionDate() {
		return deletionDate;
	}

	/**
	 * 获取时间 secondswhen 玩家 bedeleted0 玩家 wasbedeleted。
	 * Get time in seconds when this player will be deleted ( 0 if player was not set to be deleted ). Время возвращается с учетом временной зоны сервера
	 *
	 * @return deletion time in seconds
	 */
	public int getDeletionTimeInSeconds() {
		if (deletionDate == null) {
			return 0;
		}
		
		int timezoneOffset = 0;
		try {
			if (GSConfig.TIME_ZONE_ID != null && !GSConfig.TIME_ZONE_ID.isEmpty()) {
				TimeZone tz = TimeZone.getTimeZone(GSConfig.TIME_ZONE_ID);
				timezoneOffset = tz.getRawOffset() / 1000;
			}
		} catch (Exception e) {
			timezoneOffset = TimeZone.getDefault().getRawOffset() / 1000;
		}
		
		long localTimeSeconds = (deletionDate.getTime() / 1000) + timezoneOffset;
		return (int) localTimeSeconds;
	}

	/**
	 * @return the playerCommonData
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

	/** 返回 appereance / Returns the appereance */
	public PlayerAppearance getAppereance() {
		return appereance;
	}

	/**
	 * @param creationDate
	 */
	public void setCreationDate(Timestamp creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * @return the legionMember
	 */
	public Legion getLegion() {
		return legionMember.getLegion();
	}

	/**
	 * 返回若为真则玩家为军团成员。 / Returns true if player is a legion member
	 *
	 * @return true or false
	 */
	public boolean isLegionMember() {
		return legionMember != null;
	}

	/**
	 * @return the equipment
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
