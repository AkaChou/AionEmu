package com.aionemu.gameserver.model.ai;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 炸弹集合：容纳单个炸弹模板。
 * Bomb container: holds a single bomb template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Bombs")
public class Bombs {

	@XmlElement(name = "bomb")
	private BombTemplate bombTemplate;

	/** 返回 bomb template / Returns the bomb template */
	public BombTemplate getBombTemplate() {
		return this.bombTemplate;
	}
}
