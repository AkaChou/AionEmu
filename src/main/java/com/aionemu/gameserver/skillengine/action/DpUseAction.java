package com.aionemu.gameserver.skillengine.action;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * DP 消耗动作：施法时从玩家扣除指定 DP（仅玩家施法者）。
 * DP cost action: deducts DP from the player caster on cast.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DpUseAction")
public class DpUseAction extends Action {

	/**
	 * 扣除的 DP 值。
	 * DP amount to consume.
	 */
	@XmlAttribute(required = true)
	protected int value;

	/**
	 * 扣除施法玩家的 DP；不足时直接返回。
	 * Deducts DP from the casting player; no-op if insufficient.
	 *
	 * @param skill 当前技能上下文 / current skill context
	 */
	@Override
	public void act(Skill skill) {
		if (!SkillConfig.CONSUME_DP) {
			return;
		}
		Player effector = (Player) skill.getEffector();
		int currentDp = effector.getCommonData().getDp();

		if (currentDp <= 0 || currentDp < value) {
			return;
		}
		effector.getCommonData().setDp(currentDp - value);
	}
}
