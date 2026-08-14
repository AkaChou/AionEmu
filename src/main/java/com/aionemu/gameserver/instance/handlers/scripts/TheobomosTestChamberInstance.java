package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Map;
import java.util.Set;

/**
 * 泰奥勃莫斯试验室副本事件处理器。
 * Instance event handler for Theobomos Test Chamber.
 *
 * @author Encom
 */

@InstanceID(301610000)
public class TheobomosTestChamberInstance extends GeneralInstanceHandler
{
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * @param npc NPC / npc
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
        switch (npcId) {
			case 220425: //Galateia The Living.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000264, 1)); //Blood-Sealed Treasure Box Key.
			break;
			case 220426: //Desecrated Ifrit.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058413, 1)); //? ?  ??.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188057620, 1)); //Chaotic Dimension Stone Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 166040001, 1)); //Essence Core Solution.
						switch (Rnd.get(1, 4)) {
				            case 1:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057618, 1)); //Theobomos's Weapon Box.
				            break;
							case 2:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057619, 1)); //Theobomos's Armor Box.
				            break;
							case 3:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054908, 1)); //Laboratory Weapon Box.
				            break;
					        case 4:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054909, 1)); //Laboratory Armor Box.
				            break;
						}
					}
                }
            break;
			case 806221: //Blood-Sealed Coffer.
				switch (Rnd.get(1, 2)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 110000049, 1)); //Galateia's Dress.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 125004518, 1)); //Galateia's Hairpin.
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
        doors = instance.getDoors();
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
			case 220424: //Blood-Swollen Arachne.
			    doors.get(129).setOpen(true);
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 2000);
			break;
			case 220425: //Galateia The Living.
			    doors.get(2).setOpen(true);
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 2000);
			break;
			case 220426: //Desecrated Ifrit.
			    spawn(806206, 298.48328f, 120.17713f, 196.02815f, (byte) 68); //Theobomos Test Chamber Exit.
				spawn(806221, 284.93094f, 119.47065f, 196.01285f, (byte) 1); //Blood-Sealed Coffer.
			    // 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Theobomos Test Chamber>");
			break;
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
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
    public void onInstanceDestroy() {
        doors.clear();
    }
}