package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 遗物兑换奖励任务模板：≥45 级时用四类遗物物品兑换对应奖励页。
 * Relic-exchange reward quest template: at level ≥45, exchanges one of four relic item types for matching reward pages.
 */
public class RelicRewards extends QuestHandler {
	/** 任务 ID / quest id */
	private final int questId;
	/** 起始/兑换 NPC ID 集合 / start/exchange NPC id set */
	private final Set<Integer> startNpcs = new HashSet<Integer>();
	/** 遗物物品 ID 1 / relic item id 1 */
	private final int relicVar1;
	/** 遗物物品 ID 2 / relic item id 2 */
	private final int relicVar2;
	/** 遗物物品 ID 3 / relic item id 3 */
	private final int relicVar3;
	/** 遗物物品 ID 4 / relic item id 4 */
	private final int relicVar4;
	/** 每次兑换消耗的遗物数量 / relic count consumed per exchange */
	private int relicCount;

	/**
	 * 构造遗物兑换奖励任务处理器。
	 * Constructs a relic-rewards quest handler.
	 *
	 * quest id
	 * start NPC list
	 * relic item 1
	 * relic item 2
	 * relic item 3
	 * relic item 4
	 * @param relicCount 每次消耗数量 / count per exchange
	 */
	public RelicRewards(int questId, List<Integer> startNpcIds, int relicVar1, int relicVar2, int relicVar3,
			int relicVar4, int relicCount) {
		super(questId);
		this.startNpcs.addAll(startNpcIds);
		this.startNpcs.remove(0);
		this.questId = questId;
		this.relicVar1 = relicVar1;
		this.relicVar2 = relicVar2;
		this.relicVar3 = relicVar3;
		this.relicVar4 = relicVar4;
		this.relicCount = relicCount;
	}

	/**
	 * 注册兑换 NPC 的接取与对话事件。
	 * Registers quest-start and talk events for exchange NPCs.
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
	 * 处理遗物兑换对话：校验等级与背包，扣除对应遗物并进入对应奖励页。
	 * Handles relic-exchange dialogs: checks level/inventory, removes the chosen relic and opens the matching reward page.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (startNpcs.contains(targetId)) {
				switch (env.getDialog()) {
				case EXCHANGE_COIN: {
					if (player.getCommonData().getLevel() >= 45) {
						if ((player.getInventory().getItemCountByItemId(relicVar1) > 0)
								|| (player.getInventory().getItemCountByItemId(relicVar2) > 0)
								|| (player.getInventory().getItemCountByItemId(relicVar3) > 0)
								|| (player.getInventory().getItemCountByItemId(relicVar4) > 0)) {
							QuestService.startQuest(env);
							return sendQuestDialog(env, 1011);
						} else {
							return sendQuestDialog(env, 3398);
						}
					} else {
						return sendQuestDialog(env, 3398);
					}
				}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
			if (startNpcs.contains(targetId)) {
				if (relicCount == 0) {
					relicCount = 1;
				}
				switch (env.getDialog()) {
				case USE_OBJECT:
					return sendQuestDialog(env, 1011);
				case SELECT_ACTION_1011:
					if (player.getInventory().getItemCountByItemId(relicVar1) >= relicCount) {
						removeQuestItem(env, relicVar1, relicCount);
						qs.setQuestVar(1);
						qs.setStatus(QuestStatus.REWARD);
						qs.setCompleteCount(0);
						updateQuestStatus(env);
						return sendQuestDialog(env, 5);
					} else {
						return sendQuestDialog(env, 1009);
					}
				case SELECT_ACTION_1352:
					if (player.getInventory().getItemCountByItemId(relicVar2) >= relicCount) {
						removeQuestItem(env, relicVar2, relicCount);
						qs.setQuestVar(2);
						qs.setStatus(QuestStatus.REWARD);
						qs.setCompleteCount(0);
						updateQuestStatus(env);
						return sendQuestDialog(env, 6);
					} else {
						return sendQuestDialog(env, 1009);
					}
				case SELECT_ACTION_1693:
					if (player.getInventory().getItemCountByItemId(relicVar3) >= relicCount) {
						removeQuestItem(env, relicVar3, relicCount);
						qs.setQuestVar(3);
						qs.setStatus(QuestStatus.REWARD);
						qs.setCompleteCount(0);
						updateQuestStatus(env);
						return sendQuestDialog(env, 7);
					} else {
						return sendQuestDialog(env, 1009);
					}
				case SELECT_ACTION_2034:
					if (player.getInventory().getItemCountByItemId(relicVar4) >= relicCount) {
						removeQuestItem(env, relicVar4, relicCount);
						qs.setQuestVar(4);
						qs.setStatus(QuestStatus.REWARD);
						qs.setCompleteCount(0);
						updateQuestStatus(env);
						return sendQuestDialog(env, 8);
					} else {
						return sendQuestDialog(env, 1009);
					}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (startNpcs.contains(targetId)) {
				int var = qs.getQuestVarById(0);
				switch (env.getDialog()) {
				case USE_OBJECT:
					if (var == 1) {
						return sendQuestDialog(env, 5);
					} else if (var == 2) {
						return sendQuestDialog(env, 6);
					} else if (var == 3) {
						return sendQuestDialog(env, 7);
					} else if (var == 4) {
						return sendQuestDialog(env, 8);
					}
				case SELECT_NO_REWARD:
					QuestService.finishQuest(env, qs.getQuestVars().getQuestVars() - 1);
					PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
					return true;
				}
			}
		}
		return false;
	}
}
