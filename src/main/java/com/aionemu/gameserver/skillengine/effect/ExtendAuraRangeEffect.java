package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 光环范围扩展效果：作为增益挂载，扩大光环类技能的作用半径。
 * Aura range extend effect: buff that expands the radius of aura skills.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExtendAuraRangeEffect")
public class ExtendAuraRangeEffect extends BuffEffect {

}
