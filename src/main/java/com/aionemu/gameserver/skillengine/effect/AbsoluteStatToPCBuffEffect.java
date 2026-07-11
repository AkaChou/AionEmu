package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 绝对属性增益（PC Buff）：按 statset 将绝对属性套用到玩家增益侧。
 * Absolute stat-to-PC buff: applies an absolute stat set as a player buff.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbsoluteStatToPCBuff")
public class AbsoluteStatToPCBuffEffect extends AbstractAbsoluteStatEffect {
}
