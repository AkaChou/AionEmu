package com.aionemu.gameserver.skillengine.action;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 技能动作集合：JAXB 绑定的施法消耗动作列表。
 * Skill action collection: JAXB-bound list of cast-cost actions.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Actions", propOrder = { "actions" })
public class Actions {

	/**
	 * 动作列表（物品/MP/HP/DP 消耗）。
	 * Action list (item/MP/HP/DP costs).
	 */
	@XmlElements({ @XmlElement(name = "itemuse", type = ItemUseAction.class),
			@XmlElement(name = "mpuse", type = MpUseAction.class),
			@XmlElement(name = "hpuse", type = HpUseAction.class),
			@XmlElement(name = "dpuse", type = DpUseAction.class),
			@XmlElement(name = "chargeuse", type = ChargeUseAction.class) })
	protected List<Action> actions;

	/**
	 * 获取动作列表（活动列表，非快照；修改会反映到 JAXB 对象）。
	 * Returns the live action list (not a snapshot; mutations affect the JAXB object).
	 *
	 * @return 动作列表 / list of actions
	 */
	public List<Action> getActions() {
		if (actions == null) {
			actions = new ArrayList<Action>();
		}
		return this.actions;
	}
}
