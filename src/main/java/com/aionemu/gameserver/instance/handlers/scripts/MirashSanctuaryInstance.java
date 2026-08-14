package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.*;

/**
 * 米拉什圣所副本事件处理器。
 * Instance event handler for Mirash Sanctuary.
 *
 * @author Encom
 */

@InstanceID(301720000)
public class MirashSanctuaryInstance extends GeneralInstanceHandler
{
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
		doors = instance.getDoors();
		switch (Rnd.get(1, 2)) {
			case 1:
				spawn(248533, 664.0945f, 623.5564f, 532.5159f, (byte) 2); //IDAbRe_Core_03_Key_Drakan_High_As_An.
			break;
			case 2:
				spawn(248533, 684.9298f, 630.2946f, 530.1250f, (byte) 74); //IDAbRe_Core_03_Key_Drakan_High_As_An.
			break;
		}
    }
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		super.onInstanceCreate(instance);
	}
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * @param npc NPC / npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 835730: //IDAbRe_Core_03_TreasureBox01.
			case 835732: //IDAbRe_Core_03_TreasureBox03.
			case 835733: //IDAbRe_Core_03_TreasureBox04.
				switch (Rnd.get(1, 4)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058115, 1)); //?    .
				    break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058116, 1)); //?  ?  .
				    break;
					case 3:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058117, 1)); //?     ??.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058118, 1)); //?   .
					break;
				}
			break;
			case 835784: //IDAb1_Core_03_Stone_01.
			case 835785: //IDAb1_Core_03_Stone_02.
			case 835786: //IDAb1_Core_03_Stone_03.
			case 835787: //IDAb1_Core_03_Stone_04.
			case 835788: //IDAb1_Core_03_Stone_05.
			case 835789: //IDAb1_Core_03_Stone_06.
				switch (Rnd.get(1, 5)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000312, 1)); //item_core_03_stone_01.
				    break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000313, 1)); //item_core_03_stone_02.
				    break;
					case 3:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000314, 1)); //item_core_03_stone_03.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000315, 1)); //item_core_03_stone_04.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000316, 1)); //item_core_03_stone_05.
					break;
				}
			break;
			/*
			case 248382: //IDAbRe_Core_03_A1_Witch_An.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000531, 1)); //IDAbRe_Core_03_Move_Item_Skill.
			break;
			*/
			case 248533: //IDAbRe_Core_03_Key_Drakan_High_As_An.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000317, 1)); //item_core_03_key_01.
			break;
			case 248013: // .
				switch (Rnd.get(1, 6)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058117, 1)); //?     ??.
				    break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058118, 1)); //?   .
				    break;
					case 3:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190200000, 50)); //.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058130, 1)); //?  ?   ??.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058131, 1)); //?  ?   ??.
					break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058132, 1)); //?  ?   ??.
					break;
				} switch (Rnd.get(1, 4)) {
					case 1:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080005, 5)); //低级随从契约。 / Lesser Minion Contract.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080006, 5)); //高级随从契约。 / Greater Minion Contract.
					break;
					case 3:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080007, 5)); //大型随从契约。 / Major Minion Contract.
					break;
					case 4:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080008, 5)); //可爱随从契约。 / Cute Minion Contract.
					break;
				}
			break;
		}
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * @param npc NPC / npc
	 */
	@Override
    public void onDie(Npc npc) {
        Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 248382: //IDAbRe_Core_03_A1_Witch_An.
			    player.getSkillList().addSkill(player, 11333, 1);
			break;
			case 248013: // .
			    SkillLearnService.removeSkill(player, 11333);
			    spawn(835733, npc.getX(), npc.getY(), npc.getZ(), (byte) 0); //IDAbRe_Core_03_TreasureBox04.
			break;
			case 248389:
			    despawnNpc(npc);
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
						mirashSanctuaryWave();
					}
				}, 5000);
			break;
			case 248444: //IDAbRe_Core_03_Resurrect_Drakan_Statue_01.
				spawn(248449, player.getX(), player.getY(), player.getZ(), (byte) 0);
			break;
			case 248445: //IDAbRe_Core_03_Resurrect_Drakan_Statue_02.
				spawn(248450, player.getX(), player.getY(), player.getZ(), (byte) 0);
			break;
			case 248446: //IDAbRe_Core_03_Resurrect_Drakan_Statue_03.
				spawn(248451, player.getX(), player.getY(), player.getZ(), (byte) 0);
			break;
			case 248447: //IDAbRe_Core_03_Resurrect_Drakan_Statue_04.
				spawn(248452, player.getX(), player.getY(), player.getZ(), (byte) 0);
			break;
		}
    }
	
	private void raidMirashSanctuary(final Npc npc, float x, float y, float z, boolean despawn) {
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}
	
	private void mirashSanctuaryWave() {
		raidMirashSanctuary((Npc)spawn(248385, 283.95505f, 482.93155f, 548.0041f, (byte) 30), 284.07160f, 559.67145f, 547.99650f, false);
		raidMirashSanctuary((Npc)spawn(248383, 282.09033f, 485.19363f, 547.9946f, (byte) 30), 281.76416f, 559.63020f, 547.99585f, false);
		raidMirashSanctuary((Npc)spawn(248387, 279.90730f, 483.05050f, 548.0014f, (byte) 29), 279.76407f, 559.63390f, 547.99390f, false);
	}
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
    public void onInstanceDestroy() {
		isInstanceDestroyed = true;
        doors.clear();
    }
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(164000531, storage.getItemCountByItemId(164000531));
	}
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
	/**
	 * 处理 sendMsgByRace。
	 * Handle sendMsgByRace.
	 *
	 * @param msg 消息 / message
	 * @param race 阵营 / race
	 * @param time 时间 / time
	 */
	
	protected void sendMsgByRace(final int msg, final Race race, int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				instance.doOnAllPlayers(new Visitor<Player>() {
					/**
					 * 处理 visit。
					 * Handle visit.
					 *
					 * @param player 玩家 / player
					 */
					@Override
					public void visit(Player player) {
						if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
						}
					}
				});
			}
		}, time);
	}
}