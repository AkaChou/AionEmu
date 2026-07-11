package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 一次性治疗加成效果：作为增益挂载，提升下一次/有限次治疗量。
 * One-time heal boost: buff that increases the next limited heal amount(s).
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OneTimeBoostHealEffect")
public class OneTimeBoostHealEffect extends BuffEffect {

}
