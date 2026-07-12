package com.aionemu.gameserver.model.templates.teleport;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * TeleLocID 数据模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer
 */
@XmlRootElement(name = "locations")
@XmlAccessorType(XmlAccessType.FIELD)
public class TeleLocIdData {

	@XmlElement(name = "telelocation")
	private List<TeleportLocation> locids;

	/**
	 * @return Teleport locations
	 */
	public List<TeleportLocation> getTelelocations() {
		return locids;
	}

	/** 获取传送位置。 / Returns the teleport location. */
	public TeleportLocation getTeleportLocation(int value) {
		for (TeleportLocation t : locids) {
			if (t != null && t.getLocId() == value) {
				return t;
			}
		}
		return null;
	}
}
