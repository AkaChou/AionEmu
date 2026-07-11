package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端播放施法动画的服务端包。
 * Server packet that shows the casting-spell animation on the client.
 *
 * @author alexa026
 * @author rhys2002
 */
public class SM_CASTSPELL extends AionServerPacket {

	private final int attackerObjectId;
	private final int spellId;
	private final int level;
	private final int targetType;
	private final int duration;
	private int targetObjectId;
	private float x;
	private float y;
	private float z;
	private int skinId;

	/**
	 * 以对象为目标的施法动画包。
	 * Cast-spell animation packet targeting a creature object.
	 *
	 * @param attackerObjectId 施法者对象 ID / caster object id
	 * skill id
	 * @param level 技能等级 / skill level
	 * target type
	 * target object id
	 * @param duration 施法持续时间 / cast duration
	 * @param skinId 技能皮肤/动画 ID / skill skin animation id
	 */
	public SM_CASTSPELL(int attackerObjectId, int spellId, int level, int targetType, int targetObjectId, int duration,
			int skinId) {
		this.attackerObjectId = attackerObjectId;
		this.spellId = spellId;
		this.level = level;
		this.targetType = targetType;
		this.targetObjectId = targetObjectId;
		this.duration = duration;
		this.skinId = skinId;
	}

	/**
	 * 以坐标点为目标的施法动画包。
	 * Cast-spell animation packet targeting a world position.
	 *
	 * @param attackerObjectId 施法者对象 ID / caster object id
	 * skill id
	 * @param level 技能等级 / skill level
	 * target type
	 * @param x 目标 X / target X
	 * @param y 目标 Y / target Y
	 * @param z 目标 Z / target Z
	 * @param duration 施法持续时间 / cast duration
	 * @param skinId 技能皮肤/动画 ID / skill skin animation id
	 */
	public SM_CASTSPELL(int attackerObjectId, int spellId, int level, int targetType, float x, float y, float z,
			int duration, int skinId) {
		this(attackerObjectId, spellId, level, targetType, 0, duration, skinId);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(attackerObjectId);
		writeH(spellId);
		writeC(level);

		writeC(targetType);
		switch (targetType) {
		case 0:
		case 3:
		case 4:
		case 87:
			writeD(targetObjectId);
			break;
		case 1:
			writeF(x);
			writeF(y);
			writeF(z);
			break;
		case 2:
			writeF(x);
			writeF(y);
			writeF(z);
			writeD(0);// unk1
			writeD(0);// unk2
			writeD(0);// unk3
			writeD(0);// unk4
			writeD(0);// unk5
			writeD(0);// unk6
			writeD(0);// unk7
			writeD(0);// unk8
		}
		writeH(duration);// 未知 / unk
		writeC(0x00);// 未知 / unk
		writeF((float) 0.8);
		// SkinID Skill Animation
		writeH(skinId);
		if (duration > 0) {
			writeC(0x01);// 未知 / unk
		} else {
			writeC(0x00);
		}
	}
}
