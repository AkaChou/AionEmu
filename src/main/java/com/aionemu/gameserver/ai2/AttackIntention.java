package com.aionemu.gameserver.ai2;

/**
 * 攻击意图枚举，描述 AI 在战斗中下一动作的选择。
 * Attack intention enumeration describing the AI's next combat action choice.
 *
 * @author ATracer
 */
public enum AttackIntention {

	/** 结束攻击 / Finish the attack sequence */
	FINISH_ATTACK,
	/** 切换目标 / Switch target */
	SWITCH_TARGET,
	/** 普通攻击 / Simple auto-attack */
	SIMPLE_ATTACK,
	/** 技能攻击 / Skill attack */
	SKILL_ATTACK,
	/** 技能增益 / Skill buff */
	SKILL_BUFF
}
