package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 瞬时经验回复效果：Buff 形态的 XP 即时回复。
 * Instant XP heal effect: buff-form immediate XP recovery.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "XPHealInstantEffect")
public class XPHealInstantEffect extends BuffEffect {

}
