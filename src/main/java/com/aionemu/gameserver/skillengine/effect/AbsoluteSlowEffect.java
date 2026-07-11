package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 绝对减速效果：作为 Buff 壳，通过修饰器强制降低攻击/施法速度。
 * Absolute slow effect: buff shell that forces attack/cast-speed reduction via modifiers.
 *
 * @author Dtem
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbsoluteSlowEffect")
public class AbsoluteSlowEffect extends BuffEffect {

}
