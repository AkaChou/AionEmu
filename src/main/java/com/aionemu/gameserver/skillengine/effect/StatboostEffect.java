package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 属性增强效果：通过 Buff 修饰器提升相关属性（增强型）。
 * Stat boost effect: raises stats via buff modifiers (boost variant).
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatboostEffect")
public class StatboostEffect extends BuffEffect {

}
