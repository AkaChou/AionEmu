package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.templates.stats.SummonStatsTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_UPDATE;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster.BroadcastMode;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 召唤物游戏属性，用于属性相关逻辑。
 * Summon Game Stats for stats logic.
 *
 * @author ATracer
 */
public class SummonGameStats extends CreatureGameStats<Summon> {

	private int cachedSpeed;
	private final SummonStatsTemplate statsTemplate;

	/**
	 * @param owner
	 * @param statsTemplate
	 */
	public SummonGameStats(Summon owner, SummonStatsTemplate statsTemplate) {
		super(owner);
		this.statsTemplate = statsTemplate;
	}

	@Override
	protected void onStatsChange() {
		updateStatsAndSpeedVisually();
	}

	/** 更新 stats and speed visually / Update stats and speed visually */
	public void updateStatsAndSpeedVisually() {
		updateStatsVisually();
		checkSpeedStats();
	}

	/** 更新 stats visually / Update stats visually */
	public void updateStatsVisually() {
		owner.addPacketBroadcastMask(BroadcastMode.UPDATE_STATS);
	}

	private void checkSpeedStats() {
		int current = getMovementSpeed().getCurrent();
		if (current != cachedSpeed) {
			owner.addPacketBroadcastMask(BroadcastMode.UPDATE_SPEED);
		}
		cachedSpeed = current;
	}

	/** 返回 all speed / Returns the all speed */
	@Override
	public Stat2 getAllSpeed() {
		return getStat(StatEnum.ALLSPEED, 7500);
	}

	/** 获取属性。 / Returns the stat. */
	@Override
	public Stat2 getStat(StatEnum statEnum, int base) {
		Stat2 stat = super.getStat(statEnum, base);
		if (owner.getMaster() == null)
			return stat;
		switch (statEnum) {
		case MAXHP:
			stat.setBonusRate(0.5f);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);
		case BOOST_MAGICAL_SKILL:
		case MAGIC_SKILL_BOOST_RESIST:
			stat.setBonusRate(0.8f);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);
		case PHYSICAL_ATTACK:
			stat.setBonusRate(0.3f);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);
		case PHYSICAL_DEFENSE:
		case EVASION:
		case MAGICAL_ACCURACY:
		case MAGICAL_RESIST:
			stat.setBonusRate(0.5f);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);
		case PHYSICAL_ACCURACY:
			stat.setBonusRate(0.5f);
			owner.getMaster().getGameStats().getItemStatBoost(StatEnum.MAIN_HAND_ACCURACY, stat);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);
		case PHYSICAL_CRITICAL:
			stat.setBonusRate(0.5f);
			owner.getMaster().getGameStats().getItemStatBoost(StatEnum.MAIN_HAND_CRITICAL, stat);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);

		}
		return stat;
	}

	/** 返回最大生命 / Returns the max hp*/
	@Override
	public Stat2 getMaxHp() {
		return getStat(StatEnum.MAXHP, statsTemplate.getMaxHp());
	}

	/** 返回最大魔法 / Returns the max mp*/
	@Override
	public Stat2 getMaxMp() {
		return getStat(StatEnum.MAXHP, statsTemplate.getMaxMp());
	}

	/** 返回 strike resist / Returns the strike resist */
	@Override
	public Stat2 getStrikeResist() {
		return getStat(StatEnum.PHYSICAL_CRITICAL_RESIST, 0);
	}

	/** 返回 strike fort / Returns the strike fort */
	@Override
	public Stat2 getStrikeFort() {
		return getStat(StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE, 0);
	}

	/** 返回 spell resist / Returns the spell resist */
	@Override
	public Stat2 getSpellResist() {
		return getStat(StatEnum.MAGICAL_CRITICAL_RESIST, 0);
	}

	/** 返回 spell fort / Returns the spell fort */
	@Override
	public Stat2 getSpellFort() {
		return getStat(StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE, 0);
	}

	/** 返回 b casting time / Returns the b casting time */
	@Override
	public Stat2 getBCastingTime() {
		return getStat(StatEnum.BOOST_CASTING_TIME, 1000);
	}

	/** 返回 concentration / Returns the concentration */
	@Override
	public Stat2 getConcentration() {
		return getStat(StatEnum.CONCENTRATION, 0);
	}

	/** 返回 root resistance / Returns the root resistance */
	@Override
	public Stat2 getRootResistance() {
		return getStat(StatEnum.ROOT_RESISTANCE, 0);
	}

	/** 返回 snare resistance / Returns the snare resistance */
	@Override
	public Stat2 getSnareResistance() {
		return getStat(StatEnum.SNARE_RESISTANCE, 0);
	}

	/** 返回 bind resistance / Returns the bind resistance */
	@Override
	public Stat2 getBindResistance() {
		return getStat(StatEnum.BIND_RESISTANCE, 0);
	}

	/** 返回 fear resistance / Returns the fear resistance */
	@Override
	public Stat2 getFearResistance() {
		return getStat(StatEnum.FEAR_RESISTANCE, 0);
	}

	/** 返回 sleep resistance / Returns the sleep resistance */
	@Override
	public Stat2 getSleepResistance() {
		return getStat(StatEnum.SLEEP_RESISTANCE, 0);
	}

	/** 返回 attack speed / Returns the attack speed */
	@Override
	public Stat2 getAttackSpeed() {
		return getStat(StatEnum.ATTACK_SPEED, owner.getObjectTemplate().getAttackDelay());
	}

	/** 返回 movement speed / Returns the movement speed */
	@Override
	public Stat2 getMovementSpeed() {
		int bonusSpeed = 0;
		Player master = owner.getMaster();
		if (master != null && (master.isInFlyingState() || master.isInState(CreatureState.GLIDING))) {
			bonusSpeed += 3000;
		}
		return getStat(StatEnum.SPEED, Math.round(statsTemplate.getRunSpeed() * 1000) + bonusSpeed);
	}

	/** 返回攻击范围 / Returns the attack range*/
	@Override
	public Stat2 getAttackRange() {
		return getStat(StatEnum.ATTACK_RANGE, owner.getObjectTemplate().getAttackRange() * 1500);
	}

	/** 返回 p def / Returns the p def */
	@Override
	public Stat2 getPDef() {
		return getStat(StatEnum.PHYSICAL_DEFENSE, statsTemplate.getPdefense());
	}

	/** 返回 m def / Returns the m def */
	@Override
	public Stat2 getMDef() {
		return getStat(StatEnum.MAGICAL_DEFEND, 0);
	}

	/** 返回 m resist / Returns the m resist */
	@Override
	public Stat2 getMResist() {
		return getStat(StatEnum.MAGICAL_RESIST, statsTemplate.getMresist());
	}

	/** 返回 mb resist / Returns the mb resist */
	@Override
	public Stat2 getMBResist() {
		int base = 0;
		return getStat(StatEnum.MAGIC_SKILL_BOOST_RESIST, base);
	}

	/** 返回 power / Returns the power */
	@Override
	public Stat2 getPower() {
		return getStat(StatEnum.POWER, 100);
	}

	/** 返回 health / Returns the health */
	@Override
	public Stat2 getHealth() {
		return getStat(StatEnum.HEALTH, 100);
	}

	/** 返回 accuracy / Returns the accuracy */
	@Override
	public Stat2 getAccuracy() {
		return getStat(StatEnum.PHYSICAL_ACCURACY, 100);
	}

	/** 返回 agility / Returns the agility */
	@Override
	public Stat2 getAgility() {
		return getStat(StatEnum.AGILITY, 100);
	}

	/** 返回 knowledge / Returns the knowledge */
	@Override
	public Stat2 getKnowledge() {
		return getStat(StatEnum.KNOWLEDGE, 100);
	}

	/** 返回 will / Returns the will */
	@Override
	public Stat2 getWill() {
		return getStat(StatEnum.WILL, 100);
	}

	/** 返回 evasion / Returns the evasion */
	@Override
	public Stat2 getEvasion() {
		return getStat(StatEnum.EVASION, statsTemplate.getEvasion());
	}

	/** 返回 parry / Returns the parry */
	@Override
	public Stat2 getParry() {
		return getStat(StatEnum.PARRY, statsTemplate.getParry());
	}

	/** 返回黑名单 / Returns the block */
	@Override
	public Stat2 getBlock() {
		return getStat(StatEnum.BLOCK, statsTemplate.getBlock());
	}

	/** 返回 main hand p attack / Returns the main hand p attack */
	@Override
	public Stat2 getMainHandPAttack() {
		return getStat(StatEnum.PHYSICAL_ATTACK, statsTemplate.getMainHandAttack());
	}

	/** 返回 main hand p critical / Returns the main hand p critical */
	@Override
	public Stat2 getMainHandPCritical() {
		return getStat(StatEnum.PHYSICAL_CRITICAL, statsTemplate.getMainHandCritRate());
	}

	/** 返回 main hand p accuracy / Returns the main hand p accuracy */
	@Override
	public Stat2 getMainHandPAccuracy() {
		return getStat(StatEnum.PHYSICAL_ACCURACY, statsTemplate.getMainHandAccuracy());
	}

	/** 返回 m attack / Returns the m attack */
	@Override
	public Stat2 getMAttack() {
		return getStat(StatEnum.MAGICAL_ATTACK, 100);
	}

	/** 返回 main hand m attack / Returns the main hand m attack */
	@Override
	public Stat2 getMainHandMAttack() {
		return getStat(StatEnum.MAGICAL_ATTACK, 100);
	}

	/** 返回 off hand m attack / Returns the off hand m attack */
	@Override
	public Stat2 getOffHandMAttack() {
		return getStat(StatEnum.MAGICAL_ATTACK, 0);
	}

	/** 返回 m boost / Returns the m boost */
	@Override
	public Stat2 getMBoost() {
		return getStat(StatEnum.BOOST_MAGICAL_SKILL, 0);
	}

	/** 返回 m accuracy / Returns the m accuracy */
	@Override
	public Stat2 getMAccuracy() {
		return getStat(StatEnum.MAGICAL_ACCURACY, statsTemplate.getMagicAccuracy());
	}

	/** 返回 m critical / Returns the m critical */
	@Override
	public Stat2 getMCritical() {
		return getStat(StatEnum.MAGICAL_CRITICAL, statsTemplate.getMcrit());
	}

	/** 返回 hp regen rate / Returns the hp regen rate */
	@Override
	public Stat2 getHpRegenRate() {
		int base = (int) (owner.getLifeStats().getMaxHp() * owner.getMode().getId() == 2 ? 0.05f : 0.025f);
		return getStat(StatEnum.REGEN_HP, base);
	}

	/** 返回 mp regen rate / Returns the mp regen rate */
	@Override
	public Stat2 getMpRegenRate() {
		throw new IllegalStateException("No mp regen for Summon");
	}

	/** 更新属性信息。 / Update stat info. */
	@Override
	public void updateStatInfo() {
		Player master = owner.getMaster();
		if (master != null) {
			PacketSendUtility.sendPacket(master, new SM_SUMMON_UPDATE(owner));
		}
	}

	/** 更新 speed info / Update speed info */
	@Override
	public void updateSpeedInfo() {
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.START_EMOTE2, 0, 0));
	}
}
