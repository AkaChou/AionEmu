package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

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
		} if (dialogId == DialogAction.INSTANCE_PARTY_MATCH.id()) {
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
				case 832991: // 被占领的伦图斯基地 [天族]。
				case 832992: // 被占领的伦图斯基地 [魔族]。
				case 730721: // 封印的达努阿尔秘境 - 银色庄园 [天族]。
				case 730722: // 封印的达努阿尔秘境 - 银色庄园 [魔族]。
				case 833024: // 石矛地域 [天族]。
				case 833025: // 石矛地域 [魔族]。
				case 833043: // 石矛地域 [天族]。
				case 833044: // 石矛地域 [魔族]。
				case 833045: // 石矛地域 [天族]。
				case 833046: // 石矛地域 [魔族]。
				case 835609: //IDTransform_NPC_Entrance_PC
				case 835610: //IDStation_NPC_Entrance_PC
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10, 0));
				break;
				case 731549: // 被占领的符文安息处。
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
				case 731570: // 符文安息处。
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
				case 832995: // 提亚马特要塞 [天族]。
				    switch (player.getWorldId()) {
						case 210070000: //Cygnea.
						    // 进入提亚马特要塞。 / Enter Tiamat Stronghold.
							if (player.getCommonData().getRace() == Race.ELYOS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011, 0));
							}
						break;
					}
				break;
				case 832996: // 提亚马特要塞 [魔族]。
				    switch (player.getWorldId()) {
						case 220080000: //Enshar.
						    // 进入提亚马特要塞。 / Enter Tiamat Stronghold.
							if (player.getCommonData().getRace() == Race.ASMODIANS) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1352, 0));
							}
						break;
					}
				break;
				case 832997: // [痛苦] 龙主避难所。
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
				case 832998: // 龙主避难所。
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
