package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_UPDATE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 召唤物的生命值/魔法值属性与恢复逻辑。
 * Summon HP/MP stats and restore logic.
 * @author ATracer
 */
public class SummonLifeStats extends CreatureLifeStats<Summon> {

	public SummonLifeStats(Summon owner) {
		super(owner, owner.getGameStats().getMaxHp().getCurrent(), owner.getGameStats().getMaxMp().getCurrent());
	}

	@Override
	protected void onIncreaseHp(TYPE type, int value, int skillId, LOG log) {
		sendAttackStatusPacketUpdate(type, value, skillId, log);
		sendSummonPanelUpdate();
	}

	/**
	 * 通知主人刷新召唤面板（绝对生命值）。
	 * Notifies the master to refresh the summon panel (absolute HP).
	 * <p>血条由 {@code SM_ATTACK_STATUS} 驱动，而面板消费的是绝对生命值，两者必须分别下发；
	 * 抽成独立方法便于测试观测而不触网。主人引用会被召唤释放流程置空，因此必须判空。</p>
	 * <p>The bar is driven by SM_ATTACK_STATUS while the panel consumes absolute HP, so both must be sent; this
	 * seam keeps it observable in tests without touching the network. The master reference is cleared by the
	 * summon-release flow, so the null check is required.</p>
	 */
	protected void sendSummonPanelUpdate() {
		Player master = getOwner().getMaster();
		if (master != null) {
			PacketSendUtility.sendPacket(master, new SM_SUMMON_UPDATE(getOwner()));
		}
	}

	@Override
	protected void onIncreaseMp(TYPE type, int value, int skillId, LOG log) {
	}

	/**
	 * 有意保持空实现：攻击路径的伤害广播由 {@code SummonController#onAttack} 独占，
	 * 而它已经调用过 {@code CreatureLifeStats#reduceHp}；此处再补发会形成双包。
	 * Intentionally empty: the attack path already broadcasts via SummonController#onAttack after calling
	 * reduceHp; emitting the sync packet here would duplicate it.
	 */
	@Override
	protected void onReduceHp() {
	}

	/**
	 * 非攻击流程扣血（技能效果、施法代价、周期性消耗）同步血条与主人面板。
	 * Synchronizes non-attack HP reduction (skill effects, cast costs, periodic drains) to the bar and panel.
	 * <p>召唤物有两条客户端通道：血条由 {@code SM_ATTACK_STATUS} 广播给能看到它的玩家，而主人的召唤面板
	 * 消费的是绝对生命值，必须单独下发 {@code SM_SUMMON_UPDATE}。攻击路径不经过本入口，其广播仍由
	 * {@code SummonController#onAttack} 独占。此处不做出生守卫：未进入世界时已知列表为空、广播自然退化，
	 * 而传送挂起期间主人面板仍然存在，同步反而有益。</p>
	 * <p>A summon has two client channels: the bar is broadcast through SM_ATTACK_STATUS to every player that can
	 * see it, while the master's summon panel consumes absolute HP and needs its own SM_SUMMON_UPDATE. The attack
	 * path never reaches this entry, so its broadcast stays owned by SummonController#onAttack. No spawn guard is
	 * applied here: before the creature is in the world the known list is empty and the broadcast degrades to a
	 * no-op, while the master panel still exists during a pending teleport and benefits from the update.</p>
	 * @param value 扣除量 / amount to reduce
	 * @param attacker 归因来源 / source of the reduction
	 * @return 变化后的生命值 / the resulting HP
	 */
	@Override
	public int reduceHpFromEffect(int value, Creature attacker) {
		int hpBefore = getCurrentHp();
		int hpPercentBefore = getHpPercentage();
		int hpAfter = super.reduceHpFromEffect(value, attacker);
		if (hpPercentBefore != getHpPercentage()) {
			sendAttackStatusPacketUpdate(TYPE.HP, hpAfter - hpBefore, 0, LOG.REGULAR);
			sendSummonPanelUpdate();
		}
		return hpAfter;
	}

	@Override
	protected void onReduceMp() {
	}

	/** 返回所有者 / Returns the owner. */
	@Override
	public Summon getOwner() {
		return super.getOwner();
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
}
