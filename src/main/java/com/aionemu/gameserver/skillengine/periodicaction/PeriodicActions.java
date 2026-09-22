package com.aionemu.gameserver.skillengine.periodicaction;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 周期动作集合：JAXB 绑定的效果周期消耗动作与检查间隔。
 * Periodic action collection: JAXB-bound effect tick actions and check interval.
 * @author MATTY (ADev.Team)
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PeriodicActions", propOrder = "periodicActions")
public class PeriodicActions {

	/**
	 * 周期动作列表（HP/MP/DP 消耗）。
	 * Periodic action list (HP/MP/DP costs).
	 */
	@XmlElements({ @XmlElement(name = "hpuse", type = HpUsePeriodicAction.class),
			@XmlElement(name = "mpuse", type = MpUsePeriodicAction.class),
			@XmlElement(name = "dpuse", type = DpUsePeriodicAction.class) })
    protected List<PeriodicAction> periodicActions;

	/**
	 * 检查间隔（毫秒）。
	 * Check interval in milliseconds.
	 */
	@XmlAttribute(name = "checktime")
	protected int checktime;
}
