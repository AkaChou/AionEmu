package com.aionemu.gameserver.model.instance;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.instance_bonusatrr.InstanceBonusAttr;
import com.aionemu.gameserver.model.templates.instance_bonusatrr.InstancePenaltyAttr;
import com.aionemu.gameserver.skillengine.change.Func;

/**
 * 副本 Buff 模型。
 * Instance Buff model.
 */

public class InstanceBuff implements StatOwner {

	private Future<?> task;
	private List<IStatFunction> functions = new ArrayList<IStatFunction>();
	private InstanceBonusAttr instanceBonusAttr;
	private long startTime;

	public InstanceBuff(int buffId) {
		instanceBonusAttr = DataManager.INSTANCE_BUFF_DATA.getInstanceBonusattr(buffId);
	}

	/** 应用效果。 / Apply effect. */
	public void applyEffect(Player player, int time) {

		if (hasInstanceBuff() || instanceBonusAttr == null) {
			return;
		}
		if (time != 0) {
			task = GameThreadPoolServices.threadPoolManager().schedule(new InstanceBuffTask(player), time);
		}
		startTime = System.currentTimeMillis();
		for (InstancePenaltyAttr instancePenaltyAttr : instanceBonusAttr.getPenaltyAttr()) {
			StatEnum stat = instancePenaltyAttr.getStat();
			int statToModified = player.getGameStats().getStat(stat, 0).getBase();
			int value = instancePenaltyAttr.getValue();
			int valueModified = instancePenaltyAttr.getFunc().equals(Func.PERCENT) ? (statToModified * value / 100)
					: (value);
			functions.add(new StatAddFunction(stat, valueModified, true));
		}
		player.getGameStats().addEffect(this, functions);
	}

	/** 结束效果 / End Effect */
	public void endEffect(Player player) {
		functions.clear();
		if (hasInstanceBuff()) {
			task.cancel(true);
		}
		player.getGameStats().endEffect(this);
	}

	/** Apply pledge / Apply pledge */
	public void applyPledge(Player player, int buffId) {
		if (instanceBonusAttr == null) {
			return;
		}
		for (InstancePenaltyAttr instancePenaltyAttr : instanceBonusAttr.getPenaltyAttr()) {
			if (instancePenaltyAttr.getFunc().equals(Func.PERCENT)) {
				functions
						.add(new StatRateFunction(instancePenaltyAttr.getStat(), instancePenaltyAttr.getValue(), true));
			} else {
				functions.add(new StatAddFunction(instancePenaltyAttr.getStat(), instancePenaltyAttr.getValue(), true));
			}
		}
		player.setBonusId(buffId);
		player.getGameStats().addEffect(this, functions);
	}

	/** 结束誓约 / end Pledge. */
	public void endPledge(Player player) {
		functions.clear();
		player.setBonusId(0);
		player.getGameStats().endEffect(this);
	}

	/** Apply pledge duration / Apply pledge duration */
	public void applyPledgeDuration(Player player, int buffId, int time) {
		if (hasInstanceBuff() || instanceBonusAttr == null) {
			return;
		}
		if (time != 0) {
			task = GameThreadPoolServices.threadPoolManager().schedule(new InstanceBuffTask(player), time);
		}
		startTime = System.currentTimeMillis();
		for (InstancePenaltyAttr instancePenaltyAttr : instanceBonusAttr.getPenaltyAttr()) {
			if (instancePenaltyAttr.getFunc().equals(Func.PERCENT)) {
				functions
						.add(new StatRateFunction(instancePenaltyAttr.getStat(), instancePenaltyAttr.getValue(), true));
			} else {
				functions.add(new StatAddFunction(instancePenaltyAttr.getStat(), instancePenaltyAttr.getValue(), true));
			}
		}
		player.setBonusId(buffId);
		player.getGameStats().addEffect(this, functions);
	}

	/** 结束誓约时长 / end Pledge Duration. */
	public void endPledgeDuration(Player player) {
		functions.clear();
		if (hasInstanceBuff()) {
			task.cancel(true);
			player.setBonusId(0);
		}
		player.getGameStats().endEffect(this);
	}

	/** 返回 remaning time / Returns the remaning time */
	public int getRemaningTime() {
		return (int) ((System.currentTimeMillis() - startTime) / 1000);
	}

	private class InstanceBuffTask implements Runnable {

		private Player player;

		public InstanceBuffTask(Player player) {
			this.player = player;
		}

		/** 运行 / run. */
		@Override
		public void run() {
			endEffect(player);
			if (player.getBonusId() > 0) {
				endPledgeDuration(player);
			}
		}
	}

	/** 是否实例增益 / Whether instance buff*/
	public boolean hasInstanceBuff() {
		return task != null && !task.isDone();
	}
}
