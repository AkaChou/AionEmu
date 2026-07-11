package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 回城冷却缩减效果：作为 Buff 壳，降低返回/回城技能冷却。
 * Return cooltime reduce effect: buff shell that reduces return/teleport cooltime.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReturnCoolReduceEffect")
public class ReturnCoolReduceEffect extends BuffEffect {

}
