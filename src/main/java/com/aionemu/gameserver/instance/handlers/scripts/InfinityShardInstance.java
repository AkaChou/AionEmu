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
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.Set;

/**
 * 无限碎片副本事件处理器。
 * Instance event handler for Infinity Shard.
 *
 * @author Encom
 */

@InstanceID(300800000)
public class InfinityShardInstance extends GeneralInstanceHandler
{
	/** ide forcefield generator / ide forcefield generator */
		private int ideForcefieldGenerator;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed = false;
	/** 对象 / objects */
		private Map<Integer, VisibleObject> objects = new LinkedHashMap<Integer, VisibleObject>();
	
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
            case 231073: //Hyperion.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
                    } switch (Rnd.get(1, 4)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052387, 1)); //Hyperion's Equipment Box.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052718, 1)); //Hyperion's Weapons Chest.
				        break;
						case 3:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053005, 1)); //Hyperion's Wing Chest.
				        break;
						case 4:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053154, 1)); //Hyperion's Accessory Box.
				        break;
					}
                }
            break;
			case 802184: //Infinity Shard Opportunity Bundle.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000051, 30)); //Major Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000052, 30)); //Greater Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000236, 50)); //Blood Mark.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000237, 50)); //Ancient Coin.
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
		SpawnTemplate protectiveShield = SpawnEngine.addNewSingleTimeSpawn(300800000, 284437, 129.26147f, 137.86557f, 110.50481f, (byte) 0);
		protectiveShield.setEntityId(27);
		objects.put(284437, SpawnEngine.spawnObject(protectiveShield, instanceId)); //Protective Shield.
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
			case 231096: //Hyperion Defense Combatant.
			case 231097: //Hyperion Defense Scout.
			case 231098: //Hyperion Defense Medic.
			case 231103: //Summoned Ancien Tyrhund.
			case 233297: //Hyperion Defense Assaulter.
			case 233298: //Hyperion Defense Assassin.
			    despawnNpc(npc);
			break;
			case 231074: //Ide Forcefield Generator I.
			case 231078: //Ide Forcefield Generator II.
			case 231082: //Ide Forcefield Generator III.
			case 231086: //Ide Forcefield Generator IV.
				ideForcefieldGenerator++;
				if (ideForcefieldGenerator == 1) {
				} else if (ideForcefieldGenerator == 2) {
				} else if (ideForcefieldGenerator == 3) {
				} else if (ideForcefieldGenerator == 4) {
				    // 许珀里翁的护盾已落下。 / The Hyperion's shields are down.
					sendMsgByRace(1401796, Race.PC_ALL, 10000);
					deleteNpc(284437); //Protective Shield.
				}
				despawnNpc(npc);
				// 许珀里翁的护盾正在减弱。 / The Hyperion's shields are faltering.
				sendMsgByRace(1401795, Race.PC_ALL, 0);
            break;
			case 231073: //Hyperion.
			    // 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Infinity Shard>");
				spawn(730842, 124.669853f, 137.840668f, 113.942917f, (byte) 0); //Infinity Shard Exit.
				spawn(802184, 127.32316f, 131.72421f, 112.17429f, (byte) 25); //Infinity Shard Opportunity Bundle.
			break;
		}
	}
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
    public void onInstanceDestroy() {
        isInstanceDestroyed = true;
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