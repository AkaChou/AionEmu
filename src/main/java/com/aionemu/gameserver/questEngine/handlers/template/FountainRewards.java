package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestActionType;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 喷泉/硬币喷泉奖励任务模板：通过交互物体消耗收集物品并立即进入奖励流程。
 * coin-fountain reward quest template: consumes collected items via object interaction and enters reward immediately.
 */
public class FountainRewards extends QuestHandler {
	/** 任务 ID / quest id */
	private final int questId;
	/** 可交互的起始 NPC/物体 ID 集合 / set of interactable start NPC/object ids */
	private final Set<Integer> startNpcs = new HashSet<Integer>();

	/**
	 * 构造喷泉奖励任务处理器。
	 * Constructs a fountain-rewards quest handler.
	 *
	 * quest id
	 * @param startNpcIds 起始 NPC/物体 ID 列表（含占位 0） / start NPC/object id list (includes placeholder 0)
	 */
	public FountainRewards(int questId, List<Integer> startNpcIds) {
		super(questId);
		this.questId = questId;
		this.startNpcs.addAll(startNpcIds);
		this.startNpcs.remove(0);
	}

	/**
	 * 注册各起始 NPC 的接取与对话事件。
	 * Registers quest-start and talk events for every start NPC.
	 */
	@Override
	public void register() {
		Iterator<Integer> iterator = startNpcs.iterator();
		while (iterator.hasNext()) {
			int startNpc = iterator.next();
			qe.registerQuestNpc(startNpc).addOnQuestStart(getQuestId());
			qe.registerQuestNpc(startNpc).addOnTalkEvent(getQuestId());
		}
	}

	/**
	 * 处理喷泉交互：校验物品与背包，启动任务并直接进入奖励；奖励阶段扣物品或放弃任务。
	 * Handles fountain interaction: validates items and inventory, starts the quest into reward, then deducts items or abandons.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理该对话事件 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (startNpcs.contains(targetId)) {
				switch (dialog) {
				case USE_OBJECT: {
					if (!QuestService.inventoryItemCheck(env, true)) {
						return true;
					} else {
						if (targetId == 806559 || // Reshanta Elyos 5.3
								targetId == 806560 || // Reshanta Asmodians 5.3
								targetId == 701429 || // Oriel Coin Fountain.
								targetId == 701430 || // Pernon Coin Fountain.
								targetId == 804759 || // Enshar Coin Fountain.
								targetId == 804788 || // Cygnea Coin Fountain.
								targetId == 805778 || // Iluma Coin Fountain.
								targetId == 805753) { // Norsvold Coin Fountain.
							return sendQuestDialog(env, 1011);
						} else {
							return sendQuestSelectionDialog(env);
						}
					}
				}
				case STEP_TO_1: {
					if (QuestService.collectItemCheck(env, false)) {
						if (!player.getInventory().isFullSpecialCube()) {
							if (QuestService.startQuest(env)) {
								changeQuestStep(env, 0, 0, true);
								return sendQuestDialog(env, 5);
							}
						} else {
							PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY);
							return sendQuestSelectionDialog(env);
						}
					} else {
						return sendQuestSelectionDialog(env);
					}
				}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (startNpcs.contains(targetId)) {
				if (dialog == QuestDialog.SELECT_NO_REWARD) {
					if (QuestService.collectItemCheck(env, true))
						return sendQuestEndDialog(env);
				} else {
					return QuestService.abandonQuest(player, questId);
				}
			}
		}
		return false;
	}

	/**
	 * 判断目标是否属于本任务可交互物体（喷泉等）。
	 * Returns whether the target is an interactable object for this quest (fountains, etc.).
	 *
	 * @param env 任务环境 / quest environment
	 * @param questEventType 任务动作类型 / quest action type
	 * extra objects
	 *
	 * @return 目标是否可交互 / whether the target may be acted on
	 */
	@Override
	public boolean onCanAct(QuestEnv env, QuestActionType questEventType, Object... objects) {
		if (startNpcs.contains(env.getTargetId())) {
			return true;
		}
		return false;
	}
}
