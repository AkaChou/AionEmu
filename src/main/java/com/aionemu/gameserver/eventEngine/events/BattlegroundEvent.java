package com.aionemu.gameserver.eventEngine.events;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.eventEngine.Event;

/**
 * 战场活动事件：创建普通战场并跟踪进行中的战场实例。
 * Battleground event that creates normal BGs and tracks live instances.
 *
 * @author wanke
 */
public class BattlegroundEvent extends Event {

	/**
	 * 进行中的战场 ID 列表。
	 * Live battleground ids.
	 */
	private List<Integer> battlegrounds = new ArrayList<Integer>();

	/**
	 * 创建普通战场。
	 * Creates normal battlegrounds.
	 */
	@Override
	public void execute() {
		GameFeatureServices.ladderService().createNormalBgs(this);
	}

	/**
	 * 当前进行中战场数量。
	 * Number of live battlegrounds.
	 *
	 * @return 进行中战场数量 / live bg count
	 */
	public int getBgCount() {
		return battlegrounds.size();
	}

	/**
	 * 战场创建回调，记录战场 ID。
	 * Callback when a BG is created; records the id.
	 *
	 * @param bgId 战场 ID / battleground id
	 */
	public void onCreate(Integer bgId) {
		if (!battlegrounds.contains(bgId)) {
			battlegrounds.add(bgId);
		}
	}

	/**
	 * 单个战场结束回调；全部结束后结束事件。
	 * Callback when one BG ends; finishes the event when none remain.
	 *
	 * @param bgId 战场 ID / battleground id
	 */
	public void onEnd(Integer bgId) {
		battlegrounds.remove(bgId);
		if (battlegrounds.isEmpty()) {
			this.onEnd();
		}
	}

	/**
	 * 全部战场结束。
	 * All battlegrounds ended.
	 */
	public void onEnd() {
		super.finish();
	}

	/**
	 * 清空进行中战场列表。
	 * Clears live battleground list.
	 */
	@Override
	protected void onReset() {
		battlegrounds.clear();
	}

	/**
	 * 不支持取消。
	 * Cancel is not supported.
	 *
	 * @param mayInterruptIfRunning 忽略参数 / ignored
	 * @return 始终为 false / always false
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}
}
