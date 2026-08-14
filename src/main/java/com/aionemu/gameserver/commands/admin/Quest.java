package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对目标玩家启动、设置、查看或删除任务状态的管理员命令。
 * Admin command to start, set, show or delete quest state on a targeted player.
 */
public class Quest extends AdminCommand {

    /**
     * 以别名 {@code quest} 构造命令。
     * Construct the command with alias {@code quest}.
     */
    public Quest() {
        super("quest");
    }

    /**
     * 分发 start/set/delete/show 子命令；目标必须为玩家。
     * Dispatch start/set/delete/show; the target must be a player.
     *
     * @param params 子命令与任务参数 / Subcommand and quest args
     */
    @Override
    public void execute(Player admin, String... params) {
        if (params == null || params.length < 1) {
            PacketSendUtility.sendMessage(admin, "syntax //quest <start|set|show|delete>");
            return;
        }

        Player target = null;
        VisibleObject creature = admin.getTarget();
        if (admin.getTarget() instanceof Player) {
            target = (Player) creature;
        }

        if (target == null) {
            PacketSendUtility.sendMessage(admin, "Incorrect target!");
            return;
        }

        if (params[0].equals("start")) {
            handleStart(admin, target, params);
        }
        else if (params[0].equals("set")) {
            handleSet(admin, target, params);
        }
        else if (params[0].equals("delete")) {
            handleDelete(admin, target, params);
        }
        else if (params[0].equals("show")) {
            handleShow(admin, target, params);
        }
        else {
            PacketSendUtility.sendMessage(admin, "syntax //quest <start|set|show|delete>");
        }
    }

    private void handleStart(Player admin, Player target, String... params) {
        if (params.length != 2) {
            PacketSendUtility.sendMessage(admin, "syntax //quest start <questId>");
            return;
        }

        int id;
        try {
            String quest = params[1];
            Pattern questId = Pattern.compile("\\[quest:([^%]+)]");
            Matcher result = questId.matcher(quest);
            if (result.find())
                id = Integer.parseInt(result.group(1));
            else
                id = Integer.parseInt(params[1]);
        }
        catch (NumberFormatException e) {
            PacketSendUtility.sendMessage(admin, "syntax //quest start <questId>");
            return;
        }

        QuestEnv env = new QuestEnv(null, target, id, 0);

        if (QuestService.startQuest(env)) {
            PacketSendUtility.sendMessage(admin, "Quest started.");
        }
        else {
            var metadata = GameEngineServices.questEngine().questCatalog().findMetadata(id).orElse(null);
            if (metadata != null) {
                // 备选起始条件组是 OR 分支，只有全局必填的前置条件才能单独上报，避免产生误诊断。
                // Alternative start-condition groups are OR branches. Only globally mandatory
                // prerequisites can be reported individually without producing false diagnostics.
                for (int prerequisite : metadata.prerequisites()) {
                    QuestState state = target.getQuestStateList().getQuestState(prerequisite);
                    if (state == null || state.getStatus() != QuestStatus.COMPLETE) {
                        PacketSendUtility.sendMessage(admin, "You have to finish " + prerequisite + " first!");
                    }
                }
            }
            PacketSendUtility.sendMessage(admin, "Quest not started. Some preconditions failed");
        }
    }

    private void handleSet(Player admin, Player target, String... params) {
        int questId, var;
        int varNum = 0;
        QuestStatus questStatus;

        try {
            String quest = params[1];
            Pattern id = Pattern.compile("\\[quest:([^%]+)]");
            Matcher result = id.matcher(quest);
            if (result.find())
                questId = Integer.parseInt(result.group(1));
            else
                questId = Integer.parseInt(params[1]);

            String statusValue = params[2];
            if ("START".equals(statusValue)) {
                questStatus = QuestStatus.START;
            }
            else if ("NONE".equals(statusValue)) {
                questStatus = QuestStatus.NONE;
            }
            else if ("COMPLETE".equals(statusValue)) {
                questStatus = QuestStatus.COMPLETE;
            }
            else if ("REWARD".equals(statusValue)) {
                questStatus = QuestStatus.REWARD;
            }
            else {
                PacketSendUtility.sendMessage(admin, "<status is one of START, NONE, REWARD, COMPLETE>");
                return;
            }
            var = Integer.valueOf(params[3]);
            if (params.length == 5 && params[4] != null && !params[4].isEmpty()) {
                varNum = Integer.valueOf(params[4]);
            }
        }
        catch (NumberFormatException e) {
            PacketSendUtility.sendMessage(admin, "syntax //quest set <questId status var [varNum]>");
            return;
        }

        QuestState qs = target.getQuestStateList().getQuestState(questId);
        if (qs == null) {
            qs = new QuestState(questId, questStatus, 0, 0, new Timestamp(0), 0, new Timestamp(0));
            target.getQuestStateList().addQuest(questId, qs);
            PacketSendUtility.sendMessage(admin, "<QuestState has been newly initialized.>");
        }

        qs.setStatus(questStatus);
        if (varNum != 0) {
            qs.setQuestVarById(varNum, var);
        }
        else {
            qs.setQuestVar(var);
        }

        PacketSendUtility.sendPacket(target, new SM_QUEST_ACTION(questId, qs.getStatus(), qs.getQuestVars().getQuestVars()));

        QuestEnv env = new QuestEnv(null, target, questId, 0);
        if (questStatus == QuestStatus.COMPLETE) {
            GameEngineServices.questEngine().onLvlUp(env);
            target.getController().updateNearbyQuests();
            qs.setCompleteCount(qs.getCompleteCount() + 1);
            PacketSendUtility.sendPacket(target, new SM_QUEST_COMPLETED_LIST(target.getQuestStateList().getAllFinishedQuests()));
        }

        target.getController().updateZone();
        target.getController().updateNearbyQuests();

        PacketSendUtility.sendMessage(admin, "Quest status updated successfully.");
    }

    private void handleDelete(Player admin, Player target, String... params) {
        if (params.length != 2) {
            PacketSendUtility.sendMessage(admin, "syntax //quest delete <quest id>");
            return;
        }

        int questId;
        try {
            questId = Integer.valueOf(params[1]);
        }
        catch (NumberFormatException e) {
            PacketSendUtility.sendMessage(admin, "syntax //quest delete <quest id>");
            return;
        }

        QuestStateList list = target.getQuestStateList();
        QuestState qs = list.getQuestState(questId);

        if (qs == null) {
            PacketSendUtility.sendMessage(admin, "Quest not found.");
            return;
        }

        var metadata = GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null);

        if (metadata != null) {
            for (var workItem : metadata.questWorkItems()) {
                long count = target.getInventory().getItemCountByItemId(workItem.itemId());
                if (count > 0) {
                    target.getInventory().decreaseByItemId(workItem.itemId(), count);
                }
            }
        }

        if (target.getController().getTask(TaskId.QUEST_TIMER) != null) {
            QuestService.questTimerEnd(new QuestEnv(null, target, questId, 0));
        }

        if (qs.getPersistentState() == PersistentState.NEW) {
            qs.setStatus(QuestStatus.NONE);
            qs.setQuestVar(0);
            qs.setCompleteCount(0);
        } else {
            qs.setQuestVar(0);
            qs.setCompleteCount(0);
            qs.setStatus(QuestStatus.NONE);
            qs.setPersistentState(PersistentState.DELETED);
        }

        PacketSendUtility.sendPacket(target, new SM_QUEST_ACTION(questId));

        PacketSendUtility.sendPacket(target, new SM_QUEST_ACTION(questId, QuestStatus.NONE, 0));

        PacketSendUtility.sendPacket(target, new SM_QUEST_COMPLETED_LIST(target.getQuestStateList().getAllFinishedQuests()));

        target.getController().updateZone();
        target.getController().updateNearbyQuests();

        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                if (target.isOnline()) {
                    target.getController().updateNearbyQuests();
                }
            }
        }, 1000);

        PacketSendUtility.sendMessage(admin, "Quest " + questId + " deleted successfully for " + target.getName());
    }

    private void handleShow(Player admin, Player target, String... params) {
        if (params.length != 2) {
            PacketSendUtility.sendMessage(admin, "syntax //quest show <quest id>");
            return;
        }

        int questId;
        try {
            questId = Integer.valueOf(params[1]);
        }
        catch (NumberFormatException e) {
            PacketSendUtility.sendMessage(admin, "syntax //quest show <quest id>");
            return;
        }

        QuestState qs = target.getQuestStateList().getQuestState(questId);
        if (qs == null) {
            PacketSendUtility.sendMessage(admin, "Quest state: NULL");
        }
        else {
            StringBuilder sb = new StringBuilder();
            sb.append("Quest ID: ").append(questId).append("\n");
            sb.append("Status: ").append(qs.getStatus().toString()).append("\n");
            sb.append("Vars: ");
            for (int i = 0; i < 5; i++) {
                sb.append(qs.getQuestVarById(i)).append(" ");
            }
            sb.append(qs.getQuestVarById(5)).append("\n");
            sb.append("Complete count: ").append(qs.getCompleteCount());

            PacketSendUtility.sendMessage(admin, sb.toString());
        }
    }

    /**
     * 参数错误时显示语法。
     * Show syntax when parameters are invalid.
     *
     */
    @Override
    public void onFail(Player player, String message) {
        PacketSendUtility.sendMessage(player, "syntax //quest <start|set|show|delete>");
    }
}
