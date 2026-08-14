package com.aionemu.gameserver.controllers.observer;

/**
 * 动作观察者类型（位掩码），用于过滤关心的事件。
 * Action observer type bit-mask used to filter relevant events.
 *
 * @author ATracer
 */
public enum ObserverType {
	/** 移动 / Move */
	MOVE(1),
	/** 主动攻击 / Attack */
	ATTACK(1 << 1),
	/** 受到攻击 / Attacked */
	ATTACKED(1 << 2),
	/** 装备物品 / Equip item */
	EQUIP(1 << 3),
	/** 卸下物品 / Unequip item */
	UNEQUIP(1 << 4),
	/** 使用技能 / Skill use */
	SKILLUSE(1 << 5),
	/** 死亡 / Death */
	DEATH(1 << 6),
	/** 受到持续伤害 / DoT attacked */
	DOT_ATTACKED(1 << 7),
	/** 使用物品 / Item use */
	ITEMUSE(1 << 8),
	/** NPC 对话请求 / NPC dialog request */
	NPCDIALOGREQUEST(1 << 9),
	/** 异常状态被设置 / Abnormal state set */
	ABNORMALSETTED(1 << 10),
	/** 召唤物释放 / Summon release */
	SUMMONRELEASE(1 << 11),
	/** 生命或魔法值变化 / HP or MP changed */
	LIFE_CHANGED(1 << 12),
	/** 装备或卸下 / Equip or unequip */
	EQUIP_UNEQUIP(EQUIP.observerMask | UNEQUIP.observerMask),
	/** 攻击或防御 / Attack or defend */
	ATTACK_DEFEND(ATTACK.observerMask | ATTACKED.observerMask),
	/** 移动或死亡 / Move or die */
	MOVE_OR_DIE(MOVE.observerMask | DEATH.observerMask),
	/** 全部事件 / All events */
	ALL(MOVE.observerMask | ATTACK.observerMask | ATTACKED.observerMask | SKILLUSE.observerMask | DEATH.observerMask
			| DOT_ATTACKED.observerMask | ITEMUSE.observerMask | NPCDIALOGREQUEST.observerMask
			| ABNORMALSETTED.observerMask | SUMMONRELEASE.observerMask | LIFE_CHANGED.observerMask);

	/** 观察者位掩码 / Observer bit mask */
	private int observerMask;

	/**
	 * @param observerMask 位掩码 / bit mask
	 */
	private ObserverType(int observerMask) {
		this.observerMask = observerMask;
	}

	/**
	 * 判断本类型是否覆盖指定观察者类型。
	 * Whether this type covers the given observer type.
	 *
	 * @param observerType 待匹配类型 / type to match
	 * @return 是否覆盖 / whether it matches
	 */
	public boolean matchesObserver(ObserverType observerType) {
		return (observerType.observerMask & observerMask) == observerType.observerMask;
	}
}
