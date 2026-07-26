package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TeleportAnimation;

/**
 * 传送门路径模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalPath")
public class PortalPath {

	@XmlElement(name = "portal_req")
	protected PortalReq portalReq;
	@XmlAttribute(name = "dialog")
	protected int dialog;
	@XmlAttribute(name = "loc_id")
	protected int locId;
	@XmlAttribute(name = "player_count")
	protected int playerCount;
	@XmlAttribute(name = "instance")
	protected boolean instance;
	@XmlAttribute(name = "siege_id")
	protected int siegeId;
	@XmlAttribute(name = "race")
	protected Race race = Race.PC_ALL;
	@XmlAttribute(name = "err_group")
	protected int errGroup;
	@XmlAttribute(name = "source_world_id")
	protected int sourceWorldId;
	@XmlAttribute(name = "animation")
	protected TeleportAnimation animation = TeleportAnimation.FIRE_ANIMATION;
	@XmlAttribute(name = "destination_alias")
	protected String destinationAlias = "";

	/** 返回 portal req / Returns the portal req */
	public PortalReq getPortalReq() {
		return portalReq;
	}

	/** 获取对话。 / Returns the dialog. */
	public int getDialog() {
		return dialog;
	}

	/** 设置对话。 / Sets the dialog. */
	public void setDialog(int value) {
		this.dialog = value;
	}

	/** 返回 loc id / Returns the loc id */
	public int getLocId() {
		return locId;
	}

	/** 设置 loc id / Sets the loc id */
	public void setLocId(int value) {
		this.locId = value;
	}

	/** 获取玩家计数。 / Returns the player count. */
	public int getPlayerCount() {
		return playerCount;
	}

	/** 设置玩家计数。 / Sets the player count. */
	public void setPlayerCount(int value) {
		this.playerCount = value;
	}

	/** 是否副本。 / Whether Instance. */
	public boolean isInstance() {
		return instance;
	}

	/** 设置副本。 / Sets the instance. */
	public void setInstance(boolean value) {
		this.instance = value;
	}

	/** 返回攻城 ID / Returns the siege id */
	public int getSiegeId() {
		return siegeId;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 返回 err group / Returns the err group */
	public int getErrGroup() {
		return errGroup;
	}

	public boolean matches(Race race, int sourceWorldId) {
		return (this.sourceWorldId == 0 || this.sourceWorldId == sourceWorldId)
				&& (this.race == Race.PC_ALL || this.race == race);
	}

	public TeleportAnimation getAnimation() {
		return animation;
	}

	public String getDestinationAlias() {
		return destinationAlias;
	}

}
