package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 治疗削弱效果：作为 Buff 壳，通过修饰器降低治疗量。
 * Heal deboost effect: buff shell that reduces heal amount via modifiers.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeboostHealEffect")
public class DeboostHealEffect extends BuffEffect {

}
