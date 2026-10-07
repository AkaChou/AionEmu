package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.lifecycle.GameEngineServices;


import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.autogroup.AutoGroupType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.teleport.PortalService;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.List;

/**
 * 传送门/传送点 AI：Portal Dialog（@AIName "portal_dialog"），继承 PortalAI2。
 * Portal/teleporter AI: Portal Dialog (@AIName "portal_dialog"), extends PortalAI2.
 * @author Encom
 */
@AIName("portal_dialog")
public class PortalDialogAI2 extends PortalAI2 {

	private static final int FISSURE_OF_OBLIVION_EXIT = 834194;
	private static final int FISSURE_OF_OBLIVION_FINAL_ORB_ENTITY = 29;
	/** 克萝梅德试炼（噩梦副本）世界。 / Kromede's Trial (nightmare instance) world. */
	private static final int KROMEDE_TRIAL_WORLD_ID = 300230000;
	/** 噩梦副本入口：天族兰尼尼亚。 / Kromede's Trial entrance (Elyos): Raninia. */
	private static final int KROMEDE_TRIAL_ENTRY_ELYOS = 205229;
	/** 噩梦副本入口：魔族布里奇特。 / Kromede's Trial entrance (Asmodians): Bridget. */
	private static final int KROMEDE_TRIAL_ENTRY_ASMODIANS = 205234;

	protected int rewardDialogId = 5;
	protected int startingDialogId = 10;
	protected int questDialogId = 10;

	@Override
	protected void handleDialogStart(Player player) {
		if (getTalkDelay() == 0) {
			checkDialog(player);
		} else {
			super.handleDialogStart(player);
		}
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		switch (getNpcId()) {
			case 730399: // 伦图斯基地。 / Rentus Base.
			case 731549: // [被占领的] 符文安息处。 / [Seized] Danuar Sanctuary.
			case 731570: // 符文安息处。 / Danuar Sanctuary.
			case 832991: // 被占领的伦图斯基地 [天族]。 / Occupied Rentus Base [Elyos].
			case 832992: // 被占领的伦图斯基地 [魔族]。 / Occupied Rentus Base [Asmodians].
			case 832995: // 提亚马特要塞 [天族]。 / Tiamat Stronghold [Elyos].
			case 832996: // 提亚马特要塞 [魔族]。 / Tiamat Stronghold [Asmodians].
			case 832997: // [痛苦] 龙主避难所。 / [Anguished] Dragon Lord Refuge.
			case 832998: // 龙主避难所。 / Dragon Lord Refuge.
/* 				startLifeTask(); */
			break;
			case 730883: // [炼狱] 光明方尖碑。 / [Infernal] Illuminary Obelisk.
			    announceIlluminaryObeliskOpen();
			break;
        }
	}

/* 	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(PortalDialogAI2.this);
			}
		}, 120000); //2 Minutes.
	} */

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		if (questId > 0 && GameEngineServices.questEngine().onDialog(env)) {
			return true;
		}
		// 克萝梅德试炼入口（兰尼尼亚/布里奇特）：questId=0 的「进入恶梦」(SETPRO1=10000) 在真端由任务脚本
		// 处理（fun_893.cpp FUN_180f859b0：关窗 + 18602/28602 步=1 + 进副本），而非传送门。这里先让任务引擎
		// 按该 NPC 的进行中任务重放（QuestEngine.onDialog 的 requestedOwner==0 候选分发）；引擎认领即推进任务
		// 步并走任务侧传送（与 portal_loc 3002300 同坐标）。不认领（未接/步骤已过）时保持既有传送门行为。
		// Kromede's Trial entrance (Raninia/Bridget): the questId=0 "enter the nightmare" (SETPRO1=10000) is
		// served by the quest script in retail (step:=1 + enter), not by the portal. Route it through the quest
		// engine first (QuestEngine.onDialog's requestedOwner==0 replay); the portal stays for everything else.
		if (questId == 0 && dialogId == QuestDialogAction.SETPRO1.id()
				&& isKromedeTrialEntryNpc(getNpcId())) {
			if (GameEngineServices.questEngine().onDialog(env)) {
				return true;
			}
			// 已在噩梦副本内（含同一包内任务侧传送后的重复派发）不再走传送门，避免二次传送。
			// Already inside the nightmare instance (incl. a same-packet replay after the quest-side
			// teleport): do not run the portal again.
			if (player.getWorldId() == KROMEDE_TRIAL_WORLD_ID) {
				return true;
			}
		}
		if (dialogId == DialogAction.INSTANCE_PARTY_MATCH.id()) {
			AutoGroupType agt = AutoGroupType.getAutoGroup(player.getLevel(), getNpcId());
			if (agt != null) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(agt.getInstanceMaskId()));
			}
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		} else if (dialogId == DialogAction.OPEN_INSTANCE_RECRUIT.id()) {
			AutoGroupType agt = AutoGroupType.getAutoGroup(player.getLevel(), getNpcId());
			if (agt != null) {
				PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(0x1A, agt.getInstanceMapId()));
			}
		} else {
			if (questId == 0) {
				PortalPath portalPath = DataManager.PORTAL2_DATA.getPortalDialog(getNpcId(), dialogId, player.getRace());
				if (portalPath != null) {
					PortalService.port(portalPath, player, getObjectId());
				}
			} else {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
			}
		}
		return true;
	}

	@Override
	protected void handleUseItemFinish(Player player) {
		checkDialog(player);
	}

	private void checkDialog(Player player) {
		int npcId = getNpcId();
		int entityId = getOwner().getSpawn() == null ? 0 : getOwner().getSpawn().getEntityId();
		for (int dialogId : questFirstDialogIds(npcId, entityId)) {
			if (AI2Actions.selectDialog(this, player, 0, dialogId).isSuccess()) {
				return;
			}
		}
		int teleportationDialogId = DataManager.PORTAL2_DATA.getTeleportDialogId(npcId);
		List<Integer> relatedQuests = GameEngineServices.questEngine().getQuestNpc(npcId).getOnTalkEvent();
		boolean playerHasQuest = false;
		boolean playerCanStartQuest = false;
		if (!relatedQuests.isEmpty()) {
			for (int questId : relatedQuests) {
				QuestState qs = player.getQuestStateList().getQuestState(questId);
				if (qs != null && (qs.getStatus() == QuestStatus.START || qs.getStatus() == QuestStatus.REWARD)) {
					playerHasQuest = true;
					break;
				} else if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
					if (QuestService.checkStartConditions(new QuestEnv(getOwner(), player, questId, 0), false)) {
						playerCanStartQuest = true;
						continue;
					}
				}
			}
		} if (playerHasQuest) {
			boolean isRewardStep = false;
			for (int questId : relatedQuests) {
				QuestState qs = player.getQuestStateList().getQuestState(questId);
				if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), rewardDialogId, questId));
					isRewardStep = true;
					break;
				}
			} if (!isRewardStep) {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), questDialogId));
			}
		} else if (playerCanStartQuest) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), startingDialogId));
		} else {
        	switch (npcId) {
				case 730883: // 光明方尖碑。 / Illuminary Obelisk.
				case 804619: // 幸运达努阿尔遗迹守卫。 / Lucky Danuar Reliquary Gatekeeper.
				case 804620: // 幸运奥菲丹桥守卫。 / Lucky Ophidan Bridge Gatekeeper.
				case 804621: // 达努阿尔遗迹。 / Danuar Reliquary.
				case 832991: // 被占领的伦图斯基地 [天族]。 / Occupied Rentus Base [Elyos].
				case 832992: // 被占领的伦图斯基地 [魔族]。 / Occupied Rentus Base [Asmodians].
				case 730721: // 封印的达努阿尔秘境 - 银色庄园 [天族]。 / Sealed Danuar Mysticarium - Silver Garden [Elyos].
				case 730722: // 封印的达努阿尔秘境 - 银色庄园 [魔族]。 / Sealed Danuar Mysticarium - Silver Garden [Asmodians].
				case 833024: // 石矛地域 [天族]。 / Stonespear Reach [Elyos].
				case 833025: // 石矛地域 [魔族]。 / Stonespear Reach [Asmodians].
				case 833043: // 石矛地域 [天族]。 / Stonespear Reach [Elyos].
				case 833044: // 石矛地域 [魔族]。 / Stonespear Reach [Asmodians].
				case 833045: // 石矛地域 [天族]。 / Stonespear Reach [Elyos].
				case 833046: // 石矛地域 [魔族]。 / Stonespear Reach [Asmodians].
				case 835609: //IDTransform_NPC_Entrance_PC
				case 835610: //IDStation_NPC_Entrance_PC
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10, 0));
				break;
				case 731549: // 被占领的符文安息处。 / Seized Danuar Sanctuary.
				    switch (player.getWorldId()) {
						case 210070000: //Cygnea.
						    // 进入被占领的符文安息处。 / Enter Seized Danuar Sanctuary.
							if (player.getCommonData().getRace() == Race.ASMODIANS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
						case 220080000: //Enshar.
						    // 进入被占领的符文安息处。 / Enter Seized Danuar Sanctuary.
							if (player.getCommonData().getRace() == Race.ELYOS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
					}
				break;
				case 731570: // 符文安息处。 / Danuar Sanctuary.
				    switch (player.getWorldId()) {
						case 210070000: //Cygnea.
						    // 进入符文安息处。 / Enter Danuar Sanctuary.
							if (player.getCommonData().getRace() == Race.ELYOS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
						case 220080000: //Enshar.
						    // 进入符文安息处。 / Enter Danuar Sanctuary.
							if (player.getCommonData().getRace() == Race.ASMODIANS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
					}
				break;
				case 832995: // 提亚马特要塞 [天族]。 / Tiamat Stronghold [Elyos].
				    switch (player.getWorldId()) {
						case 210070000: //Cygnea.
						    // 进入提亚马特要塞。 / Enter Tiamat Stronghold.
							if (player.getCommonData().getRace() == Race.ELYOS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
					}
				break;
				case 832996: // 提亚马特要塞 [魔族]。 / Tiamat Stronghold [Asmodians].
				    switch (player.getWorldId()) {
						case 220080000: //Enshar.
						    // 进入提亚马特要塞。 / Enter Tiamat Stronghold.
							if (player.getCommonData().getRace() == Race.ASMODIANS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1352, 0));
							}
						break;
					}
				break;
				case 832997: // [痛苦] 龙主避难所。 / [Anguished] Dragon Lord's Refuge.
				    switch (player.getWorldId()) {
					    case 210070000: //Cygnea.
						    // 进入痛苦龙主避难所。 / Enter the Anguished Dragon Lord's Refuge.
							if (player.getCommonData().getRace() == Race.ASMODIANS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
						case 220080000: //Enshar.
						    // 进入痛苦龙主避难所。 / Enter the Anguished Dragon Lord's Refuge.
						    if (player.getCommonData().getRace() == Race.ELYOS) {
						  	    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
						    }
						break;
					}
				break;
				case 832998: // 龙主避难所。 / Dragon Lord's Refuge.
					switch (player.getWorldId()) {
					    case 210070000: //Cygnea.
						    // 进入龙主避难所。 / Enter Dragon Lord's Refuge.
						    if (player.getCommonData().getRace() == Race.ELYOS) {
						  	    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1352, 0));
						    }
						break;
						case 220080000: //Enshar.
						    // 进入龙主避难所。 / Enter Dragon Lord's Refuge.
							if (player.getCommonData().getRace() == Race.ASMODIANS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1352, 0));
							}
						break;
					}
				break;
				default:
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), teleportationDialogId, 0));
				break;
			}
		}
	}

	static List<Integer> questFirstDialogIds(int npcId, int entityId) {
		if (npcId == FISSURE_OF_OBLIVION_EXIT && entityId == FISSURE_OF_OBLIVION_FINAL_ORB_ENTITY) {
			return List.of(QuestDialogAction.QUEST_SELECT.id());
		}
		// 克萝梅德试炼入口（兰尼尼亚/布里奇特）的开门对话由任务脚本承担（真端 FUN_180f859b0 的页轴）：
		// 未接 → select_none 接取页；进行中 → select1；可交 → 领奖页。这些页必须携带 questId 发送，
		// 否则客户端拿 questId=0 的页 10 去 NPC 通用对话 html 里查找并渲染失败——呈现为空白对话、
		// 任务无法接取（2026-10-07 实机：Kk 满足前置却接不到 18602，只有页 10/questId=0 下发）。
		// checkDialog 先经引擎重放本动作；引擎按 NPC 取所有含匹配路由的任务（未接也命中
		// unaccepted 分支），认领即发出契约页并短路下方旧页轴。
		// Kromede's Trial entrance open-door dialogs are served by the quest script (retail
		// FUN_180f859b0 page axis): accept pages while unaccepted, select1 while in progress, reward
		// pages while reportable. They must carry the questId, or the client renders the context-less
		// page 10 against the NPC dialog html and shows an empty window (live 2026-10-07: Kk met the
		// prerequisites yet could not accept 18602). checkDialog replays this action through the quest
		// engine first; the engine considers every route-matching quest of the npc (unaccepted ones
		// included) and the claim short-circuits the legacy page axis below.
		if (isKromedeTrialEntryNpc(npcId)) {
			return List.of(QuestDialogAction.QUEST_SELECT.id());
		}
		return List.of();
	}

	/**
	 * 是否为克萝梅德试炼入口 NPC（天族兰尼尼亚 205229 / 魔族布里奇特 205234）——
	 * 仅这两个 NPC 的「进入恶梦」动作需要先经任务引擎推进任务步。
	 * Whether the npc is a Kromede's Trial entrance (Raninia 205229 / Bridget 205234): only these two
	 * serve the "enter the nightmare" action through the quest engine first.
	 */
	static boolean isKromedeTrialEntryNpc(int npcId) {
		return npcId == KROMEDE_TRIAL_ENTRY_ELYOS || npcId == KROMEDE_TRIAL_ENTRY_ASMODIANS;
	}

	private void announceIlluminaryObeliskOpen() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			// 通往炼狱光明方尖碑的入口已开启。 / The entrance to the Infernal Illuminary Obelisk has opened.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDF5_U3_Hard_Door_Open);
		});
	}
}
