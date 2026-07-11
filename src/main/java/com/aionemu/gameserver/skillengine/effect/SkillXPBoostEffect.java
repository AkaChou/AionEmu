package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 技能经验加成效果：作为 Buff 壳，提高技能熟练度经验获取。
 * Skill XP boost effect: buff shell that increases skill proficiency XP gain.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillXPBoostEffect")
public class SkillXPBoostEffect extends BuffEffect {

}
