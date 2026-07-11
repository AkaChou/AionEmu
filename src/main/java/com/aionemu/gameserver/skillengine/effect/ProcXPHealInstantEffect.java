package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 触发即时经验治疗：作为 Buff 壳，用于 proc 经验回复类技能。
 * Proc instant XP heal: buff shell used by proc-based XP recovery skills.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProcXPHealInstantEffect")
public class ProcXPHealInstantEffect extends BuffEffect {

}
