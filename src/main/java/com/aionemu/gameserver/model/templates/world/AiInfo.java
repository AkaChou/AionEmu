package com.aionemu.gameserver.model.templates.world;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * AI 信息模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AiInfo")
public class AiInfo {

	public static final AiInfo DEFAULT = new AiInfo();

	@XmlAttribute(name = "chase_target")
	private int chaseTarget = 50;
	@XmlAttribute(name = "chase_home")
	private int chaseHome = 200;

	/** 返回 chase target / Returns the chase target */
	public final int getChaseTarget() {
		return chaseTarget;
	}

	/** 返回 chase home / Returns the chase home */
	public final int getChaseHome() {
		return chaseHome;
	}
}
