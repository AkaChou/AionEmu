package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.Iterator;
import java.util.List;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.models.WorkOrdersData;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.RecipeService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 制作工单任务模板：接取后发放材料与配方，收集成品后交任务并删除配方。
 * Crafting work-order quest template: grants materials and recipe on accept, turns in finished goods and removes the recipe.
 */
public class WorkOrders extends QuestHandler {
	/** 工单 XML 配置数据 / work-order XML config data */
	private final WorkOrdersData workOrdersData;
	private final QuestMetadata metadata;

	/**
	 * 构造制作工单任务处理器。
	 * Constructs a work-orders quest handler.
	 *
	 * work-order config
	 */
	public WorkOrders(WorkOrdersData workOrdersData) {
		super(workOrdersData.getId());
		this.workOrdersData = workOrdersData;
		this.metadata = GameEngineServices.questEngine().questCatalog().findMetadata(workOrdersData.getId())
			.orElseThrow(() -> new IllegalStateException(
				"missing canonical metadata for legacy work-order quest " + workOrdersData.getId()));
	}

	/**
	 * 注册工单起始 NPC 的接取与对话事件。
	 * Registers quest-start and talk events for work-order start NPCs.
	 */
	@Override
	public void register() {
		Iterator<Integer> iterator = workOrdersData.getStartNpcIds().iterator();
		while (iterator.hasNext()) {
			int startNpc = iterator.next();
			qe.registerQuestNpc(startNpc).addOnQuestStart(workOrdersData.getId());
			qe.registerQuestNpc(startNpc).addOnTalkEvent(workOrdersData.getId());
		}
	}

	/**
	 * 处理接取（发材料/配方）、交成品与奖励对话；领奖时清理收集物与配方。
	 * Handles accept (materials/recipe), finished-goods turn-in and reward dialogs; clears collect items and recipe on reward.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		if (workOrdersData.getStartNpcIds().contains(targetId)) {
			QuestState qs = player.getQuestStateList().getQuestState(workOrdersData.getId());
			if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat(metadata)) {
				switch (env.getDialog()) {
				case START_DIALOG: {
					return sendQuestDialog(env, 4);
				}
				case ACCEPT_QUEST: {
					if (RecipeService.validateNewRecipe(player, workOrdersData.getRecipeId()) != null) {
						if (QuestService.startQuest(env)) {
							if (ItemService.addQuestItems(player, workOrdersData.getGiveComponent())) {
								RecipeService.addRecipe(player, workOrdersData.getRecipeId(), false);
								PacketSendUtility.sendPacket(player,
										new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 0));
							}
							return true;
						}
					}
				}
				}
			} else if (qs.getStatus() == QuestStatus.START) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					int var = qs.getQuestVarById(0);
					if (QuestService.collectItemCheck(env, false)) {
						changeQuestStep(env, var, var, true);
						for (var workItem : metadata.questWorkItems()) {
							long count = player.getInventory().getItemCountByItemId(workItem.itemId());
							if (count > 0) {
								player.getInventory().decreaseByItemId(workItem.itemId(), count);
							}
						}
						return sendQuestDialog(env, 5);
					} else {
						return sendQuestSelectionDialog(env);
					}
				}
			} else if (qs.getStatus() == QuestStatus.REWARD) {
				for (var collectItem : metadata.itemRequirements()) {
					long count = player.getInventory().getItemCountByItemId(collectItem.itemId());
					if (count > 0) {
						player.getInventory().decreaseByItemId(collectItem.itemId(), count);
					}
				}
				player.getRecipeList().deleteRecipe(player, workOrdersData.getRecipeId());
				if (env.getDialogId() == -1) {
					QuestService.finishQuest(env, 0);
					env.setQuestId(workOrdersData.getId());
					return sendQuestDialog(env, 1008);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}

	@Override
	public List<Integer> getQuestOwnedRecipeIds() {
		return List.of(workOrdersData.getRecipeId());
	}
}
