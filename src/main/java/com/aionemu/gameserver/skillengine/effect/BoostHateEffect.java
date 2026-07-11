package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 仇恨提升效果：通过 Buff 属性修正提高仇恨生成。
 * Boost-hate effect: increases hate generation via buff stat modifiers.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BoostHateEffect")
public class BoostHateEffect extends BuffEffect {

}
