package com.aionemu.gameserver.model.account;

import java.sql.Timestamp;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import lombok.Getter;
import lombok.Setter;

/**
 * 玩家账号数据，用于账号相关逻辑。
 * Player Account Data for account logic.
 *
 * @author Luno
 */
@Getter
@Setter
public class PlayerAccountData {

	private final CharacterBanInfo cbi;
	/**
	 * @return 角色公共数据 / the playerCommonData
	 */
	private PlayerCommonData playerCommonData;
	/**
	 * 返回外观。
	 * Returns the appearance
	 */
	private final PlayerAppearance appereance;
	/**
	 * @return 装备列表 / the equipment
	 */
	private List<Item> equipment;
	/** 返回 creation date / Returns the creation date */
	private Timestamp creationDate;
	/**
	 * 设置 deletiondate。
	 * Sets deletion date
	 *
	 * @param deletionDate
	 */
	private Timestamp deletionDate;
	private final LegionMember legionMember;

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
}
