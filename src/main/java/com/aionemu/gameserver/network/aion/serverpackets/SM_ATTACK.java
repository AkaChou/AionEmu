package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.controllers.attack.AttackResult;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 广播普通攻击结果（伤害、格挡/招架/闪避、护盾反射等）的服务端包。
 * Server packet broadcasting basic-attack results (damage, block/parry/dodge, shield reflection, etc.).
 */
public class SM_ATTACK extends AionServerPacket {
	private int attackno;
	private int time;
	private int type;
	private int SimpleAttackType;
	private List<AttackResult> attackList;
	private Creature attacker;
	private Creature target;

	/**
	 * attacker
	 * target
	 * attack sequence number
	 * @param time 时间戳/动画时序 / timing value for animation sync
	 * @param type 攻击类型 / attack type flag
	 * @param attackList 命中结果列表 / list of hit results
	 */
	public SM_ATTACK(Creature attacker, Creature target, int attackno, int time, int type,
			List<AttackResult> attackList) {
		this.attacker = attacker;
		this.target = target;
		this.attackno = attackno;
		this.time = time;
		this.type = type;
		this.attackList = attackList;
		this.SimpleAttackType = attacker.getController().getSimpleAttackType();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(attacker.getObjectId());
		writeC(attackno); // Attack Number e.g. 1, 2, 3, 5, ..., Max_Integer_Value
		writeH(time); // unknown
		writeC((byte) SimpleAttackType);// 0=Ground Attacks | 1=Air Attacks (v4.7.5.17)
		writeC(type); // 0, 1, 2
		writeD(target.getObjectId());
		int attackerMaxHp = attacker.getLifeStats().getMaxHp();
		int attackerCurrHp = attacker.getLifeStats().getCurrentHp();
		int targetMaxHp = target.getLifeStats().getMaxHp();
		int targetCurrHp = target.getLifeStats().getCurrentHp();

		writeC((int) (100f * targetCurrHp / targetMaxHp)); // target %hp
		writeC((int) (100f * attackerCurrHp / attackerMaxHp)); // attacker %hp

		switch (attackList.get(0).getAttackStatus().getId()) { // Counter skills
		case 196: // case CRITICAL_BLOCK 4.5
		case 4: // case BLOCK
		case 5:
		case 213:
			writeH(32);
			break;
		case 194: // case CRITICAL_PARRY 4.5
		case 2: // case PARRY
		case 3:
		case 211:
			writeH(64);
			break;
		case 192: // case CRITICAL_DODGE 4.5
		case 0: // case DODGE
		case 1:
		case 209:
			writeH(128);
			break;
		case 198: // case CRITICAL_RESIST 4.5
		case 6: // case RESIST
		case 7:
		case 215:
			writeH(256); // need more info becuz sometimes 0
			break;
		default:
			writeH(0);
			break;
		}
		// 从数据包设置计数技能以获得最佳时间同步。 / setting counter skill from packet to have the best synchronization of time
		// 与客户端 / with client
		if (target instanceof Player) {
			if (attackList.get(0).getAttackStatus().isCounterSkill()) {
				((Player) target).setLastCounterSkill(attackList.get(0).getAttackStatus());
			}
		}
		writeH(0);
		writeC(attackList.size());
		for (AttackResult attack : attackList) {
			writeD(attack.getDamage());
			writeC(attack.getAttackStatus().getId());
			byte shieldType = (byte) attack.getShieldType();
			writeC(shieldType);

			// 护盾标志：1 反射，2 普通护盾，8 保护效果（如技能 417 保镖）。 / Shield flags: 1 reflector, 2 normal shield, 8 protect effect (for example skill 417 Bodyguard).
			switch (shieldType) {
			case 0:
			case 2:
				break;
			case 8:
			case 10:
				writeD(attack.getShieldMp());
				writeD(attack.getProtectorId());
				writeD(attack.getProtectedDamage());
				writeD(attack.getProtectedSkillId());
				break;
			default:
				writeD(attack.getProtectorId());
				writeD(attack.getProtectedDamage());
				writeD(attack.getProtectedSkillId());
				writeD(attack.getReflectedDamage());
				writeD(attack.getReflectedSkillId());
				writeD(0);
				writeD(0);
				break;
			}
		}
		writeC(0);
	}
}
