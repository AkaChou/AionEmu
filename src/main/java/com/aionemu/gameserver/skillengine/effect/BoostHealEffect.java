package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 治疗量提升效果：作为 Buff 壳，通过修饰器提高治疗输出。
 * Heal-boost effect: buff shell that increases heal output via modifiers.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BoostHealEffect")
public class BoostHealEffect extends BuffEffect {

}
