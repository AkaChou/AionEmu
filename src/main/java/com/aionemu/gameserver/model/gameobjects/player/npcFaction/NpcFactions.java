package com.aionemu.gameserver.model.gameobjects.player.npcFaction;

import java.util.Calendar;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.factions.FactionCategory;
import com.aionemu.gameserver.model.templates.factions.NpcFactionTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestMentorType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TITLE_INFO;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.runtime.PlayerQuestStartEligibilityPort;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * NpcFactions 游戏对象。
 * Npc Factions game object.
 */

public class NpcFactions {
	private Player owner;

	private Map<Integer, NpcFaction> factions = new HashMap<Integer, NpcFaction>();
	private NpcFaction[] activeNpcFaction = new NpcFaction[2];
	private int[] timeLimit = new int[] { 0, 0 };

	public NpcFactions(Player owner) {
		this.owner = owner;
	}

	/** 添加 npc faction / Adds npc faction */
	public void addNpcFaction(NpcFaction faction) {
		factions.put(faction.getId(), faction);
		int type = 0;
		if (faction.isMentor()) {
			type = 1;
		}
		if (faction.isActive()) {
			activeNpcFaction[type] = faction;
		}
		if (timeLimit[type] < faction.getTime() && faction.getState() == ENpcFactionQuestState.COMPLETE) {
			timeLimit[type] = faction.getTime();
		}
	}

	/** 按 ID 返回 npc faction / Returns the npc faction by id */
	public NpcFaction getNpcFactionById(int id) {
		return factions.get(id);
	}

	/** 返回 npc factions / Returns the npc factions */
	public Collection<NpcFaction> getNpcFactions() {
		return factions.values();
	}

	/** 返回当前 NPCfaction / Returns the active npc faction */
	public NpcFaction getActiveNpcFaction(boolean mentor) {
		if (mentor) {
			return activeNpcFaction[1];
		} else {
			return activeNpcFaction[0];
		}
	}

	/** 设置 active / Sets the active */
	public NpcFaction setActive(int npcFactionId) {
		NpcFaction npcFaction = getNpcFactionById(npcFactionId);
		if (npcFaction == null) {
			npcFaction = new NpcFaction(npcFactionId, 0, false, ENpcFactionQuestState.NOTING, 0);
			factions.put(npcFactionId, npcFaction);
		}
		npcFaction.setActive(true);
		if (npcFaction.isMentor()) {
			this.activeNpcFaction[1] = npcFaction;
		} else {
			this.activeNpcFaction[0] = npcFaction;
		}
		return npcFaction;
	}

	/** Leave Npc Faction / Leave Npc Faction */
	public void leaveNpcFaction(Npc npc) {
		int targetObjectId = npc.getObjectId();
		NpcFactionTemplate npcFactionTemplate = DataManager.NPC_FACTIONS_DATA.getNpcFactionByNpcId(npc.getNpcId());
		if (npcFactionTemplate == null) {
			return;
		}
		NpcFaction npcFaction = getNpcFactionById(npcFactionTemplate.getId());
		if (npcFaction == null || !npcFaction.isActive()) {
			PacketSendUtility.sendPacket(owner, new SM_DIALOG_WINDOW(targetObjectId, 1438));
			return;
		}
		PacketSendUtility.sendPacket(owner, new SM_DIALOG_WINDOW(targetObjectId, 1353));
		leaveNpcFaction(npcFaction);
	}

	private void leaveNpcFaction(NpcFaction npcFaction) {
		NpcFactionTemplate npcFactionTemplate = DataManager.NPC_FACTIONS_DATA.getNpcFactionById(npcFaction.getId());
		PacketSendUtility.sendPacket(owner,
				new SM_SYSTEM_MESSAGE(1300526, new DescriptionId(npcFactionTemplate.getNameId())));
		npcFaction.setActive(false);
		activeNpcFaction[npcFactionTemplate.isMentor() ? 1 : 0] = null;
		if (npcFaction.getState() == ENpcFactionQuestState.START) {
			QuestService.abandonQuest(owner, npcFaction.getQuestId());
			npcFaction.setState(ENpcFactionQuestState.NOTING);
		}
	}

	/** 进入势力 / Enter guild. */
	public void enterGuild(Npc npc) {
		int targetObjectId = npc.getObjectId();
		NpcFactionTemplate npcFactionTemplate = DataManager.NPC_FACTIONS_DATA.getNpcFactionByNpcId(npc.getNpcId());
		if (npcFactionTemplate == null) {
			return;
		}
		NpcFaction npcFaction = getNpcFactionById(npcFactionTemplate.getId());
		NpcFaction activeNpcFaction = getActiveNpcFaction(npcFactionTemplate.isMentor());
		int npcFactionId = npcFactionTemplate.getId();
		int skillPoints = npcFactionTemplate.getSkillPoints();
		if (skillPoints != 0) {
			boolean canEnter = false;
			if (npcFactionTemplate.getCategory() == FactionCategory.COMBINESKILL) {
				for (PlayerSkillEntry skill : owner.getSkillList().getAllSkills()) {
					if (CraftSkillUpdateService.isCraftingSkill(skill.getSkillId())
							&& skill.getSkillLevel() >= skillPoints) {
						canEnter = true;
						break;
					}
				}
			}
			if (!canEnter) {
				PacketSendUtility.sendPacket(owner, new SM_DIALOG_WINDOW(targetObjectId, 1098));
				return;
			}
		}
		if (owner.getLevel() < npcFactionTemplate.getMinLevel()
				|| owner.getLevel() > npcFactionTemplate.getMaxLevel()) {
			PacketSendUtility.sendPacket(owner, new SM_DIALOG_WINDOW(targetObjectId, 1182));
			return;
		}
		if (owner.getRace() != npcFactionTemplate.getRace() && !npcFactionTemplate.getRace().equals(Race.NPC)) {
			PacketSendUtility.sendPacket(owner, new SM_DIALOG_WINDOW(targetObjectId, 1097));
			return;
		}
		if (npcFaction != null && npcFaction.isActive()) {
			PacketSendUtility.sendPacket(owner, new SM_SYSTEM_MESSAGE(1300525));
			return;
		}
		if (activeNpcFaction != null && activeNpcFaction.getId() != npcFactionId) {
			askLeaveNpcFaction(npc);
			return;
		}
		if (npcFaction == null || !npcFaction.isActive()) {
			PacketSendUtility.sendPacket(owner,
					new SM_SYSTEM_MESSAGE(1300524, new DescriptionId(npcFactionTemplate.getNameId())));
			PacketSendUtility.sendPacket(owner, new SM_DIALOG_WINDOW(targetObjectId, 1012));
			setActive(npcFactionId);
			sendDailyQuest();
		}
	}

	private void askLeaveNpcFaction(final Npc npc) {
		NpcFactionTemplate npcFactionTemplate = DataManager.NPC_FACTIONS_DATA.getNpcFactionByNpcId(npc.getNpcId());
		final NpcFaction activeNpcFaction = getActiveNpcFaction(npcFactionTemplate.isMentor());
		NpcFactionTemplate activeNpcFactionTemplate = DataManager.NPC_FACTIONS_DATA
				.getNpcFactionById(activeNpcFaction.getId());
		RequestResponseHandler responseHandler = new RequestResponseHandler(owner) {
			/** 接受请求 / Accept Request */
			@Override
			public void acceptRequest(Creature requester, Player responder) {
				leaveNpcFaction(activeNpcFaction);
				enterGuild(npc);
			}

			/** 拒绝请求 / Deny Request */
			@Override
			public void denyRequest(Creature requester, Player responder) {
			}
		};
		boolean requested = owner.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_ASK_JOIN_NEW_FACTION,
				responseHandler);
		if (requested) {
			PacketSendUtility.sendPacket(owner,
					new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_ASK_JOIN_NEW_FACTION, 0, 0,
							new DescriptionId(activeNpcFactionTemplate.getNameId()),
							new DescriptionId(npcFactionTemplate.getNameId())));
		}
		return;
	}

	/** 启动任务。 / Start quest. */
	public void startQuest(QuestTemplate questTemplate) {
		startQuest(questTemplate.getNpcFactionId());
	}

	/** QuestTemplate 退役后的规范元数据生命周期入口。 / Canonical metadata lifecycle entry point after QuestTemplate retirement. */
	public void startQuest(int npcFactionId) {
		NpcFaction npcFaction = factions.get(npcFactionId);
		if (npcFaction == null || !npcFaction.isActive()) {
			return;
		}
		if (npcFaction.getState() != ENpcFactionQuestState.NOTING && npcFaction.getQuestId() == 0) {
			return;
		}
		npcFaction.setState(ENpcFactionQuestState.START);
	}

	/** Abort Quest / Abort Quest */
	public void abortQuest(QuestTemplate questTemplate) {
		abortQuest(questTemplate.getNpcFactionId());
	}

	/** QuestTemplate 退役后的规范元数据生命周期入口。 / Canonical metadata lifecycle entry point after QuestTemplate retirement. */
	public void abortQuest(int npcFactionId) {
		NpcFaction npcFaction = getNpcFactionById(npcFactionId);
		if (npcFaction == null || !npcFaction.isActive()) {
			return;
		}
		npcFaction.setState(ENpcFactionQuestState.NOTING);
		sendDailyQuest();
	}

	/** Complete Quest / Complete Quest */
	public void completeQuest(QuestTemplate questTemplate) {
		completeQuest(questTemplate.getNpcFactionId(), questTemplate.getMentorType() == QuestMentorType.MENTOR);
	}

	/** QuestTemplate 退役后的规范元数据生命周期入口。 / Canonical metadata lifecycle entry point after QuestTemplate retirement. */
	public void completeQuest(int npcFactionId, boolean mentor) {
		NpcFaction npcFaction = getNpcFactionById(npcFactionId);
		if (npcFaction == null || !npcFaction.isActive()) {
			return;
		}
		npcFaction.setTime(getNextTime());
		npcFaction.setState(ENpcFactionQuestState.COMPLETE);
		this.timeLimit[npcFaction.isMentor() ? 1 : 0] = npcFaction.getTime();
		if (mentor) {
			owner.getCommonData().setMentorFlagTime((int) (System.currentTimeMillis() / 1000) + 60 * 60 * 24);
			PacketSendUtility.broadcastPacket(owner, new SM_TITLE_INFO(owner, true), false);
			PacketSendUtility.sendPacket(owner, new SM_TITLE_INFO(true));
		}
	}

	/**
	 * Send daily quest
	 */
	public void sendDailyQuest() {
		for (int i = 0; i < 2; i++) {
			NpcFaction faction = activeNpcFaction[i];
			if (faction == null || !faction.isActive()) {
				continue;
			}
			if (this.timeLimit[i] > System.currentTimeMillis() / 1000) {
				continue;
			}
			int questId = 0;
			switch (faction.getState()) {
			case COMPLETE:
				if (faction.getTime() > System.currentTimeMillis() / 1000) {
					continue;
				}
				break;
			case START:
				continue;
			case NOTING:
				if (faction.getTime() > System.currentTimeMillis() / 1000) {
					questId = faction.getQuestId();
				}
				break;
			}
			if (questId == 0) {
				var questEngine = GameEngineServices.questEngine();
				var catalog = questEngine.questCatalog();
				PlayerQuestStartEligibilityPort eligibility = new PlayerQuestStartEligibilityPort(playerId -> owner,
					id -> catalog.findMetadata(id).orElse(null));
				// 真实按星期位控制势力每日任务发放；当天不可发放的任务不进随机池，空池跳过不发。
				int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
				List<Integer> quests = canonicalDailyQuestCandidates(catalog, faction.getId(),
					questEngine::isHaveHandler,
					id -> isNpcFactionRotationEligible(eligibility, id, faction.getId()),
					id -> DataManager.NPC_FACTIONS_QUEST_DATA.isActiveOn(id, today));
				if (quests.isEmpty()) {
					continue;
				}
				questId = quests.get(Rnd.get(quests.size()));
				faction.setQuestId(questId);
				faction.setTime(getNextTime());
			}
			PacketSendUtility.sendPacket(owner, new SM_QUEST_ACTION(questId, true));
		}
	}

	static List<Integer> canonicalDailyQuestCandidates(QuestCatalog catalog, int npcFactionId,
			IntPredicate hasOwner, IntPredicate eligible, IntPredicate activeToday) {
		return new ArrayList<>(catalog.entries().stream()
			.filter(entry -> entry.metadata().npcFactionId() == npcFactionId)
			.mapToInt(entry -> entry.id())
			.filter(hasOwner)
			.filter(eligible)
			.filter(activeToday)
			.sorted()
			.boxed()
			.toList());
	}

	private boolean isNpcFactionRotationEligible(PlayerQuestStartEligibilityPort eligibility, int questId,
			int npcFactionId) {
		try {
			return eligibility.snapshotNpcFactionRotation(owner.getObjectId(), questId, npcFactionId).eligible();
		} catch (java.sql.SQLException e) {
			return false;
		}
	}

	/** 等级 / On Level Up */
	public void onLevelUp() {
		for (int i = 0; i < 2; i++) {
			NpcFaction faction = activeNpcFaction[i];
			if (faction == null || !faction.isActive()) {
				continue;
			}
			NpcFactionTemplate npcFactionTemplate = DataManager.NPC_FACTIONS_DATA.getNpcFactionById(faction.getId());
			if (npcFactionTemplate.getMaxLevel() < owner.getLevel()) {
				faction.setActive(false);
				activeNpcFaction[i] = null;
				if (faction.getState() == ENpcFactionQuestState.START) {
					QuestService.abandonQuest(owner, faction.getQuestId());
				}
				PacketSendUtility.sendPacket(owner,
						SM_SYSTEM_MESSAGE.STR_FACTION_LEAVE_BY_LEVEL_LIMIT(npcFactionTemplate.getNameId()));
				faction.setState(ENpcFactionQuestState.NOTING);
			}
		}
	}

	private int getNextTime() {
		Calendar repeatDate = Calendar.getInstance();
		repeatDate.set(Calendar.AM_PM, Calendar.AM);
		repeatDate.set(Calendar.HOUR, 9);
		repeatDate.set(Calendar.MINUTE, 0);
		repeatDate.set(Calendar.SECOND, 0);
		if (repeatDate.getTime().getTime() < System.currentTimeMillis()) {
			repeatDate.add(Calendar.HOUR, 24);
		}
		return (int) (repeatDate.getTimeInMillis() / 1000);
	}

	/** 是否开始任务 / Whether start quest*/
	public boolean canStartQuest(QuestTemplate template) {
		return canStartQuest(template.isMentor());
	}

	/** 旧版 QuestTemplate 退役后使用的规范元数据入口。 / Canonical metadata entry point used after legacy QuestTemplate retirement. */
	public boolean canStartQuest(boolean mentor) {
		int type = mentor ? 1 : 0;
		NpcFaction faction = activeNpcFaction[type];
		if (faction != null && this.timeLimit[type] < System.currentTimeMillis() / 1000) {
			return true;
		}
		return false;
	}
}
