package com.aionemu.gameserver.skillengine.task;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemQuality;

/**
 * 制作类任务基类：维护成功/失败进度与暴击类型，驱动交互 tick。
 * Base craft task: tracks success/failure progress and crit type across interaction ticks.
 */
public abstract class AbstractCraftTask extends AbstractInteractionTask {

	/**
	 * 成功进度上限。
	 * Maximum success progress value.
	 */
	protected int maxSuccessValue = 100;

	/**
	 * 失败进度上限。
	 * Maximum failure progress value.
	 */
	protected int maxFailureValue = 100;

	/**
	 * 当前成功进度。
	 * Current success progress.
	 */
	protected int currentSuccessValue;

	/**
	 * 当前失败进度。
	 * Current failure progress.
	 */
	protected int currentFailureValue;

	/**
	 * 技能等级差（影响成功权重）。
	 * Skill level difference (affects success weighting).
	 */
	protected int skillLvlDiff;

	/**
	 * 本 tick 是否触发加速。
	 * Whether this tick triggered a craft speed-up.
	 */
	protected boolean craftSpeedUp;

	/**
	 * 当前暴击类型。
	 * Current craft crit type.
	 */
	protected CraftCritType critType = CraftCritType.NONE;

	/**
	 * 产物物品品质。
	 * Product item quality.
	 */
	protected ItemQuality itemQuality;

	/**
	 * 制作暴击类型。
	 * Craft critical-hit type.
	 */
	protected enum CraftCritType {

		NONE(0), INSTANT(1), BLUE(2), PURPLE(3);

		private int critId;

		private CraftCritType(int critId) {
			this.critId = critId;
		}

		/**
		 * 获取暴击 ID。
		 * Returns the crit id.
		 *
		 * crit id
		 */
		public int getCritId() {
			return critId;
		}

		/**
		 * 获取发包用暴击 ID（NONE 时回退为 1）。
		 * Returns the packet crit id (falls back to 1 for NONE).
		 *
		 * packet id
		 */
		public int getPacketId() {
			return critId > 0 ? critId : 1;
		}
	}

	/**
	 * 构造制作任务。
	 * Creates a craft task.
	 *
	 * requesting player
	 * responder
	 * @param skillLvlDiff 技能等级差 / skill level difference
	 */
	public AbstractCraftTask(Player requestor, VisibleObject responder, int skillLvlDiff) {
		super(requestor, responder);
		this.skillLvlDiff = skillLvlDiff;
	}

	/**
	 * 执行一次制作交互：检查完成条件并更新进度。
	 * Performs one craft tick: checks finish conditions and updates progress.
	 *
	 * @return true 表示任务应停止 / true if the task should stop
	 */
	@Override
	protected boolean onInteraction() {
		if (currentSuccessValue == maxSuccessValue) {
			return onSuccessFinish();
		}
		if (currentFailureValue == maxFailureValue) {
			onFailureFinish();
			return true;
		}
		analyzeInteraction();
		sendInteractionUpdate();
		return false;
	}

	/**
	 * 分析本 tick 成功/失败进度增量。
	 * Analyzes this tick's success/failure progress increments.
	 */
	protected void analyzeInteraction() {
		craftSpeedUp = false;
		int multi = Math.max(0, 33 - skillLvlDiff * 5);
		craftSpeedUp = Rnd.get(100) <= 15;
		if (craftSpeedUp) {
			currentSuccessValue += Rnd.get(maxSuccessValue / 2, maxSuccessValue);
		}
		if (Rnd.get(100) > multi) {
			currentSuccessValue += Rnd.get(maxSuccessValue / (multi + 1) / 2, maxSuccessValue);
		} else {
			currentFailureValue += Rnd.get(maxFailureValue / (multi + 1) / 2, maxFailureValue);
		}
		if (currentSuccessValue >= maxFailureValue) {
			currentSuccessValue = maxFailureValue;
		} else if (currentFailureValue >= maxFailureValue) {
			currentFailureValue = maxFailureValue;
		}
	}

	/**
	 * 向客户端发送进度更新。
	 * Sends a progress update packet to the client.
	 */
	protected abstract void sendInteractionUpdate();

	/**
	 * 成功完成时的处理。
	 * Handles successful completion.
	 *
	 * @return true 表示任务应停止 / true if the task should stop
	 */
	protected abstract boolean onSuccessFinish();

	/**
	 * 失败完成时的处理。
	 * Handles failure completion.
	 */
	protected abstract void onFailureFinish();
}
