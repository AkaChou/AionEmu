package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 法术攻击提升效果：作为 Buff 壳，通过修饰器提高法术攻击。
 * Spell-attack boost effect: buff shell that increases spell attack via modifiers.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BoostSpellAttackEffect")
public class BoostSpellAttackEffect extends BuffEffect {

}
