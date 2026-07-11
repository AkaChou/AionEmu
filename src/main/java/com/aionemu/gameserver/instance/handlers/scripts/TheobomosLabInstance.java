package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 西奥博莫斯实验室副本事件处理器。
 * Instance event handler for Theobomos Lab.
 *
 * @author Encom
 */

@InstanceID(310110000)
public class TheobomosLabInstance extends GeneralInstanceHandler
{
	/** silikor guard / silikor guard */
		private int silikorGuard;
	/** 是否启动计时器 / is start timer */
		private boolean isStartTimer = false;
	/** elementsealingstone 任务 / element sealing stone task */
		private Future<?> elementSealingStoneTask;
	/** element sealing stone / element sealing stone */
		private List<Npc> elementSealingStone = new ArrayList<Npc>();
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
        switch (npcId) {
			case 237108: //Frozen Harint.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000016, 1)); //Fire Key.
		    break;
			case 237110: //Naughty Pocaching.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000025, 1)); //Laboratory Key.
		    break;
			case 237112: //Wistful Syripne.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000023, 1)); //Syripne's Key.
		    break;
			case 237113: //Soul Spirit Nomura.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000022, 1)); //Nomura's Key.
		    break;
			case 237114: //Water Spirit Undine.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000021, 1)); //Undine's Key.
		    break;
			case 700422: //Faded Book.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 182208053, 1)); //Research Center Document.
		    break;
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 237247: //Watcher Cracked Nuhas.
				switch (Rnd.get(1, 3)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000018, 1)); //Water Key.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000019, 1)); //Earth Key.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000020, 1)); //Wind Key.
					break;
				}
		    break;
			case 237251: //Corrupted Ifrit.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053788, 1)); //Greater Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054176, 1)); //Master Triroan's Weapon Box.
					    break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054180, 1)); //Master Accessory Relic Boxx.
					    break;
					}
				}
			break;
			case 237118: //Titan Protector.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000015, 1)); //Laboratory Chest Key.
		    break;
			case 237119: //Antique Treasure Chest.
				switch (Rnd.get(1, 8)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050023, 2)); //Noble Blue Idian: Physical Attack.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050024, 2)); //Noble Blue Idian: Magical Attack.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050025, 2)); //Noble Blue Idian: Physical Defense.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050026, 2)); //Noble Blue Idian: Magical Defense.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050027, 2)); //Noble Blue Idian: Assistance.
					break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050028, 2)); //Noble Blue Idian: Resistance.
					break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050029, 2)); //Noble Blue Idian: Physical Magical Attack.
					break;
					case 8:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050030, 2)); //Noble Blue Idian: Physical Magical Defense.
					break;
				}
			break;
			case 237120: //Antique Treasure Chest.
			case 237121: //Antique Treasure Chest.
				switch (Rnd.get(1, 8)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050031, 2)); //Esoteric Idian: Physical Attack.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050032, 2)); //Esoteric Idian: Magical Attack.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050033, 2)); //Esoteric Idian: Physical Defense.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050034, 2)); //Esoteric Idian: Magical Defense.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050035, 2)); //Esoteric Idian: Assistance.
					break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050036, 2)); //Esoteric Idian: Resistance.
					break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050037, 2)); //Esoteric Idian: Physical Magical Attack.
					break;
					case 8:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050038, 2)); //Esoteric Idian: Physical Magical Defense.
					break;
				}
			break;
        }
    }
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		spawn(237250, 616.169f, 488.758f, 196.015f, (byte) 62); //Sealed Unstable Triroan.
		switch (Rnd.get(1, 3)) {
			case 1:
				spawn(237119, 455.78845f, 774.0474f, 157.89963f, (byte) 0); //Antique Treasure Chest.
			break;
			case 2:
				spawn(237120, 455.78845f, 774.0474f, 157.89963f, (byte) 0); //Antique Treasure Chest.
			break;
			case 3:
				spawn(237121, 455.78845f, 774.0474f, 157.89963f, (byte) 0); //Antique Treasure Chest.
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
		// 元素封印石已出现。将在 3 分钟后消失。 / The Element Sealing Stone has appeared. The Element Sealing Stone will disappear in 3 minutes.
		sendMsgByRace(1403061, Race.PC_ALL, 2000);
		if (!isStartTimer) {
			isStartTimer = true;
			System.currentTimeMillis();
			elementSealingStone.add((Npc) spawn(237253, 477.88632f, 230.60364f, 173.06987f, (byte) 90)); //Fiery Sealing Stone.
			elementSealingStoneTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					// 元素封印石已消失。 / The Element Sealing Stone has disappeared.
					sendMsg(1403062);
					elementSealingStone.get(0).getController().onDelete();
				}
			}, 180000);
		}
	}
	
    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * npc
     */
    @Override
    public void onDie(Npc npc) {
        Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 237253: //Fiery Sealing Stone.
				elementSealingStoneTask.cancel(true);
				// 若未按正确顺序举行仪式，辉煌元素将失去力量。 / If you do not perform the proper order of the ritual, the Brilliant Elemental will lose its power.
				sendMsgByRace(1403039, Race.PC_ALL, 4000);
				// 辉煌元素正朝记忆硅石所在的元素核心生成室发出光束。 / The Brilliant Elemental is beaming towards the researcher's lounge where Queen Arachne is located.
				sendMsgByRace(1403021, Race.PC_ALL, 6000);
				spawn(237258, 477.88632f, 230.60364f, 173.06987f, (byte) 90); //Demon Lord Mulion.
			break;
			case 237246: //Watcher Queen Arachne.
				// 刺眼光束正射向中央控制室。 / The blinding light is beaming towards the Central Control Room.
				sendMsgByRace(1403022, Race.PC_ALL, 2000);
            break;
			case 237247: //Watcher Cracked Nuhas.
				// 辉煌元素正朝记忆硅石所在的元素核心生成室发出光束。 / The Brilliant Elemental is beaming towards the Elemental Core Generation Room where the Silicanimum of Memory is located.
				sendMsgByRace(1403023, Race.PC_ALL, 2000);
            break;
			case 237248: //Watcher Silikor Of Memory.
				// 辉煌元素正朝记忆硅石所在的元素核心生成室发出光束。 / The Brilliant Elemental is beaming towards the Library of Theobomos where Jilitia of Innocence is located.
				sendMsgByRace(1403024, Race.PC_ALL, 2000);
            break;
			case 237249: //Watcher Jilitia.
				// 辉煌元素正朝记忆硅石所在的元素核心生成室发出光束。 / The Brilliant Elemental is beaming towards the Elemental Core Testing Room where Unstable Triroan is located.
				sendMsgByRace(1403025, Race.PC_ALL, 2000);
            break;
			case 280971: //First Silikor Guard.
			case 280972: //Second Silikor Guard.
				silikorGuard ++;
				if (silikorGuard == 1) {
				} else if (silikorGuard == 2) {
					spawn(237248, 392.5771f, 744.2743f, 189.38637f, (byte) 41); //Watcher Silikor Of Memory.
				}
            break;
			case 237250: //Sealed Unstable Triroan.
				despawnNpc(npc);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
						// 破碎元素领主已出现。 / Fractured Elemental Lord has appeared.
						sendMsgByRace(1403026, Race.PC_ALL, 0);
						spawn(237251, 616.169f, 488.758f, 196.015f, (byte) 62); //Corrupted Ifrit.
				    }
			    }, 3000);
			break;
			case 237251: //Corrupted Ifrit.
			    //sendMsg("Congratulation]: you finish <Theobomos Lab>");
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 602.04486f, 488.82837f, 196.01512f, (byte) 60); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 602.04486f, 488.82837f, 196.01512f, (byte) 60); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				spawn(730178, 637.3241f, 475.9548f, 195.96295f, (byte) 0, 244); //Unstable Exit Fragment.
			break;
		}
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
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
	 * message
	 * 阵营 / race
	 * time
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