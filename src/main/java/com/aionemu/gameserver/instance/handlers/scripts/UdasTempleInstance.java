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
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Map;
import java.util.Set;

/**
 * 乌达斯神殿副本事件处理器。
 * Instance event handler for Udas Temple.
 *
 * @author Encom
 */


@InstanceID(300150000)
public class UdasTempleInstance extends GeneralInstanceHandler {

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
				spawn(215787, 778.537f, 661.278f, 134.0f, (byte) 78); //Cota The Gatekeeper.
			break;
			case 2:
				spawn(215787, 689.529f, 669.005f, 134.0f, (byte) 103); //Cota The Gatekeeper.
			break;
		} switch (Rnd.get(1, 2)) {
		    case 1:
				spawn(215788, 807.969f, 560.979f, 130.902f, (byte) 60); //Kiya The Protector.
			break;
			case 2:
				spawn(215788, 749.2811f, 559.79895f, 131.29901f, (byte) 0); //Kiya The Protector.
			break;
		}
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
			case 215782: //Vallakhan.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000084, 1)); //Great Chapel Key.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053788, 1)); //Greater Stigma Support Bundle.
		    break;
			case 215787: //Cota The Gatekeeper.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000083, 1)); //Silent Chapel Key.
		    break;
			case 215791: //Agra The Guide.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000085, 1)); //Chamber Of Guidance Key.
		    break;
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
		}
	}
	
	@Override
    public void onDie(Npc npc) {
        Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 215783: //Nexus.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Udas Temple>");
				spawn(730255, 508.3610f, 362.7170f, 137.0000f, (byte) 31); //Udas Temple Exit.
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 508.4381f, 374.57526f, 135.88919f, (byte) 30); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 508.4381f, 374.57526f, 135.88919f, (byte) 30); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
            break;
			case 215787: //Cota The Gatekeeper.
				// 统一封印已被削弱。 / The Seal of Uniformity has been weakened.
				sendMsgByRace(1400366, Race.PC_ALL, 2000);
			break;
			case 215790: //Tala The Protector.
			    doors.get(99).setOpen(true);
				// 你现在可进入团结之室。 / You can now enter the Chamber of Unity.
				sendMsgByRace(1400367, Race.PC_ALL, 2000);
			break;
		}
    }
	
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}
	
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
	}
	
	@Override
    public void onInstanceDestroy() {
        doors.clear();
    }
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000083, storage.getItemCountByItemId(185000083)); //Silent Chapel Key.
		storage.decreaseByItemId(185000084, storage.getItemCountByItemId(185000084)); //Great Chapel Key.
		storage.decreaseByItemId(185000085, storage.getItemCountByItemId(185000085)); //Chamber Of Guidance Key.
	}
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
	
	protected void sendMsgByRace(final int msg, final Race race, int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				instance.doOnAllPlayers(new Visitor<Player>() {
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