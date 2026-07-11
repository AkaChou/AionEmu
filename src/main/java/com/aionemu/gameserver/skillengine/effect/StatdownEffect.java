package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 属性降低效果：通过 Buff 修饰器降低目标属性。
 * Stat down effect: lowers target stats via buff modifiers.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatdownEffect")
public class StatdownEffect extends BuffEffect {

}
