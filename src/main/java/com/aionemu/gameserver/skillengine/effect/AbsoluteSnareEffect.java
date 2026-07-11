package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 绝对定身效果：作为 Buff 壳，通过修饰器强制限制移动速度。
 * Absolute snare effect: buff shell that forces movement-speed restriction via modifiers.
 *
 * @author Dtem
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbsoluteSnareEffect")
public class AbsoluteSnareEffect extends BuffEffect {

}
