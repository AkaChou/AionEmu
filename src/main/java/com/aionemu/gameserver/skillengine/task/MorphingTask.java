package com.aionemu.gameserver.skillengine.task;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.StaticObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CRAFT_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CRAFT_UPDATE;
import com.aionemu.gameserver.services.craft.CraftService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 变形任务：简化版制作，进度满后立即成功结算。
 * Morphing task: simplified crafting that succeeds immediately once progress fills.
 */
public class MorphingTask extends CraftingTask {

	/**
	 * 构造单次变形任务。
	 * Creates a single morphing task.
	 *
	 * requesting player
	 * responder
	 * recipe template
	 */
	public MorphingTask(Player requestor, StaticObject responder, RecipeTemplate recipeTemplates) {
		this(requestor, responder, recipeTemplates, 1);
	}

	/**
	 * 构造可批量变形的任务。
	 * Creates a morphing task with optional multi-craft count.
	 *
	 * requesting player
	 * responder
	 * recipe template
	 * morph attempt count
	 */
	public MorphingTask(Player requestor, StaticObject responder, RecipeTemplate recipeTemplates, int craftCount) {
		super(requestor, responder, recipeTemplates, 0, 0, craftCount);
		this.maxSuccessValue = 100;
		this.maxFailureValue = 100;
	}

	/**
	 * 启动变形任务（更短的 tick 间隔）。
	 * Starts the morphing task with a shorter tick interval.
	 */
	@Override
	public void start() {
		onInteractionStart();
		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (!validateParticipants()) {
					stop(true);
				}
				boolean stopTask = onInteraction();
				if (stopTask) {
					stop(false);
				}
			}
		}, 1000, 1500);
	}

	/**
	 * 变形不分析进度，直接成功。
	 * Morphing skips progress analysis and succeeds directly.
	 */
	@Override
	protected void analyzeInteraction() {
	}

	/**
	 * 失败完成：发送失败更新与动画。
	 * Failure finish: sends failure update and animation.
	 */
	@Override
	protected void onFailureFinish() {
		PacketSendUtility.sendPacket(requestor, new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate,
				currentSuccessValue, currentFailureValue, 6));
		PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), 0, 0, 3), true);
	}

	/**
	 * 成功完成：结算产物并处理批量次数。
	 * Success finish: settles product and handles multi-craft count.
	 *
	 * @return true 表示全部次数完成 / true if all attempts are done
	 */
	@Override
	protected boolean onSuccessFinish() {
		PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), 0, 0, 2), true);
		PacketSendUtility.sendPacket(requestor,
				new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, 0, 0, 5));
		CraftService.finishCrafting(requestor, recipeTemplate, critCount, 0);
		return finishCraftAttempt();
	}

	/**
	 * 开始下一次批量变形。
	 * Starts the next multi-morph attempt.
	 */
	@Override
	protected void startNextCraft() {
		currentSuccessValue = 0;
		currentFailureValue = 0;
		PacketSendUtility.sendPacket(requestor,
				new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, maxSuccessValue, maxFailureValue, 0));
		PacketSendUtility.sendPacket(requestor,
				new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, 0, 0, 1));
		PacketSendUtility.broadcastPacket(requestor,
				new SM_CRAFT_ANIMATION(requestor.getObjectId(), 0, recipeTemplate.getSkillid(), 0), true);
		PacketSendUtility.broadcastPacket(requestor,
				new SM_CRAFT_ANIMATION(requestor.getObjectId(), 0, recipeTemplate.getSkillid(), 1), true);
	}

	/**
	 * 交互结束：清理玩家制作任务引用。
	 * Interaction finish: clears the player's craft task reference.
	 */
	@Override
	protected void onInteractionFinish() {
		requestor.setCraftingTask(null);
	}

	/**
	 * 一次交互即视为成功。
	 * Treats one interaction as immediate success.
	 *
	 * @return 成功结算结果 / result of success finish
	 */
	@Override
	protected boolean onInteraction() {
		currentSuccessValue = maxSuccessValue;
		return onSuccessFinish();
	}

	/**
	 * 交互开始：加载产物并发送动画。
	 * Interaction start: loads product and sends animation.
	 */
	@Override
	protected void onInteractionStart() {
		this.itemTemplate = DataManager.ITEM_DATA.getItemTemplate(recipeTemplate.getProductid());
		PacketSendUtility.sendPacket(requestor,
				new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, maxSuccessValue, maxFailureValue, 0));
		PacketSendUtility.sendPacket(requestor,
				new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, 0, 0, 1));
		PacketSendUtility.broadcastPacket(requestor,
				new SM_CRAFT_ANIMATION(requestor.getObjectId(), 0, recipeTemplate.getSkillid(), 0), true);
		PacketSendUtility.broadcastPacket(requestor,
				new SM_CRAFT_ANIMATION(requestor.getObjectId(), 0, recipeTemplate.getSkillid(), 1), true);
	}

	/**
	 * 中止变形：发包并清理任务。
	 * Aborts morphing: packets and clears the task.
	 */
	@Override
	protected void onInteractionAbort() {
		PacketSendUtility.sendPacket(requestor,
				new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, 0, 0, 4));
		PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), 0, 0, 2), true);
		requestor.setCraftingTask(null);
	}
}
