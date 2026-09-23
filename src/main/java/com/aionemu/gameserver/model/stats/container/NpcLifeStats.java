package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;

/**
 * NPC 的生命值/魔法值属性与恢复逻辑。
 * NPC HP/MP stats and restore logic.
 * @author ATracer
 */
public class NpcLifeStats extends CreatureLifeStats<Npc> {

	/**
	 * 创建 NPC 生命属性。
	 * Creates NPC life stats.
	 * @param owner 所属 NPC / owner NPC
	 */
	public NpcLifeStats(Npc owner) {
		super(owner, owner.getGameStats().getMaxHp().getCurrent(), owner.getGameStats().getMaxMp().getCurrent());
	}

	@Override
	protected void onIncreaseHp(TYPE type, int value, int skillId, LOG log) {
		sendAttackStatusPacketUpdate(packetType(type), value, skillId, log);
	}

	static TYPE packetType(TYPE type) {
		return type == TYPE.NATURAL_HP ? TYPE.HP : type;
	}

	@Override
	protected void onIncreaseMp(TYPE type, int value, int skillId, LOG log) {
	}

	/**
	 * 有意保持空实现：攻击路径的伤害广播由 {@code NpcController#onAttack} 独占，而它已经调用过
	 * {@code CreatureLifeStats#reduceHp}；此处再补发会形成双包双飘字。
	 * Intentionally empty: the attack path already broadcasts via NpcController#onAttack, which has invoked
	 * reduceHp itself; emitting the sync packet here would duplicate both the packet and the floating text.
	 */
	@Override
	protected void onReduceHp() {
	}

	@Override
	protected void onReduceMp() {
	}

	/**
	 * 判断一次直接改血是否产生了客户端可见的生命百分比变化。
	 * Decides whether a direct HP assignment produced a client-visible HP percentage change.
	 * <p>客户端血条的唯一数据源是生命百分比，只有百分比真的变了才需要补发同步包。只比较绝对值会误报：
	 * {@code CreatureGameStats#checkHPStats} 在最大生命变化时按比例重算当前生命，百分比被刻意保持不变，
	 * 此时绝对值有增减却没有任何可见变化（例如实例人数变化会对全实例生物重算，误报会形成整片假飘字）。</p>
	 * <p>The sole source of the client HP bar is the HP percentage, so only a real percentage change needs a
	 * sync packet. Comparing absolute HP alone would misfire: CreatureGameStats#checkHPStats rescales the
	 * current HP when max HP changes while deliberately keeping the percentage unchanged, so the absolute
	 * delta carries no visible meaning (an instance population change rescales every creature in it, and a
	 * misfire would flood clients with fake floats).</p>
	 * @param hpPercentBefore 改血前的生命百分比 / HP percentage before the assignment
	 * @param hpPercentAfter 改血后的生命百分比 / HP percentage after the assignment
	 * @return 是否需要补发同步包 / whether a sync packet must be sent
	 */
	static boolean hasVisibleHpChange(int hpPercentBefore, int hpPercentAfter) {
		return hpPercentBefore != hpPercentAfter;
	}

	/**
	 * 同步出口的前置条件：目标存在且已进入世界。
	 * Precondition of the sync outlet: the target exists and has entered the world.
	 * <p>未进入世界时不补包：客户端还没见过该生物，出生包 {@code SM_NPC_INFO} 自带正确百分比；
	 * 该短路同时让「每只 NPC 出生都会执行」的初始化改血保持零额外开销。</p>
	 * <p>No packet before the creature is in the world: no client has seen it yet and the spawn packet
	 * SM_NPC_INFO already carries the correct percentage. The short-circuit also keeps the initialization
	 * assignment that runs for every NPC spawn free of extra cost.</p>
	 * @return 是否允许补发同步包 / whether a sync packet may be sent
	 */
	private boolean canSyncDirectHpSet() {
		return owner != null && owner.isSpawned();
	}

	/**
	 * 直接设置当前生命值，并在需要时把变化同步给周边玩家。
	 * Directly sets the current HP and synchronizes the change to nearby players when required.
	 * <p>AI 脚本与属性重算会绕过攻击流程直接改血，基类入口本身不广播；本覆写把这些改血统一收敛到血量
	 * 同步出口，攻击路径的广播仍由 {@code NpcController#onAttack} 独占，两条通道互不重复。</p>
	 * <p>AI scripts and stat recalculations bypass the attack flow when they assign HP, and the base entry
	 * point never broadcasts. This override funnels those assignments into the HP sync outlet, while the
	 * attack path keeps broadcasting exclusively through NpcController#onAttack without overlap.</p>
	 * @param hp 目标生命值 / target HP
	 */
	@Override
	public void setCurrentHp(int hp) {
		if (!canSyncDirectHpSet()) {
			super.setCurrentHp(hp);
			return;
		}
		int hpBefore;
		int hpPercentBefore;
		int hpAfter;
		int hpPercentAfter;
		restoreLock.lock();
		try {
			hpBefore = getCurrentHp();
			hpPercentBefore = getHpPercentage();
			super.setCurrentHp(hp);
			hpAfter = getCurrentHp();
			hpPercentAfter = getHpPercentage();
		} finally {
			restoreLock.unlock();
		}
		if (hasVisibleHpChange(hpPercentBefore, hpPercentAfter)) {
			sendAttackStatusPacketUpdate(TYPE.HP, hpAfter - hpBefore, 0, LOG.REGULAR);
		}
	}

	/**
	 * 按百分比直接设置当前生命值，并在需要时把变化同步给周边玩家。
	 * Directly sets the current HP by percentage and synchronizes the change to nearby players when required.
	 * <p>与 {@link #setCurrentHp(int)} 共用同一个同步判据与出口，用于阶段转换、换阶段回血等按百分比改血的脚本。</p>
	 * <p>Shares the same sync predicate and outlet as setCurrentHp(int); used by scripts that assign HP by
	 * percentage, such as phase transitions and scripted heals.</p>
	 * @param hpPercent 目标生命百分比 / target HP percentage
	 */
	@Override
	public void setCurrentHpPercent(int hpPercent) {
		if (!canSyncDirectHpSet()) {
			super.setCurrentHpPercent(hpPercent);
			return;
		}
		int hpBefore;
		int hpPercentBefore;
		int hpAfter;
		int hpPercentAfter;
		restoreLock.lock();
		try {
			hpBefore = getCurrentHp();
			hpPercentBefore = getHpPercentage();
			super.setCurrentHpPercent(hpPercent);
			hpAfter = getCurrentHp();
			hpPercentAfter = getHpPercentage();
		} finally {
			restoreLock.unlock();
		}
		if (hasVisibleHpChange(hpPercentBefore, hpPercentAfter)) {
			sendAttackStatusPacketUpdate(TYPE.HP, hpAfter - hpBefore, 0, LOG.REGULAR);
		}
	}

	/**
	 * 最大生命变化时的等比重算保持客户端可见百分比不变，因此静默改值、不补发同步包。
	 * The proportional rescale after a max-HP change preserves the client-visible percentage, so it assigns
	 * the value silently without a sync packet.
	 * <p>注意不能用百分比变化来推断这一点：{@code CreatureGameStats#checkHPStats} 先让新的最大生命生效再重算当前生命，
	 * 因此覆写入口里读到的「变化前百分比」已经按新上限计算，等比重算看起来像百分比变了，实际客户端毫无感知。</p>
	 * <p>The percentage cannot be used to detect this: CreatureGameStats#checkHPStats lets the new max HP take
	 * effect before rescaling the current HP, so the "before" percentage read inside the override is already
	 * based on the new cap — a proportional rescale then looks like a percentage change although no client
	 * can perceive one.</p>
	 * @param hp 重算后的生命值 / rescaled HP value
	 */
	@Override
	public void rescaleCurrentHp(int hp) {
		super.setCurrentHp(hp);
	}

	/**
	 * 非攻击流程扣血（技能效果、施法代价、周期性消耗）在需要时补发血量同步包。
	 * Synchronizes non-attack HP reduction (skill effects, cast costs, periodic drains) to nearby players.
	 * <p>与直接设血的出口同构：未进入世界时短路，按客户端可见的生命百分比变化判断，补发有符号的实际差值。
	 * 攻击路径不会经过本入口，其广播仍由 {@code NpcController#onAttack} 独占。</p>
	 * <p>Mirrors the direct-assignment outlets: it short-circuits before the creature enters the world, decides
	 * by the client-visible HP percentage, and sends the signed actual delta. The attack path never reaches this
	 * entry, so its broadcast stays owned by NpcController#onAttack.</p>
	 * @param value 扣除量 / amount to reduce
	 * @param attacker 归因来源 / source of the reduction
	 * @return 变化后的生命值 / the resulting HP
	 */
	@Override
	public int reduceHpFromEffect(int value, Creature attacker) {
		if (!canSyncDirectHpSet()) {
			return super.reduceHpFromEffect(value, attacker);
		}
		int hpBefore = getCurrentHp();
		int hpPercentBefore = getHpPercentage();
		int hpAfter = super.reduceHpFromEffect(value, attacker);
		if (hasVisibleHpChange(hpPercentBefore, getHpPercentage())) {
			sendAttackStatusPacketUpdate(TYPE.HP, hpAfter - hpBefore, 0, LOG.REGULAR);
		}
		return hpAfter;
	}

	/** 触发恢复任务 / Trigger restore task */
	@Override
	public void triggerRestoreTask() {
		restoreLock.lock();
		try {
			if (lifeRestoreTask == null && !alreadyDead) {
				this.lifeRestoreTask = GameGameplayServices.lifeStatsRestoreService().scheduleHpRestoreTask(this);
			}
		} finally {
			restoreLock.unlock();
		}
	}

	/** 开始休息（触发恢复任务） / Start resting (triggers restore task) */
	public void startResting() {
		triggerRestoreTask();
	}
}
