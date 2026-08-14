package com.aionemu.gameserver.model.templates.npcshout;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shout 活动类型枚举。
 * Shout Event Type enumeration.
 */

@XmlType(name = "ShoutEventType")
@XmlEnum
public enum ShoutEventType {
	/** 空闲 / Idle. */
	IDLE, ATTACKED, ATTACK_BEGIN, ATTACK_END, ATTACK_K, SUMMON_ATTACK, CASTING, CAST_K, DIED, HELP, HELPCALL,
	/** 行走路点 / Walk Waypoint */
	WALK_WAYPOINT, START, WAKEUP, SLEEP, RESET_HATE, UNK_ACC, WALK_DIRECTION, STATUP, SWITCH_TARGET, SEE, PLAYER_MAGIC,
	/** 玩家减速 / Player Snare */
	PLAYER_SNARE, PLAYER_DEBUFF, PLAYER_SKILL, PLAYER_SLAVE, PLAYER_BLOW, PLAYER_PULL, PLAYER_PROVOKE, PLAYER_CAST,
	/** 祈求神助 / God Help */
	GOD_HELP, LEAVE, BEFORE_DESPAWN, ATTACK_DEADLY, WIN, ENEMY_DIED, ENTER_BATTLE, LEAVE_BATTLE, DEFORM_SKILL,
	/** 攻击生命值触发 / Attack Hitpoint */
	ATTACK_HITPOINT;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static ShoutEventType fromValue(String v) {
		return valueOf(v);
	}
}
