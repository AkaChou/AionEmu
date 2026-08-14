package com.aionemu.gameserver.model.stats.container;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.SummonedObject;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.templates.npc.NpcRating;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster.BroadcastMode;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * NPC 的游戏属性：基础属性计算与缓存。
 * NPC game stats: base stat calculation and caching.
 *
 * @author xavier
 */
public class NpcGameStats extends CreatureGameStats<Npc> {

	int currentRunSpeed = 0;
	private long lastAttackTime = 0;
	private long lastAttackedTime = 0;
	private long nextAttackTime = 0;
	private long lastSkillTime = 0;
	private long nextSkillTime = 0;
	private long fightStartingTime = 0;
	private long lastSpawnPointChaseCheck;
	private int cachedState;
	private Stat2 cachedSpeedStat;
	private long lastGeoZUpdate;
	private long lastChangeTarget = 0;
	private int pAccuracy = 0;
	private int mRes = 0;

	public NpcGameStats(Npc owner) {
		super(owner);
	}

	@Override
	protected void onStatsChange() {
		super.onStatsChange();
		checkSpeedStats();
	}

	private void checkSpeedStats() {
		Stat2 oldSpeed = cachedSpeedStat;
		cachedSpeedStat = null;
		Stat2 newSpeed = getMovementSpeed();
		cachedSpeedStat = newSpeed;
		if (oldSpeed == null || oldSpeed.getCurrent() != newSpeed.getCurrent()) {
			owner.addPacketBroadcastMask(BroadcastMode.UPDATE_SPEED);
		}
	}

	/** 返回最大生命 / Returns the max hp*/
	@Override
	public Stat2 getMaxHp() {
		return getStat(StatEnum.MAXHP, owner.getObjectTemplate().getStatsTemplate().getMaxHp());
	}

	/** 返回最大魔法 / Returns the max mp*/
	@Override
	public Stat2 getMaxMp() {
		return getStat(StatEnum.MAXMP, owner.getObjectTemplate().getStatsTemplate().getMaxMp());
	}

	/** 返回 attack speed / Returns the attack speed */
	@Override
	public Stat2 getAttackSpeed() {
		return getStat(StatEnum.ATTACK_SPEED, owner.getObjectTemplate().getAttackDelay());
	}

	/** 返回 strike resist / Returns the strike resist */
	public Stat2 getStrikeResist() {
		return getStat(StatEnum.PHYSICAL_CRITICAL_RESIST, 0);
	}

	/** 返回 strike fort / Returns the strike fort */
	public Stat2 getStrikeFort() {
		return getStat(StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE, 0);
	}

	/** 返回 spell resist / Returns the spell resist */
	public Stat2 getSpellResist() {
		return getStat(StatEnum.MAGICAL_CRITICAL_RESIST, 0);
	}

	/** 返回 spell fort / Returns the spell fort */
	public Stat2 getSpellFort() {
		return getStat(StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE, 0);
	}

	/** 返回 b casting time / Returns the b casting time */
	public Stat2 getBCastingTime() {
		return getStat(StatEnum.BOOST_CASTING_TIME, 1000);
	}

	/** 返回 concentration / Returns the concentration */
	public Stat2 getConcentration() {
		return getStat(StatEnum.CONCENTRATION, 0);
	}

	/** 返回 root resistance / Returns the root resistance */
	public Stat2 getRootResistance() {
		return getStat(StatEnum.ROOT_RESISTANCE, 0);
	}

	/** 返回 snare resistance / Returns the snare resistance */
	public Stat2 getSnareResistance() {
		return getStat(StatEnum.SNARE_RESISTANCE, 0);
	}

	/** 返回 bind resistance / Returns the bind resistance */
	public Stat2 getBindResistance() {
		return getStat(StatEnum.BIND_RESISTANCE, 0);
	}

	/** 返回 fear resistance / Returns the fear resistance */
	public Stat2 getFearResistance() {
		return getStat(StatEnum.FEAR_RESISTANCE, 0);
	}

	/** 返回 sleep resistance / Returns the sleep resistance */
	public Stat2 getSleepResistance() {
		return getStat(StatEnum.SLEEP_RESISTANCE, 0);
	}

	/** 返回 all speed / Returns the all speed */
	public Stat2 getAllSpeed() {
		return getStat(StatEnum.ALLSPEED, 7500);
	}

	/** 返回 movement speed / Returns the movement speed */
	@Override
	public Stat2 getMovementSpeed() {
		int currentState = owner.getState();
		Stat2 cachedSpeed = cachedSpeedStat;
		if (cachedSpeed != null && cachedState == currentState) {
			return cachedSpeed;
		}
		Stat2 newSpeedStat = null;
		if (owner.isFlying()) {
			newSpeedStat = getStat(StatEnum.FLY_SPEED,
					Math.round(owner.getObjectTemplate().getStatsTemplate().getRunSpeed() * 1.3f * 1000));
		} else if (owner.isInState(CreatureState.WEAPON_EQUIPPED)) {
			newSpeedStat = getStat(StatEnum.SPEED,
					Math.round(owner.getObjectTemplate().getStatsTemplate().getRunSpeedFight() * 1000));
		} else if (owner.isInState(CreatureState.WALKING)) {
			newSpeedStat = getStat(StatEnum.SPEED,
					Math.round(owner.getObjectTemplate().getStatsTemplate().getWalkSpeed() * 1000));
		} else {
			newSpeedStat = getStat(StatEnum.SPEED,
					Math.round(owner.getObjectTemplate().getStatsTemplate().getRunSpeed() * 1000));
		}
		cachedState = currentState;
		cachedSpeedStat = newSpeedStat;
		return newSpeedStat;
	}

	/** 返回攻击范围 / Returns the attack range*/
	@Override
	public Stat2 getAttackRange() {
		return getStat(StatEnum.ATTACK_RANGE, owner.getObjectTemplate().getAttackRange() * 1000);
	}

	/** 返回 p def / Returns the p def */
	@Override
	public Stat2 getPDef() {
		return getStat(StatEnum.PHYSICAL_DEFENSE, owner.getObjectTemplate().getStatsTemplate().getPdef());
	}

	/** 返回 m def / Returns the m def */
	@Override
	public Stat2 getMDef() {
		return getStat(StatEnum.MAGICAL_DEFEND, owner.getObjectTemplate().getStatsTemplate().getMdef());
	}

	/** 返回 m resist / Returns the m resist */
	@Override
	public Stat2 getMResist() {
		return getStat(StatEnum.MAGICAL_RESIST, owner.getObjectTemplate().getStatsTemplate().getMresist());
	}

	/** 返回 mb resist / Returns the mb resist */
	@Override
	public Stat2 getMBResist() {
		return getStat(StatEnum.MAGIC_SKILL_BOOST_RESIST, owner.getObjectTemplate().getStatsTemplate().getMBResist());
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
		return getStat(StatEnum.ACCURACY, 100);
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
		return getStat(StatEnum.EVASION, owner.getObjectTemplate().getStatsTemplate().getEvasion());
	}

	/** 返回 parry / Returns the parry */
	@Override
	public Stat2 getParry() {
		return getStat(StatEnum.PARRY, owner.getObjectTemplate().getStatsTemplate().getParry());
	}

	/** 返回黑名单 / Returns the block */
	@Override
	public Stat2 getBlock() {
		return getStat(StatEnum.BLOCK, owner.getObjectTemplate().getStatsTemplate().getBlock());
	}

	/** 返回 main hand p attack / Returns the main hand p attack */
	@Override
	public Stat2 getMainHandPAttack() {
		return getStat(StatEnum.PHYSICAL_ATTACK, owner.getObjectTemplate().getStatsTemplate().getMainHandAttack());
	}

	/** 返回 main hand p critical / Returns the main hand p critical */
	@Override
	public Stat2 getMainHandPCritical() {
		return getStat(StatEnum.PHYSICAL_CRITICAL, owner.getObjectTemplate().getStatsTemplate().getMainHandCritRate());
	}

	/** 返回 main hand p accuracy / Returns the main hand p accuracy */
	@Override
	public Stat2 getMainHandPAccuracy() {
		return getStat(StatEnum.PHYSICAL_ACCURACY, owner.getObjectTemplate().getStatsTemplate().getMainHandAccuracy());
	}

	/** 返回 m attack / Returns the m attack */
	@Override
	public Stat2 getMAttack() {
		return getStat(StatEnum.MAGICAL_ATTACK, 100);
	}

	/** 返回 main hand m attack / Returns the main hand m attack */
	@Override
	public Stat2 getMainHandMAttack() {
		return getStat(StatEnum.MAGICAL_ATTACK, owner.getObjectTemplate().getStatsTemplate().getPower());
	}

	/** 返回 off hand m attack / Returns the off hand m attack */
	@Override
	public Stat2 getOffHandMAttack() {
		return getStat(StatEnum.MAGICAL_ATTACK, 0);
	}

	/** 返回 m boost / Returns the m boost */
	@Override
	public Stat2 getMBoost() {
		return getStat(StatEnum.BOOST_MAGICAL_SKILL, owner.getObjectTemplate().getStatsTemplate().getMBoost()); // dmg
																												// NPC / npc
																												// 从 / from
																												// 法术 / spells
	}

	/** 返回 m accuracy / Returns the m accuracy */
	@Override
	public Stat2 getMAccuracy() {
		if (pAccuracy == 0)
			calcStats();
		// 陷阱魔法命中计入 TrapGameStats，并与 / Trap's MAccuracy is being calculated into TrapGameStats and is related to
		// 主人的魔法命中 / master's MAccuracy
		if (owner instanceof SummonedObject)
			return getStat(StatEnum.MAGICAL_ACCURACY, pAccuracy);
		return getMainHandPAccuracy();
	}

	/** 返回 m critical / Returns the m critical */
	@Override
	public Stat2 getMCritical() {
		return getStat(StatEnum.MAGICAL_CRITICAL, owner.getObjectTemplate().getStatsTemplate().getMCritical());
	}

	/** 返回 hp regen rate / Returns the hp regen rate */
	@Override
	public Stat2 getHpRegenRate() {
		// NpcStatsTemplate nst = owner.getObjectTemplate().getStatsTemplate();
		return getStat(StatEnum.REGEN_HP, owner.getObjectTemplate().getStatsTemplate().getHpRegenRate());
	}

	/** 返回 mp regen rate / Returns the mp regen rate */
	@Override
	public Stat2 getMpRegenRate() {
		throw new IllegalStateException("No mp regen for NPC");
	}

	/** 返回 last attack time delta / Returns the last attack time delta */
	public int getLastAttackTimeDelta() {
		return Math.round((System.currentTimeMillis() - lastAttackTime) / 1000f);
	}

	public long getLastAttackTime() {
		return lastAttackTime;
	}

	/** 返回 last attacked time delta / Returns the last attacked time delta */
	public int getLastAttackedTimeDelta() {
		return Math.round((System.currentTimeMillis() - lastAttackedTime) / 1000f);
	}

	public long getLastAttackedTime() {
		return lastAttackedTime;
	}

	/** 刷新上次攻击时间 / renew Last Attack Time. */
	public void renewLastAttackTime() {
		this.lastAttackTime = System.currentTimeMillis();
	}

	/** 刷新上次受击时间 / renew Last Attacked Time. */
	public void renewLastAttackedTime() {
		this.lastAttackedTime = System.currentTimeMillis();
	}

	/**
	 * @return Whether next attack scheduled
	 */
	public boolean isNextAttackScheduled() {
		return nextAttackTime - System.currentTimeMillis() > 50;
	}

	/** 设置 fight starting time / Sets the fight starting time */
	public void setFightStartingTime() {
		this.fightStartingTime = System.currentTimeMillis();
		this.lastSpawnPointChaseCheck = 0;
	}

	/** 返回 fight starting time / Returns the fight starting time */
	public long getFightStartingTime() {
		return this.fightStartingTime;
	}

	public long getLastSpawnPointChaseCheck() {
		return lastSpawnPointChaseCheck;
	}

	public void setLastSpawnPointChaseCheck(long time) {
		lastSpawnPointChaseCheck = time;
	}

	/** 设置 next attack time / Sets the next attack time */
	public void setNextAttackTime(long nextAttackTime) {
		this.nextAttackTime = nextAttackTime;
	}

	/**
	 * @return next possible attack time depending on stats
	 */
	public int getNextAttackInterval() {
		long attackDelay = System.currentTimeMillis() - lastAttackTime;
		int attackSpeed = getAttackSpeed().getCurrent();
		if (attackSpeed == 0) {
			attackSpeed = 2000;
		}
		if (owner.getAi2().isLogging()) {
			AI2Logger.info(owner.getAi2(), "adelay = " + attackDelay + " aspeed = " + attackSpeed);
		}
		int nextAttack = 0;
		if (attackDelay < attackSpeed) {
			nextAttack = (int) (attackSpeed - attackDelay);
		}
		return nextAttack;
	}

	/**
	 * @return next possible skill time depending on time
	 */

	public void renewLastSkillTime() {
		this.lastSkillTime = System.currentTimeMillis();
		this.nextSkillTime = lastSkillTime + Rnd.get(3000, 9000);
	}

	// 当前未使用 / not used at the moment
	/*
	 * public void renewLastSkilledTime() { this.lastSkilledTime =
	 * System.currentTimeMillis(); }
	 */

	public void renewLastChangeTargetTime() {
		this.lastChangeTarget = System.currentTimeMillis();
	}

	/** 返回 last skill time delta / Returns the last skill time delta */
	public int getLastSkillTimeDelta() {
		return Math.round((System.currentTimeMillis() - lastSkillTime) / 1000f);
	}

	// 当前未使用 / not used at the moment
	/*
	 * public int getLastSkilledTimeDelta() { return
	 * 1000f); }。
	 */

	public int getLastChangeTargetTimeDelta() {
		return Math.round((System.currentTimeMillis() - lastChangeTarget) / 1000f);
	}

	/** 是否使用下一技能 / Whether use next skill*/
	public boolean canUseNextSkill() {
		return isSkillDelayElapsed(System.currentTimeMillis(), nextSkillTime);
	}

	static boolean isSkillDelayElapsed(long currentTime, long nextSkillTime) {
		return currentTime >= nextSkillTime;
	}

	/** 更新 speed info / Update speed info */
	@Override
	public void updateSpeedInfo() {
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.START_EMOTE2, 0, 0));
	}

	/** 返回 last geo z update / Returns the last geo z update */
	public final long getLastGeoZUpdate() {
		return lastGeoZUpdate;
	}

	/**
	 * @param lastGeoZUpdate the lastGeoZUpdate to set
	 */
	public void setLastGeoZUpdate(long lastGeoZUpdate) {
		this.lastGeoZUpdate = lastGeoZUpdate;
	}

	private void calcStats() {
		int lvl = owner.getLevel();
		double accuracy = lvl * (33.6f - (0.16 * lvl)) + 5;
		NpcRating npcRating = owner.getObjectTemplate().getRating();
		if (npcRating != null) {
			switch (npcRating) {
			case JUNK:
				accuracy *= 1.00f;
				break;
			case NORMAL:
				accuracy *= 1.05f;
				break;
			case ELITE:
				accuracy *= 1.15f;
				break;
			case HERO:
				accuracy *= 1.25f;
				break;
			case LEGENDARY:
				accuracy *= 1.35f;
				break;
			}
		}
		this.pAccuracy = Math.round(owner.getAi2().modifyMaccuracy((int) accuracy));
	}
}
