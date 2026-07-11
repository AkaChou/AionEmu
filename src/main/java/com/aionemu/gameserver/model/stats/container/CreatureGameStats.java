package com.aionemu.gameserver.model.stats.container;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.ReverseStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatCapUtil;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunctionProxy;
import com.aionemu.gameserver.utils.stats.CalculationType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 生物游戏属性，用于属性相关逻辑。
 * Creature Game Stats for stats logic.
 */

@Slf4j(access = AccessLevel.PROTECTED)
public abstract class CreatureGameStats<T extends Creature> {
	private static final int ATTACK_MAX_COUNTER = Integer.MAX_VALUE;
	private long lastGeoUpdate = 0;
	private Map<StatEnum, TreeSet<IStatFunction>> stats;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private int attackCounter = 0;
	protected T owner = null;
	private Stat2 cachedHPStat;
	private Stat2 cachedMPStat;

	protected CreatureGameStats(T owner) {
		this.owner = owner;
		this.stats = new LinkedHashMap<StatEnum, TreeSet<IStatFunction>>();
	}

	/**
	 * @return the atcount
	 */
	public int getAttackCounter() {
		return attackCounter;
	}

	/**
	 * @param attackCounter the atcount to set
	 */
	protected void setAttackCounter(int attackCounter) {
		if (attackCounter <= 0) {
			this.attackCounter = 1;
		} else {
			this.attackCounter = attackCounter;
		}
	}

	/** Increase attack counter / Increase attack counter */
	public void increaseAttackCounter() {
		if (attackCounter == ATTACK_MAX_COUNTER) {
			this.attackCounter = 1;
		} else {
			this.attackCounter++;
		}
	}

	/** 添加 effect only / Adds effect only */
	public final void addEffectOnly(StatOwner statOwner, List<? extends IStatFunction> functions) {
		lock.writeLock().lock();
		try {
			for (IStatFunction function : functions) {
				if (!stats.containsKey(function.getName())) {
					stats.put(function.getName(), new TreeSet<IStatFunction>());
				}
				IStatFunction func = function;
				if (function instanceof StatFunction) {
					func = new StatFunctionProxy(statOwner, function);
				}
				addFunction(function.getName(), func);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/** 添加效果。 / Adds effect. */
	public final void addEffect(StatOwner statOwner, List<? extends IStatFunction> functions) {
		addEffectOnly(statOwner, functions);
		onStatsChange();
	}

	/** 结束效果 / End Effect */
	public final void endEffect(StatOwner statOwner) {
		lock.writeLock().lock();
		try {
			for (Entry<StatEnum, TreeSet<IStatFunction>> e : stats.entrySet()) {
				TreeSet<IStatFunction> value = e.getValue();
				for (Iterator<IStatFunction> iter = value.iterator(); iter.hasNext();) {
					IStatFunction ownedMod = iter.next();
					if (ownedMod.getOwner() != null && ownedMod.getOwner().equals(statOwner)) {
						iter.remove();
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
		onStatsChange();
	}

	/** 返回 positive stat / Returns the positive stat */
	public int getPositiveStat(StatEnum statEnum, int base) {
		Stat2 stat = getStat(statEnum, base);
		int value = stat.getCurrent();
		return value > 0 ? value : 0;
	}

	/** 返回 positive reverse stat / Returns the positive reverse stat */
	public int getPositiveReverseStat(StatEnum statEnum, int base) {
		Stat2 stat = getReverseStat(statEnum, base);
		int value = stat.getCurrent();
		return value > 0 ? value : 0;
	}

	/** 获取属性。 / Returns the stat. */
	public Stat2 getStat(StatEnum statEnum, int base) {
		return getStat(statEnum, base, new CalculationType[0]);
	}

	/** 获取属性。 / Returns the stat. */
	public Stat2 getStat(StatEnum statEnum, int base, CalculationType... calculationTypes) {
		Stat2 stat = new AdditionStat(statEnum, base, (Creature) owner);
		return getStat(statEnum, stat, calculationTypes);
	}

	/** 获取属性。 / Returns the stat. */
	public Stat2 getStat(StatEnum statEnum, float base, CalculationType... calculationTypes) {
		Stat2 stat = new AdditionStat(statEnum, base, (Creature) owner);
		return getStat(statEnum, stat, calculationTypes);
	}

	/** 获取属性。 / Returns the stat. */
	public Stat2 getStat(StatEnum statEnum, int base, float bonusRate, CalculationType... calculationTypes) {
		Stat2 stat = new AdditionStat(statEnum, base, (Creature) owner, bonusRate);
		return getStat(statEnum, stat, calculationTypes);
	}

	/** 返回 reverse stat / Returns the reverse stat */
	public Stat2 getReverseStat(StatEnum statEnum, int base) {
		Stat2 stat = new ReverseStat(statEnum, base, (Creature) owner);
		return getStat(statEnum, stat);
	}

	/** 返回 reverse stat / Returns the reverse stat */
	public Stat2 getReverseStat(StatEnum statEnum, int base, float bonusRate) {
		Stat2 stat = new ReverseStat(statEnum, base, (Creature) owner, bonusRate);
		return getStat(statEnum, stat);
	}

	/** 获取属性。 / Returns the stat. */
	public Stat2 getStat(StatEnum statEnum, Stat2 stat, CalculationType... calculationTypes) {
		lock.readLock().lock();
		try {
			TreeSet<IStatFunction> functions = getStatsByStatEnum(statEnum);
			if (functions == null) {
				return stat;
			}
			for (IStatFunction func : functions) {
				if (func.validate(stat, func)) {
					func.apply(stat, calculationTypes);
				}
			}
			StatCapUtil.calculateBaseValue(stat, ((Creature) owner).isPlayer());

			if (SecurityConfig.STATS_CHECK) {
				StatCapUtil.dumpWrongStats(owner.getName(), stat);
			}

			return stat;
		} finally {
			lock.readLock().unlock();
		}
	}

	/** 返回 item stat boost / Returns the item stat boost */
	public Stat2 getItemStatBoost(StatEnum statEnum, Stat2 stat) {
		lock.readLock().lock();
		try {
			TreeSet<IStatFunction> functions = getStatsByStatEnum(statEnum);
			if (functions == null || functions.isEmpty()) {
				return stat;
			}
			for (IStatFunction func : functions) {
				if (func.validate(stat, func)
						&& (func.getOwner() instanceof Item || func.getOwner() instanceof ManaStone)) {
					func.apply(stat);
				}
			}
		} finally {
			lock.readLock().unlock();
		}
		return stat;
	}

	/** 返回最大生命 / Returns the max hp*/
	public abstract Stat2 getMaxHp();

	/** 返回最大魔法 / Returns the max mp*/
	public abstract Stat2 getMaxMp();

	/** 返回 attack speed / Returns the attack speed */
	public abstract Stat2 getAttackSpeed();

	/** 返回 movement speed / Returns the movement speed */
	public abstract Stat2 getMovementSpeed();

	/** 返回攻击范围 / Returns the attack range*/
	public abstract Stat2 getAttackRange();

	/** 返回 p def / Returns the p def */
	public abstract Stat2 getPDef();

	/** 返回 m def / Returns the m def */
	public abstract Stat2 getMDef();

	/** 返回 m resist / Returns the m resist */
	public abstract Stat2 getMResist();

	/** 返回 power / Returns the power */
	public abstract Stat2 getPower();

	/** 返回 health / Returns the health */
	public abstract Stat2 getHealth();

	/** 返回 accuracy / Returns the accuracy */
	public abstract Stat2 getAccuracy();

	/** 返回 agility / Returns the agility */
	public abstract Stat2 getAgility();

	/** 返回 knowledge / Returns the knowledge */
	public abstract Stat2 getKnowledge();

	/** 返回 will / Returns the will */
	public abstract Stat2 getWill();

	/** 返回 evasion / Returns the evasion */
	public abstract Stat2 getEvasion();

	/** 返回 parry / Returns the parry */
	public abstract Stat2 getParry();

	/** 返回黑名单 / Returns the block */
	public abstract Stat2 getBlock();

	/** 返回 main hand p attack / Returns the main hand p attack */
	public abstract Stat2 getMainHandPAttack();

	/** 返回 main hand p attack / Returns the main hand p attack */
	public Stat2 getMainHandPAttack(CalculationType... calculationTypes) {
		return getMainHandPAttack();
	}

	/** 返回 main hand p critical / Returns the main hand p critical */
	public abstract Stat2 getMainHandPCritical();

	/** 返回 main hand p accuracy / Returns the main hand p accuracy */
	public abstract Stat2 getMainHandPAccuracy();

	/** 返回 m attack / Returns the m attack */
	public abstract Stat2 getMAttack();

	/** 返回 main hand m attack / Returns the main hand m attack */
	public abstract Stat2 getMainHandMAttack();

	/** 返回 main hand m attack / Returns the main hand m attack */
	public Stat2 getMainHandMAttack(CalculationType... calculationTypes) {
		return getMainHandMAttack();
	}

	/** 返回 off hand m attack / Returns the off hand m attack */
	public abstract Stat2 getOffHandMAttack();

	/** 返回 off hand m attack / Returns the off hand m attack */
	public Stat2 getOffHandMAttack(CalculationType... calculationTypes) {
		return getOffHandMAttack();
	}

	/** 返回 m boost / Returns the m boost */
	public abstract Stat2 getMBoost();

	/** 返回 mb resist / Returns the mb resist */
	public abstract Stat2 getMBResist();

	/** 返回 m accuracy / Returns the m accuracy */
	public abstract Stat2 getMAccuracy();

	/** 返回 m critical / Returns the m critical */
	public abstract Stat2 getMCritical();

	/** 返回 hp regen rate / Returns the hp regen rate */
	public abstract Stat2 getHpRegenRate();

	/** 返回 mp regen rate / Returns the mp regen rate */
	public abstract Stat2 getMpRegenRate();

	/** 返回 strike resist / Returns the strike resist */
	public abstract Stat2 getStrikeResist();

	/** 返回 strike fort / Returns the strike fort */
	public abstract Stat2 getStrikeFort();

	/** 返回 spell resist / Returns the spell resist */
	public abstract Stat2 getSpellResist();

	/** 返回 spell fort / Returns the spell fort */
	public abstract Stat2 getSpellFort();

	/** 返回 b casting time / Returns the b casting time */
	public abstract Stat2 getBCastingTime();

	/** 返回 concentration / Returns the concentration */
	public abstract Stat2 getConcentration();

	/** 返回 root resistance / Returns the root resistance */
	public abstract Stat2 getRootResistance();

	/** 返回 snare resistance / Returns the snare resistance */
	public abstract Stat2 getSnareResistance();

	/** 返回 bind resistance / Returns the bind resistance */
	public abstract Stat2 getBindResistance();

	/** 返回 fear resistance / Returns the fear resistance */
	public abstract Stat2 getFearResistance();

	/** 返回 sleep resistance / Returns the sleep resistance */
	public abstract Stat2 getSleepResistance();

	/** 返回 all speed / Returns the all speed */
	public abstract Stat2 getAllSpeed();

	/** 返回魔法防御 / Returns the magical defense for*/
	public int getMagicalDefenseFor(SkillElement element) {
		if (element == SkillElement.EARTH) {
			return getStat(StatEnum.EARTH_RESISTANCE, 0).getCurrent();
		}
		if (element == SkillElement.FIRE) {
			return getStat(StatEnum.FIRE_RESISTANCE, 0).getCurrent();
		}
		if (element == SkillElement.WATER) {
			return getStat(StatEnum.WATER_RESISTANCE, 0).getCurrent();
		}
		if (element == SkillElement.WIND) {
			return getStat(StatEnum.WIND_RESISTANCE, 0).getCurrent();
		}
		if (element == SkillElement.LIGHT) {
			return getStat(StatEnum.ELEMENTAL_RESISTANCE_LIGHT, 0).getCurrent();
		}
		if (element == SkillElement.DARK) {
			return getStat(StatEnum.ELEMENTAL_RESISTANCE_DARK, 0).getCurrent();
		}
		return 0;
	}

	/** 返回 movement speed float / Returns the movement speed float */
	public float getMovementSpeedFloat() {
		return getMovementSpeed().getCurrent() / 1000f;
	}

	/**
	 * 发送数据包 aboutstatsinfo。 / Send packet about stats info
	 */
	public void updateStatInfo() {
	}

	/**
	 * 发送数据包 aboutspeedinfo。 / Send packet about speed info
	 */
	public void updateSpeedInfo() {
	}

	/** 按 stat enum 返回 stats / Returns the stats by stat enum */
	public TreeSet<IStatFunction> getStatsByStatEnum(StatEnum stat) {
		TreeSet<IStatFunction> allStats = stats.get(stat);
		if (allStats == null) {
			return null;
		}
		TreeSet<IStatFunction> tmp = new TreeSet<IStatFunction>();
		List<IStatFunction> setFuncs = null;
		for (IStatFunction func : allStats) {
			if (func.getPriority() >= Integer.MAX_VALUE - 10) {
				if (setFuncs == null) {
					setFuncs = new ArrayList<IStatFunction>();
				}
				setFuncs.add(func);
			} else if (setFuncs != null) {
				break;
			}
		}
		if (setFuncs == null) {
			tmp.addAll(allStats);
		} else {
			tmp.addAll(setFuncs);
		}
		return tmp;
	}

	private void addFunction(StatEnum stat, IStatFunction function) {
		TreeSet<IStatFunction> allStats = stats.get(stat);
		allStats.add(function);
	}

	/**
	 * @return
	 */
	public boolean checkGeoNeedUpdate() {
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastGeoUpdate > 600) {
			lastGeoUpdate = currentTime;
			return true;
		}
		return false;
	}

	/**
	 * 效果添加/移除后执行额外计算（在属性锁外调用）。 / Perform additional calculations after effects added/removed<br> This method will be called outside of stats lock
	 */
	protected void onStatsChange() {
		checkHPStats();
		checkMPStats();
	}

	private void checkHPStats() {
		Stat2 oldHP = cachedHPStat;
		cachedHPStat = null;
		Stat2 newHP = this.getMaxHp();
		cachedHPStat = newHP;
		if (oldHP == null) {
			return;
		}
		if (oldHP.getCurrent() != newHP.getCurrent()) {
			float percent = 1f * newHP.getCurrent() / oldHP.getCurrent();
			owner.getLifeStats().setCurrentHp(Math.round(owner.getLifeStats().getCurrentHp() * percent));
		}
	}

	private void checkMPStats() {
		Stat2 oldMP = cachedMPStat;
		cachedMPStat = null;
		Stat2 newMP = this.getMaxMp();
		cachedMPStat = newMP;
		if (oldMP == null) {
			return;
		}
		if (oldMP.getCurrent() != newMP.getCurrent()) {
			float percent = 1f * newMP.getCurrent() / oldMP.getCurrent();
			owner.getLifeStats().setCurrentMp(Math.round(owner.getLifeStats().getCurrentMp() * percent));
		}
	}
}
