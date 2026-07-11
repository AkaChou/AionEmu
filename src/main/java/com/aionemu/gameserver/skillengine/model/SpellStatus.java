package com.aionemu.gameserver.skillengine.model;

/**
 * 法术状态：绊倒、击退、浮空、格挡、招架、闪避、抵抗等。
 * Spell status: stumble, knockback, aerial, block, parry, dodge, resist, etc.
 * <p>
 * 协议位：1 stumble, 2 stagger, 4 open aerial, 8 close aerial, 16 spin,
 * 32 block, 64 parry, 128 dodge, 256 resist.
 * Protocol bits: 1 stumble, 2 stagger, 4 open aerial, 8 close aerial, 16 spin,
 * 32 block, 64 parry, 128 dodge, 256 resist.
 *
 * @author ATracer
 */
public enum SpellStatus {

	/** 无 / None */
	NONE(0),
	/** 绊倒 / Stumble */
	STUMBLE(1),
	/** 踉跄 / Stagger */
	STAGGER(2),
	/** 开启浮空 / Open aerial */
	OPENAERIAL(4),
	/** 关闭浮空 / Close aerial */
	CLOSEAERIAL(8),
	/** 旋转 / Spin */
	SPIN(16),
	/** 格挡 / Block */
	BLOCK(32),
	/** 招架 / Parry */
	PARRY(64),
	/** 闪避 / Dodge */
	DODGE(128),
	/** 抵抗 / Resist */
	RESIST(256);

	private int id;

	private SpellStatus(int id) {
		this.id = id;
	}

	/**
	 * 获取协议 ID。
	 * Gets protocol id.
	 *
	 * spell status id
	 */
	public int getId() {
		return id;
	}
}
