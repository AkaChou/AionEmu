package com.aionemu.gameserver.model.templates.panel_cp;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 面板创造点类型枚举。
 * Panel Cp Type enumeration.
 *
 * @author Rinzler (Encom)
 */

@XmlEnum
public enum PanelCpType {
	/** 属性提升 / Stat up */
	STAT_UP, LEARN_SKILL, ENCHANT_SKILL;
}
