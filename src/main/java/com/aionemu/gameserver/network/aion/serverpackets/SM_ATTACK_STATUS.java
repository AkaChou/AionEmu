package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步生物生命值相关状态变化（HP/MP/FP 增减、伤害、治疗、吸收等）的服务端包。
 * Server packet synchronizing creature vital-stat changes (HP/MP/FP gain/loss, damage, heal, absorb, etc.).
 *
 * @author alexa026
 * @author ATracer
 * @author kecimis
 */
public class SM_ATTACK_STATUS extends AionServerPacket {

	private Creature creature;
	private Creature attacker;
	private TYPE type;
	private int skillId;
	private int value;
	private int logId;

	/**
	 * 状态变更类型（对应客户端显示通道）。
	 * Status-change type (maps to the client display channel).
	 */
	public static enum TYPE {

		NATURAL_HP(3),
		USED_HP(4), // when skill uses hp as cost parameter
		REGULAR(5),
		ABSORBED_HP(6),
		HP(7),
		DAMAGE(7),
		PROTECTDMG(8),
		DELAYDAMAGE(10),
		DROWNING(12),
		FALL_DAMAGE(17),
		HEAL_MP(19),
		ABSORBED_MP(20),
		MP(21),
		NATURAL_MP(22),
		ATTACK(23),
		FP_RINGS(24),
		FP(25),
		NATURAL_FP(26),
		AUTO_HEAL_FP(27);

		private int value;

		private TYPE(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}

	/**
	 * 战斗日志显示类型（决定客户端飘字/日志文案）。
	 * log wording on the client). / log wording on the client).
	 */
	public static enum LOG {

		SPELLATK(1),
		HEAL(3),
		MPHEAL(4),
		SKILLLATKDRAININSTANT(23),
		SPELLATKDRAININSTANT(24),
		POISON(25),
		BLEED(26),
		PROCATKINSTANT(93), // Old 92 New 93
		DELAYEDSPELLATKINSTANT(97), // Old 95 New 97
		SPELLATKDRAIN(130),
		FPHEAL(133),
		REGULARHEAL(170),
		REGULAR(189),
		ATTACK(197); // Old 195 (5.4) 196 (5.6) 197 (5.8)

		private int value;

		private LOG(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}

	/**
	 * @param creature 状态变化的目标生物 / creature whose vitals changed
	 * @param attacker 来源攻击者（可为 null 语义下的自身） / source attacker
	 * @param type 状态类型 / status type
	 * @param skillId 关联技能 ID，无则为 0 / related skill id, or 0
	 * @param value 变化数值 / delta value
	 * @param log 战斗日志类型 / combat-log type
	 */
	public SM_ATTACK_STATUS(Creature creature, Creature attacker, TYPE type, int skillId, int value, LOG log) {
		this.creature = creature;
		this.attacker = attacker;
		this.type = type;
		this.skillId = skillId;
		this.value = value;
		this.logId = log.getValue();
	}

	/**
	 * 使用 {@link LOG#REGULAR} 的便捷构造。
	 * Convenience constructor using {@link LOG#REGULAR}.
	 */
	public SM_ATTACK_STATUS(Creature creature, Creature attacker, TYPE type, int skillId, int value) {
		this(creature, attacker, type, skillId, value, LOG.REGULAR);
	}

	/**
	 * 普通数值变化的便捷构造（TYPE.REGULAR / LOG.REGULAR）。
	 * Convenience constructor for a plain regular delta.
	 */
	public SM_ATTACK_STATUS(Creature creature, Creature attacker, int value) {
		this(creature, attacker, TYPE.REGULAR, 0, value, LOG.REGULAR);
	}

	/**
	 * {@inheritDoc} ddchcc
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		if (type.getValue() == 5 || type.getValue() == 7 || type.getValue() == 10) {
			writeD(creature.getObjectId());
			writeD(con.getActivePlayer().getObjectId());
		}
		else {
			writeD(con.getActivePlayer().getObjectId());
			writeD(0x00);
		}
		switch (type) {
		case ATTACK:
		case DAMAGE:
		case DELAYDAMAGE:
		case DROWNING:
			writeD(-value);
				break;
			default:
				writeD(value);
		}
		writeC(type.getValue());
		writeC(creature.getLifeStats().getHpPercentage());
		writeH(skillId);
		if (attacker instanceof Player) {
			Player player = (Player) attacker;
			if (player != null) {
				writeH(player.getSkillSkinList().getSkinId(skillId));
			} else {
				writeH(0);
			}
		} else {
			writeH(0); // 5.3
		} if (skillId != 0) {
			writeH(logId);
		} else {
			writeH(LOG.ATTACK.getValue());
		}
	}
}
