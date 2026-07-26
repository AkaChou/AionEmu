package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.teleport.PortalService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;

/**
 * 传送门/传送点 AI：Portal Dialog（@AIName "portal_dialog"），继承 PortalAI2。
 * Portal/teleporter AI: Portal Dialog (@AIName "portal_dialog"), extends PortalAI2.
 *
 * @author Encom
 */
@AIName("portal_dialog")
public class PortalDialogAI2 extends PortalAI2 {

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
			case 730399: //Rentus Base.
			case 731549: //[Seized] Danuar Sanctuary.
			case 731570: //Danuar Sanctuary.
			case 832991: //Occupied Rentus Base [Elyos].
			case 832992: //Occupied Rentus Base [Asmodians].
			case 832995: //Tiamat Stronghold [Elyos].
			case 832996: //Tiamat Stronghold [Asmodians].
			case 832997: //[Anguished] Dragon Lord Refuge.
			case 832998: //Dragon Lord Refuge.
/* 				startLifeTask(); */
			break;
			case 730883: //[Infernal] Illuminary Obelisk.
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
		} if (dialogId == DialogAction.INSTANCE_PARTY_MATCH.id()) {
			MatchDefinition agt = MatchDefinition.forNpc(player.getLevel(), getNpcId());
			if (agt != null) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(agt.getInstanceMaskId()));
			}
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		} else if (dialogId == DialogAction.OPEN_INSTANCE_RECRUIT.id()) {
			MatchDefinition agt = MatchDefinition.forNpc(player.getLevel(), getNpcId());
			if (agt != null) {
				PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(0x1A, agt.getInstanceMapId()));
			}
		} else {
			if (questId == 0) {
					PortalPath portalPath = DataManager.PORTAL2_DATA.getPortalDialog(getNpcId(), dialogId, player.getRace(),
							player.getWorldId());
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
				case 730883: //Illuminary Obelisk.
				case 804619: //Lucky Danuar Reliquary Gatekeeper.
				case 804620: //Lucky Ophidan Bridge Gatekeeper.
				case 804621: //Danuar Reliquary.
				case 832991: //Occupied Rentus Base [Elyos]
				case 832992: //Occupied Rentus Base [Asmodians]
				case 730721: //Sealed Danuar Mysticarium - Argent Manor [Elyos]
				case 730722: //Sealed Danuar Mysticarium - Argent Manor [Asmodians]
				case 833024: //Stonespear Reach [Elyos]
				case 833025: //Stonespear Reach [Asmodians]
				case 833043: //Stonespear Reach [Elyos]
				case 833044: //Stonespear Reach [Asmodians]
				case 833045: //Stonespear Reach [Elyos]
				case 833046: //Stonespear Reach [Asmodians]
				case 835609: //IDTransform_NPC_Entrance_PC
				case 835610: //IDStation_NPC_Entrance_PC
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10, 0));
				break;
				case 731549: //Seized Danuar Sanctuary.
				    switch (player.getWorldId()) {
						case 210070000: //Cygnea.
						    // 进入被占领的达努阿尔圣所。 / Enter Seized Danuar Sanctuary.
							if (player.getCommonData().getRace() == Race.ASMODIANS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
						case 220080000: //Enshar.
						    // 进入被占领的达努阿尔圣所。 / Enter Seized Danuar Sanctuary.
							if (player.getCommonData().getRace() == Race.ELYOS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
					}
				break;
				case 731570: //Danuar Sanctuary.
				    switch (player.getWorldId()) {
						case 210070000: //Cygnea.
						    // 进入达努阿尔圣所。 / Enter Danuar Sanctuary.
							if (player.getCommonData().getRace() == Race.ELYOS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
						case 220080000: //Enshar.
						    // 进入达努阿尔圣所。 / Enter Danuar Sanctuary.
							if (player.getCommonData().getRace() == Race.ASMODIANS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
					}
				break;
				case 832995: //Tiamat Stronghold [Elyos]
				    switch (player.getWorldId()) {
						case 210070000: //Cygnea.
						    // 进入提亚马特要塞。 / Enter Tiamat Stronghold.
							if (player.getCommonData().getRace() == Race.ELYOS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
					}
				break;
				case 832996: //Tiamat Stronghold [Asmodian]
				    switch (player.getWorldId()) {
						case 220080000: //Enshar.
						    // 进入提亚马特要塞。 / Enter Tiamat Stronghold.
							if (player.getCommonData().getRace() == Race.ASMODIANS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1352, 0));
							}
						break;
					}
				break;
				case 832997: //[Anguished] Dragon Lord Refuge.
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
				case 832998: //Dragon Lord Refuge.
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
	
	private void announceIlluminaryObeliskOpen() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 通往炼狱光明方尖碑的入口已开启。 / The entrance to the Infernal Illuminary Obelisk has opened.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDF5_U3_Hard_Door_Open);
			}
		});
	}
}
