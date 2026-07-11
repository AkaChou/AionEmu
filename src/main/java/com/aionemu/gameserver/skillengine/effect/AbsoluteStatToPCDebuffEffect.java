package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 绝对属性减益（PC Debuff）：按 statset 将绝对属性套用到玩家减益侧。
 * Absolute stat-to-PC debuff: applies an absolute stat set as a player debuff.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbsoluteStatToPCDebuff")
public class AbsoluteStatToPCDebuffEffect extends AbstractAbsoluteStatEffect {
}
