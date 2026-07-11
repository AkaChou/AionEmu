package com.aionemu.gameserver.skillengine.task;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.gather.GatherableTemplate;
import com.aionemu.gameserver.model.templates.gather.Material;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GATHER_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GATHER_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 采集任务：对可采集物推进成功/失败进度并发放材料。
 * Gathering task: advances success/failure against a gatherable and awards material.
 */
public class GatheringTask extends AbstractCraftTask {

	/**
	 * 可采集物模板。
	 * Gatherable template.
	 */
	private GatherableTemplate template;

	/**
	 * 目标材料。
	 * Target material.
	 */
	private Material material;

	/**
	 * 构造采集任务。
	 * Creates a gathering task.
	 *
	 * gathering player
	 * gatherable object
	 * target material
	 * @param skillLvlDiff 技能等级差 / skill level difference
	 */
	public GatheringTask(Player requestor, Gatherable gatherable, Material material, int skillLvlDiff) {
		super(requestor, gatherable, skillLvlDiff);
		this.template = gatherable.getObjectTemplate();
		this.material = material;
		this.itemQuality = DataManager.ITEM_DATA.getItemTemplate(this.material.getItemid()).getItemQuality();
		currentSuccessValue = 0;
		currentFailureValue = 0;
		maxSuccessValue = (this.itemQuality.getQualityId() + 1) * 20;
		maxFailureValue = (this.itemQuality.getQualityId() + 1) * 30;
	}

	/**
	 * 中止采集：发送中止更新与状态。
	 * Aborts gathering: sends abort update and status.
	 */
	@Override
	protected void onInteractionAbort() {
		PacketSendUtility.sendPacket(requestor, new SM_GATHER_UPDATE(template, material, 0, 0, 5));
		PacketSendUtility.broadcastPacket(requestor,
				new SM_GATHER_STATUS(requestor.getObjectId(), responder.getObjectId(), 2));
	}

	/**
	 * 交互结束：通知可采集物控制器完成。
	 * Interaction finish: notifies the gatherable controller of completion.
	 */
	@Override
	protected void onInteractionFinish() {
		((Gatherable) responder).getController().completeInteraction();
	}

	/**
	 * 交互开始：发送初始进度与状态。
	 * Interaction start: sends initial progress and status.
	 */
	@Override
	protected void onInteractionStart() {
		PacketSendUtility.sendPacket(requestor,
				new SM_GATHER_UPDATE(template, material, maxSuccessValue, maxFailureValue, 0));
		this.onInteraction();
		PacketSendUtility.broadcastPacket(requestor,
				new SM_GATHER_STATUS(requestor.getObjectId(), responder.getObjectId(), 0), true);
		PacketSendUtility.broadcastPacket(requestor,
				new SM_GATHER_STATUS(requestor.getObjectId(), responder.getObjectId(), 1), true);
	}

	/**
	 * 分析本 tick 暴击与进度增量。
	 * Analyzes this tick's crit and progress increments.
	 */
	@Override
	protected void analyzeInteraction() {
		int critVal = (int) (Rnd.get(55000) / (skillLvlDiff + 1));
		if (critVal < CraftConfig.CRAFT_CHANCE_PURPLE_CRIT) {
			critType = CraftCritType.PURPLE;
			currentSuccessValue = maxSuccessValue;
			return;
		} else if (critVal < CraftConfig.CRAFT_CHANCE_BLUE_CRIT) {
			critType = CraftCritType.BLUE;
		} else if (critVal < CraftConfig.CRAFT_CHANCE_INSTANT) {
			critType = CraftCritType.INSTANT;
			currentSuccessValue = maxSuccessValue;
			return;
		}
		if (CraftConfig.CRAFT_CHECK_TASK) {
			if (this.task == null) {
				return;
			}
		}
		double mod = Math.sqrt((double) skillLvlDiff / 450f) * 100f + Rnd.nextGaussian() * 10f;
		mod -= (double) this.itemQuality.getQualityId();
		if (mod < 0) {
			currentFailureValue -= (int) mod;
		} else {
			currentSuccessValue += (int) mod;
		}
		if (currentSuccessValue >= maxSuccessValue) {
			currentSuccessValue = maxSuccessValue;
		} else if (currentFailureValue >= maxFailureValue) {
			currentFailureValue = maxFailureValue;
		}
	}

	/**
	 * 向客户端发送采集进度更新。
	 * Sends gathering progress update to the client.
	 */
	@Override
	protected void sendInteractionUpdate() {
		PacketSendUtility.sendPacket(requestor, new SM_GATHER_UPDATE(template, material, currentSuccessValue,
				currentFailureValue, this.critType.getPacketId()));
		if (this.critType == CraftCritType.BLUE) {
			this.critType = CraftCritType.NONE;
		}
	}

	/**
	 * 执行一次采集交互 tick。
	 * Performs one gathering interaction tick.
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
	 * 失败完成：发送失败更新与状态。
	 * Failure finish: sends failure update and status.
	 */
	@Override
	protected void onFailureFinish() {
		PacketSendUtility.sendPacket(requestor,
				new SM_GATHER_UPDATE(template, material, currentSuccessValue, currentFailureValue, 1));
		PacketSendUtility.sendPacket(requestor,
				new SM_GATHER_UPDATE(template, material, currentSuccessValue, currentFailureValue, 7));
		PacketSendUtility.broadcastPacket(requestor,
				new SM_GATHER_STATUS(requestor.getObjectId(), responder.getObjectId(), 3), true);
	}

	/**
	 * 成功完成：发放材料、扣消耗物并奖励玩家。
	 * Success finish: awards material, consumes required items, and rewards the player.
	 *
	 * @return 始终 true，表示任务结束 / always true to end the task
	 */
	@Override
	protected boolean onSuccessFinish() {
		PacketSendUtility.sendPacket(requestor,
				SM_SYSTEM_MESSAGE.STR_EXTRACT_GATHER_SUCCESS_1_BASIC(new DescriptionId(material.getNameid())));
		PacketSendUtility.broadcastPacket(requestor,
				new SM_GATHER_STATUS(requestor.getObjectId(), responder.getObjectId(), 2), true);
		PacketSendUtility.sendPacket(requestor,
				new SM_GATHER_UPDATE(template, material, currentSuccessValue, currentFailureValue, 6));
		if (template.getEraseValue() > 0) {
			requestor.getInventory().decreaseByItemId(template.getRequiredItemId(), template.getEraseValue());
		}
		ItemService.addItem(requestor, material.getItemid(), requestor.getRates().getGatheringCountRate());
		if (requestor.isInInstance()) {
			requestor.getPosition().getWorldMapInstance().getInstanceHandler().onGather(requestor,
					(Gatherable) responder);
		}
		((Gatherable) responder).getController().rewardPlayer(requestor);
		return true;
	}
}
