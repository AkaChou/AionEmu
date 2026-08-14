package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
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
 * 阿德玛要塞副本事件处理器。
 * Instance event handler for Adma Stronghold.
 *
 * @author Encom
 */

@InstanceID(320130000)
public class AdmaStrongholdInstance extends GeneralInstanceHandler
{
		/** 是否启动计时器 / is start timer */
		private boolean isStartTimer = false;
		/** suspiciouspot 任务 / suspicious pot task */
		private Future<?> suspiciousPotTask;
		/** suspicious pot / suspicious pot */
		private List<Npc> suspiciousPot = new ArrayList<Npc>();
		/** 对象 / objects */
		private Map<Integer, VisibleObject> objects = new LinkedHashMap<Integer, VisibleObject>();
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
			case 237148: //Captain Mundirve.
			case 237149: //Butler Luitart.
			case 237150: //Chief Maid Miladi.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000032, 1)); //Observation Post Passage Key.
		    break;
			case 237155: //Bard Guionbark.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000027, 1)); //Library Key.
		    break;
			case 237240: //Enthralled Gutorum.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000026, 1)); //Inner Chamber Key.
		    break;
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 237241: //Enthralled Karemiwen.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 170175031, 1)); //[Souvenir] Karemiwen's Teddy Bear.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000028, 1)); //Main Hall Key.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 2)) {
				        case 1:
						    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 123000927, 1)); //Karemiwen's Band.
						break;
						case 2:
						    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 123000928, 1)); //Karemiwen's Leather Belt.
						break;
					}
				}
			break;
			case 237242: //Enthralled Taliesin.
			    switch (Rnd.get(1, 3)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000029, 1)); //Great Dining Hall Key.
				    break;
				    case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000030, 1)); //Lannok Treasury Key.
				    break;
				    case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000031, 1)); //Servants Quarters Key.
				    break;
			    }
		    break;
			case 237239: //Death Reaper.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053788, 1)); //Greater Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054548, 1)); //Master Lanmark's Weapon Box.
					    break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053273, 1)); //Master Accessory Treasure Box.
					    break;
					}
				}
			break;
		}
	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * @param player 玩家 / player
	 * @param npc NPC / npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 700396: //Ntuamu's Teddy Bear.
				player.getEffectController().removeEffect(18462); //Deep Wound.
			break;
			case 700397: //Tarnished Incense Burner.
				player.getEffectController().removeEffect(18463); //Mental Tremor.
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
		spawn(237244, 606.483f, 745.0968f, 197.72092f, (byte) 61); //Enthralled Lannok.
		switch (Rnd.get(1, 2)) {
		    case 1:
				spawn(237240, 378.346f, 222.61f, 164.007f, (byte) 65); //Enthralled Gutorum.
			break;
			case 2:
				spawn(237240, 525.4f, 222.724f, 164.007f, (byte) 88); //Enthralled Gutorum.
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
		// 可疑罐子将在 3 分钟后消失。 / The Suspicious Pot will disappear in 3 minutes.
		sendMsgByRace(1403059, Race.PC_ALL, 2000);
		if (!isStartTimer) {
			isStartTimer = true;
			System.currentTimeMillis();
			suspiciousPot.add((Npc) spawn(237245, 451.54147f, 276.3691f, 170.08488f, (byte) 90)); //Suspicious Pot.
			suspiciousPotTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					// 可疑罐子已消失。 / The Suspicious Pot has disappeared.
					sendMsg(1403060);
					suspiciousPot.get(0).getController().onDelete();
				}
			}, 180000);
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
			case 237245: //Suspicious Pot.
				suspiciousPotTask.cancel(true);
				// 须按正确顺序摧毁暗影幽魂，否则它们会保留力量。 / Destroy the Shadow Specters in the proper order or they will retain their power.
				sendMsgByRace(1403038, Race.PC_ALL, 4000);
				// 暗影幽魂正前往仓库，寻找仓库管理员古托伦。 / The Shadow Specter is moving towards the storehouse, to look for Warehouse Manager Gutorum.
				sendMsgByRace(1403014, Race.PC_ALL, 6000);
			break;
			case 237240: //Enthralled Gutorum.
				// 暗影幽魂正前往卡雷米温卧室，寻找卡雷米温公主。 / The Shadow Specter is moving towards Karemiwen's Bedroom, to look for Princess Karemiwen.
				sendMsgByRace(1403015, Race.PC_ALL, 2000);
			break;
			case 237241: //Enthralled Karemiwen.
				// 一道暗影正滑向二楼主厅。 / A dark shadow is slithering towards the 2nd floor Main Hall.
				sendMsgByRace(1403016, Race.PC_ALL, 2000);
			break;
			case 237242: //Enthralled Taliesin.
				// 暗影幽魂正前往地下马厩，寻找马厩管理员泽图伦。 / The Shadow Specter is moving towards the Underground Stable, to look for Stable Keeper Zeeturun.
				sendMsgByRace(1403017, Race.PC_ALL, 2000);
			break;
			case 237243: //Enthralled Zeeturun.
				// 暗影幽魂正前往崩塌观察哨，寻找兰马克领主。 / The Shadow Specter is moving towards the Collapsed Observation Post, to look for Lord Lanmark.
				sendMsgByRace(1403018, Race.PC_ALL, 2000);
			break;
			case 237148: //Captain Mundirve.
				spawn(237159, 346.57733f, 534.69476f, 181.204f, (byte) 40);
                spawn(237159, 345.45975f, 544.2697f, 182.18115f, (byte) 71);
                spawn(237160, 359.008f, 557.2267f, 181.3445f, (byte) 79);
                spawn(237160, 349.17578f, 519.81287f, 181.27892f, (byte) 15);
                spawn(237161, 344.26273f, 525.73566f, 180.68095f, (byte) 69);
                spawn(237161, 356.2736f, 556.973f, 180.74712f, (byte) 12);
            break;
			case 237244: //Enthralled Lannok.
				despawnNpc(npc);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
						// 死亡收割者出现在崩塌观察哨。 / The Death Reaper appeared at the Collapsed Observation Post.
						sendMsgByRace(1403019, Race.PC_ALL, 0);
						spawn(237239, 606.483f, 745.0968f, 197.72092f, (byte) 61); //Death Reaper.
				    }
			    }, 3000);
            break;
			case 237239: //Death Reaper.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Adma Stronghold>");
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 614.9905f, 745.60156f, 198.75998f, (byte) 60); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 614.9905f, 745.60156f, 198.75998f, (byte) 60); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				SpawnTemplate wreckOfUnstableExit = SpawnEngine.addNewSingleTimeSpawn(320130000, 730176, 627.72888f, 745.44885f, 199.8019f, (byte) 0);
			    wreckOfUnstableExit.setEntityId(66);
			    objects.put(730176, SpawnEngine.spawnObject(wreckOfUnstableExit, instanceId));
            break;
		}
    }
	/**
	 * 移除相关物品。
	 * Remove related items.
	 *
	 * @param player 玩家 / player
	 */
	
	public void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(185000026, storage.getItemCountByItemId(185000026)); //Inner Chamber Key.
		storage.decreaseByItemId(185000027, storage.getItemCountByItemId(185000027)); //Library Key.
        storage.decreaseByItemId(185000028, storage.getItemCountByItemId(185000028)); //Main Hall Key.
		storage.decreaseByItemId(185000029, storage.getItemCountByItemId(185000029)); //Great Dining Hall Key.
		storage.decreaseByItemId(185000030, storage.getItemCountByItemId(185000030)); //Lannok Treasury Key.
		storage.decreaseByItemId(185000031, storage.getItemCountByItemId(185000031)); //Servants Quarters Key.
		storage.decreaseByItemId(185000032, storage.getItemCountByItemId(185000032)); //Observation Post Passage Key.
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
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}
}