package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 属性提升效果：通过 Buff 修饰器提升目标属性。
 * Stat up effect: raises target stats via buff modifiers.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatupEffect")
public class StatupEffect extends BuffEffect {

}
