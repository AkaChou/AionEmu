package com.aionemu.gameserver.skillengine.model;

/**
 * 技能位移类型：击退、拉拽、后移等控制表现编码。
 * Skill move type: knockback, pull, move-behind and related control codes.
 */
public enum SkillMoveType {

	/** 抵抗 / Resist */
	RESIST(0),
	/** 默认 / Default */
	DEFAULT(16),
	/** 击飞 / Fly off */
	FLYOFF(18),
	/** 拉拽 / Pull */
	PULL(50), // OLD 18 NEW 50 (5.6)
	/** 浮空开启 / Open aerial */
	OPENAERIAL(20),
	/** 击退 / Knockback */
	KNOCKBACK(28),
	/** 移至身后 / Move behind */
	MOVEBEHIND(48),
	/** 踉跄 / Stagger */
	STAGGER(112), // 5.1
	/** 绊倒 / Stumble */
	STUMBLE(16), // 5.1
	/** 新拉拽 / New pull */
	NEWPULL(54); // 5.1

	private int id;

	private SkillMoveType(int id) {
		this.id = id;
	}

	/**
	 * 获取协议 ID。
	 * Gets protocol id.
	 *
	 */
	public int getId() {
		return id;
	}
}
