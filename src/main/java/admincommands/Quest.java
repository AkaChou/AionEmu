/*
 * This file is part of Encom.
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package admincommands;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.FinishedQuestCond;
import com.aionemu.gameserver.model.templates.quest.QuestCategory;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.quest.QuestWorkItems;
import com.aionemu.gameserver.model.templates.quest.XMLStartCondition;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.sql.Timestamp;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Quest extends AdminCommand {

    public Quest() {
        super("quest");
    }

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
            QuestTemplate template = DataManager.QUEST_DATA.getQuestById(id);
            List<XMLStartCondition> preconditions = template.getXMLStartConditions();
            if (preconditions != null && preconditions.size() > 0) {
                for (XMLStartCondition condition : preconditions) {
                    List<FinishedQuestCond> finisheds = condition.getFinishedPreconditions();
                    if (finisheds != null && finisheds.size() > 0) {
                        for (FinishedQuestCond fcondition : finisheds) {
                            QuestState qs1 = admin.getQuestStateList().getQuestState(fcondition.getQuestId());
                            if (qs1 == null || qs1.getStatus() != QuestStatus.COMPLETE) {
                                PacketSendUtility.sendMessage(admin, "You have to finish " + fcondition.getQuestId() + " first!");
                            }
                        }
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
            QuestEngine.getInstance().onLvlUp(env);
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
        
        QuestTemplate template = DataManager.QUEST_DATA.getQuestById(questId);
        
        if (template != null) {
            QuestWorkItems qwi = template.getQuestWorkItems();
            if (qwi != null) {
                for (QuestItems qi : qwi.getQuestWorkItem()) {
                    if (qi != null) {
                        long count = target.getInventory().getItemCountByItemId(qi.getItemId());
                        if (count > 0) {
                            target.getInventory().decreaseByItemId(qi.getItemId(), count);
                        }
                    }
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

        ThreadPoolManager.getInstance().schedule(new Runnable() {
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

    @Override
    public void onFail(Player player, String message) {
        PacketSendUtility.sendMessage(player, "syntax //quest <start|set|show|delete>");
    }
}