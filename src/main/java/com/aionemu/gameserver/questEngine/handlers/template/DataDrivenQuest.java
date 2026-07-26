package com.aionemu.gameserver.questEngine.handlers.template;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.models.DataDrivenQuestData;
import com.aionemu.gameserver.questEngine.handlers.models.DataDrivenQuestData.Spawn;
import com.aionemu.gameserver.questEngine.handlers.models.DataDrivenQuestData.Step;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.teleport.TeleportService2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Executes the strict subset generated from retail data_driven_quest.xml. */
public class DataDrivenQuest extends QuestHandler {

	private final DataDrivenQuestData data;
	private final Set<Integer> startIds;
	private final Set<Integer> endNpcIds;
	private final List<Step> steps;

	public DataDrivenQuest(DataDrivenQuestData data) {
		super(data.getId());
		this.data = data;
		startIds = new HashSet<>(data.getStartIds());
		endNpcIds = new HashSet<>(data.getEndNpcIds());
		steps = data.getSteps();
	}

	@Override
	public void register() {
		if ("TALK".equals(data.getStartType())) {
			for (int npcId : startIds) {
				qe.registerQuestNpc(npcId).addOnQuestStart(getQuestId());
				qe.registerQuestNpc(npcId).addOnTalkEvent(getQuestId());
			}
		} else if ("ITEM_PLAY".equals(data.getStartType())) {
			qe.registerQuestItem(data.getStartItemId(), getQuestId());
		} else if ("WORLD_ACTIVE".equals(data.getStartType())) {
			qe.registerOnEnterWorld(getQuestId());
		} else if ("SENSORY_COMPLETE".equals(data.getStartType())) {
			startIds.forEach(id -> qe.registerQuestNpc(id).addOnAddAggroListEvent(getQuestId()));
		} else {
			qe.registerOnEnterWorld(getQuestId());
			qe.registerOnLevelUp(getQuestId());
		}
		for (int npcId : endNpcIds) {
			qe.registerQuestNpc(npcId).addOnTalkEvent(getQuestId());
		}
		if (data.getResetWorldId() != 0) {
			qe.registerOnEnterWorld(getQuestId());
		}
		for (Step step : steps) {
			switch (step.getType()) {
				case "TALK", "COLLECT_ITEM", "ACTION" -> {
					step.getIds().forEach(id -> qe.registerQuestNpc(id).addOnTalkEvent(getQuestId()));
					step.getActionIds().forEach(id -> {
						qe.registerQuestNpc(id).addOnTalkEvent(getQuestId());
						qe.registerCanAct(getQuestId(), id);
					});
				}
				case "HUNT" -> step.getIds().forEach(id -> qe.registerQuestNpc(id).addOnKillEvent(getQuestId()));
				case "ENTER_AREA" -> step.getIds().forEach(id -> qe.registerQuestNpc(id).addOnAddAggroListEvent(getQuestId()));
				case "ENTER_WORLD" -> qe.registerOnEnterWorld(getQuestId());
				case "ITEM_PLAY" -> qe.registerQuestItem(step.getItemId(), getQuestId());
				case "GET_ITEM" -> qe.registerGetingItem(step.getItemId(), getQuestId());
			}
		}
		if (steps.stream().anyMatch(step -> step.getTimerSeconds() > 0)) {
			qe.registerOnQuestTimerEnd(getQuestId());
			qe.registerOnLogOut(getQuestId());
		}
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if ("TALK".equals(data.getStartType()) && startIds.contains(targetId)) {
				if (env.getDialog() == QuestDialog.START_DIALOG || env.getDialog() == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, data.getStartDialogId() == 0 ? 4762 : data.getStartDialogId());
				}
				if (env.getDialog() == QuestDialog.ACCEPT_QUEST || env.getDialog() == QuestDialog.ACCEPT_QUEST_SIMPLE) {
					if (data.getStartRemoveItemId() != 0 && !removeQuestItem(env, data.getStartRemoveItemId(), data.getStartRemoveItemCount())) {
						return false;
					}
					if (data.getStartGiveItemId() != 0 && !giveQuestItem(env, data.getStartGiveItemId(), data.getStartGiveItemCount())) {
						return false;
					}
				}
				boolean started = sendQuestStartDialog(env);
				if (started && data.isCompleteOnStart()) {
					completeOnStart(env);
				}
				return started;
			}
			if ("ITEM_PLAY".equals(data.getStartType()) && targetId == 0
					&& (env.getDialog() == QuestDialog.ACCEPT_QUEST || env.getDialog() == QuestDialog.ACCEPT_QUEST_SIMPLE)) {
				return start(env) && closeDialogWindow(env);
			}
			return false;
		}
		if (qs.getStatus() == QuestStatus.START) {
			int index = qs.getQuestVarById(0);
			if (index < steps.size()) {
				Step step = steps.get(index);
				if (step.getIds().contains(targetId)) {
					if (env.getDialog() == QuestDialog.START_DIALOG || env.getDialog() == QuestDialog.USE_OBJECT) {
						return sendQuestDialog(env, step.getDialogId() == 0 ? 1011 + 341 * index : step.getDialogId());
					}
					if ("TALK".equals(step.getType()) && advances(step, env, index)) {
						if (!advance(env, qs, index)) {
							return false;
						}
						return index + 1 == steps.size() && env.getDialog() == QuestDialog.SELECT_REWARD
								? sendQuestEndDialog(env) : closeDialogWindow(env);
					}
					if ("COLLECT_ITEM".equals(step.getType()) && (env.getDialog() == QuestDialog.CHECK_COLLECTED_ITEMS || env.getDialog() == QuestDialog.CHECK_COLLECTED_ITEMS_SIMPLE)) {
						return checkQuestItems(env, index, index + 1, index + 1 == steps.size(), 10000, 10001);
					}
				}
				if (step.getActionIds().contains(targetId)) {
					if (step.isDeleteActionTarget() && env.getDialogId() == -1 && env.getVisibleObject() != null) {
						env.getVisibleObject().getController().delete();
					}
					if ("ACTION".equals(step.getType())) {
						return advance(env, qs, index);
					}
					return true;
				}
			}
		}
		if (qs.getStatus() == QuestStatus.REWARD && endNpcIds.contains(targetId)) {
			if (env.getDialog() == QuestDialog.START_DIALOG) {
				return sendQuestDialog(env, 10002);
			}
			return sendQuestEndDialog(env);
		}
		return false;
	}

	@Override
	public boolean onKillEvent(QuestEnv env) {
		QuestState qs = activeState(env);
		if (qs == null) {
			return false;
		}
		int index = qs.getQuestVarById(0);
		if (index >= steps.size()) {
			return false;
		}
		Step step = steps.get(index);
		if (!"HUNT".equals(step.getType()) || !step.getIds().contains(env.getTargetId())) {
			return false;
		}
		int count = qs.getQuestVarById(1);
		if (count + 1 >= step.getAmount()) {
			qs.setQuestVarById(1, 0);
			return advance(env, qs, index);
		}
		return defaultOnKillEvent(env, step.getIds().stream().mapToInt(Integer::intValue).toArray(), count, count + 1, 1);
	}

	@Override
	public boolean onAddAggroListEvent(QuestEnv env) {
		if ("SENSORY_COMPLETE".equals(data.getStartType()) && startIds.contains(env.getTargetId())) {
			return completeSensory(env);
		}
		QuestState qs = activeState(env);
		if (qs == null) {
			return false;
		}
		int index = qs.getQuestVarById(0);
		if (index < steps.size() && "ENTER_AREA".equals(steps.get(index).getType()) && steps.get(index).getIds().contains(env.getTargetId())) {
			return advance(env, qs, index);
		}
		return false;
	}

	@Override
	public boolean onGetItemEvent(QuestEnv env) {
		QuestState qs = activeState(env);
		if (qs == null) {
			return false;
		}
		int index = qs.getQuestVarById(0);
		if (index < steps.size() && "GET_ITEM".equals(steps.get(index).getType())) {
			return advance(env, qs, index);
		}
		return false;
	}

	@Override
	public boolean onEnterWorldEvent(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		if ("WORLD_ACTIVE".equals(data.getStartType())) {
			if (env.getPlayer().getWorldId() == data.getWorldId()) {
				return (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) && startWorldActive(env);
			}
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				return stopWorldActive(env);
			}
			return false;
		}
		if (data.getResetWorldId() == env.getPlayer().getWorldId() && qs != null
				&& (qs.getStatus() == QuestStatus.START || qs.getStatus() == QuestStatus.REWARD)
				&& qs.getQuestVarById(0) != 0) {
			qs.setStatus(QuestStatus.START);
			qs.setQuestVarById(0, 0);
			updateQuestStatus(env);
			return true;
		}
		if ((qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) && "ENTER_AREA".equals(data.getStartType())) {
			return start(env);
		}
		qs = activeState(env);
		if (qs == null) {
			return false;
		}
		int index = qs.getQuestVarById(0);
		if (index < steps.size() && "ENTER_WORLD".equals(steps.get(index).getType()) && steps.get(index).getWorldId() == env.getPlayer().getWorldId()) {
			return advance(env, qs, index);
		}
		return false;
	}

	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		return onEnterWorldEvent(env);
	}

	@Override
	public boolean onQuestTimerEndEvent(QuestEnv env) {
		return resetTimerProgress(env);
	}

	@Override
	public boolean onLogOutEvent(QuestEnv env) {
		return resetTimerProgress(env);
	}

	protected boolean startWorldActive(QuestEnv env) {
		return QuestService.startQuest(env);
	}

	protected boolean stopWorldActive(QuestEnv env) {
		return QuestService.abandonQuest(env.getPlayer(), getQuestId());
	}

	protected boolean completeSensory(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		if ((qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) && !QuestService.startQuest(env)) {
			return false;
		}
		qs = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		if (qs.getStatus() == QuestStatus.START) {
			qs.setStatus(QuestStatus.REWARD);
		}
		return qs.getStatus() == QuestStatus.REWARD && QuestService.finishQuest(env);
	}

	private void completeOnStart(QuestEnv env) {
		QuestState qs = activeState(env);
		if (qs != null) {
			qs.setStatus(QuestStatus.REWARD);
			updateQuestStatus(env);
		}
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		int itemId = item.getItemTemplate().getTemplateId();
		if ((qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) && "ITEM_PLAY".equals(data.getStartType()) && itemId == data.getStartItemId()) {
			return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
		}
		qs = activeState(env);
		if (qs == null) {
			return HandlerResult.UNKNOWN;
		}
		int index = qs.getQuestVarById(0);
		if (index < steps.size() && "ITEM_PLAY".equals(steps.get(index).getType()) && steps.get(index).getItemId() == itemId) {
			return HandlerResult.fromBoolean(advance(env, qs, index));
		}
		return HandlerResult.UNKNOWN;
	}

	private boolean start(QuestEnv env) {
		if (!QuestService.startQuest(env)) {
			return false;
		}
		if (data.getStartGiveItemId() != 0) {
			giveQuestItem(env, data.getStartGiveItemId(), data.getStartGiveItemCount());
		}
		if (data.getStartRemoveItemId() != 0) {
			removeQuestItem(env, data.getStartRemoveItemId(), data.getStartRemoveItemCount());
		}
		if (steps.isEmpty()) {
			QuestState qs = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
			qs.setStatus(QuestStatus.REWARD);
			updateQuestStatus(env);
		}
		return true;
	}

	private QuestState activeState(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		return qs != null && qs.getStatus() == QuestStatus.START ? qs : null;
	}

	private boolean applyItemChanges(QuestEnv env, Step step) {
		if (step.getRemoveItemId() != 0 && !removeQuestItem(env, step.getRemoveItemId(), step.getRemoveItemCount())) {
			return false;
		}
		return step.getGiveItemId() == 0 || giveQuestItem(env, step.getGiveItemId(), step.getGiveItemCount());
	}

	private boolean advances(Step step, QuestEnv env, int index) {
		return step.getAdvanceDialogId() == 0
				? env.getDialogId() == 10000 + index || env.getDialog() == QuestDialog.SET_REWARD
				: env.getDialogId() == step.getAdvanceDialogId();
	}

	private boolean advance(QuestEnv env, QuestState qs, int index) {
		Step step = steps.get(index);
		if (!applyItemChanges(env, step)) {
			return false;
		}
		if (step.getTeleportWorldId() != 0 && !teleport(env, step)) {
			return false;
		}
		int movie = step.getMovie() == 0 && "TALK".equals(step.getType()) ? data.getQuestMovie() : step.getMovie();
		if (movie != 0) {
			playQuestMovie(env, movie);
		}
		for (Spawn spawn : step.getSpawns()) {
			for (int i = 0; i < spawn.getCount(); i++) {
				spawn(env, spawn);
			}
		}
		qs.setQuestVarById(0, index + 1);
		if (index + 1 == steps.size()) {
			qs.setStatus(QuestStatus.REWARD);
		}
		updateQuestStatus(env);
		if (step.getTimerSeconds() > 0) {
			startTimer(env, step.getTimerSeconds());
		}
		return true;
	}

	private boolean resetTimerProgress(QuestEnv env) {
		QuestState qs = activeState(env);
		if (qs == null) {
			return false;
		}
		int current = qs.getQuestVarById(0);
		for (int source = 0; source < steps.size(); source++) {
			Step step = steps.get(source);
			if (step.getTimerSeconds() > 0 && source < current && current < step.getTimerDestinationProgress()) {
				qs.setQuestVarById(0, source);
				qs.setQuestVarById(1, 0);
				updateQuestStatus(env);
				return true;
			}
		}
		return false;
	}

	protected void startTimer(QuestEnv env, int seconds) {
		QuestService.questTimerStart(env, seconds);
	}

	protected boolean teleport(QuestEnv env, Step step) {
		return TeleportService2.teleportTo(env.getPlayer(), step.getTeleportWorldId(), (float) step.getTeleportX(),
				step.getTeleportY(), step.getTeleportZ(), (byte) step.getTeleportHeading());
	}

	protected void spawn(QuestEnv env, Spawn spawn) {
		if (spawn.isRelative()) {
			QuestService.addNewSpawnForSeconds(env.getPlayer(), spawn.getNpcId(), spawn.getLifetimeSeconds());
		} else {
			QuestService.addNewSpawnForSeconds(env.getPlayer().getWorldId(), env.getPlayer().getInstanceId(), spawn.getNpcId(),
					spawn.getX(), spawn.getY(), spawn.getZ(), (byte) spawn.getHeading(), spawn.getLifetimeSeconds());
		}
	}

}
