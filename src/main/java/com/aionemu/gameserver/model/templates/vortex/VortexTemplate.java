package com.aionemu.gameserver.model.templates.vortex;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 漩涡模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Vortex")
public class VortexTemplate {
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "defends_race")
	protected Race dRace;

	@XmlAttribute(name = "offence_race")
	protected Race oRace;

	@XmlElement(name = "home_point")
	protected HomePoint home;

	@XmlElement(name = "resurrection_point")
	protected ResurrectionPoint resurrection;

	@XmlElement(name = "start_point")
	protected StartPoint start;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 返回 defenders race / Returns the defenders race */
	public Race getDefendersRace() {
		return this.dRace;
	}

	/** 返回 invaders race / Returns the invaders race */
	public Race getInvadersRace() {
		return this.oRace;
	}

	/** 返回 home point / Returns the home point */
	public HomePoint getHomePoint() {
		return home;
	}

	/** 返回 resurrection point / Returns the resurrection point */
	public ResurrectionPoint getResurrectionPoint() {
		return resurrection;
	}

	/** 返回开始点 / Returns the start point*/
	public StartPoint getStartPoint() {
		return start;
	}
}
