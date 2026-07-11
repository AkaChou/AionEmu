package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
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
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.Map;
import java.util.Set;

/**
 * 提亚马特要塞副本事件处理器。
 * Instance event handler for Tiamat Stronghold.
 *
 * @author Encom
 */

@InstanceID(300510000)
public class TiamatStrongholdInstance extends GeneralInstanceHandler {
	/** 击杀数 / kill count */
	private int kills;
	/** protectorate / protectorate */
		private int protectorate;
	/** start surama event / start surama event */
		private boolean startSuramaEvent;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		// 卡伦【雷安领袖】 / Kahrun [Reian Leader]
		spawn(800456, 1577.6334f, 1055.3899f, 492.0681f, (byte) 26);
		// 拉克夏卡上校 I / Laksyaka Colonel I
		spawn(219363, 763.0683f, 1446.5114f, 495.65198f, (byte) 90);
        spawn(219363, 763.0781f, 1191.3202f, 495.64584f, (byte) 30);
		spawn(219363, 893.29297f, 1190.8209f, 495.6457f, (byte) 30);
		spawn(219363, 893.3504f, 1446.1143f, 495.64215f, (byte) 91);
		spawn(730612, 1544.2832f, 1068.4409f, 489.32297f, (byte) 0, 55);
		spawn(730694, 1542.2600f, 1068.5900f, 491.32999f, (byte) 0, 35);
		doors = instance.getDoors();
		switch (Rnd.get(1, 4)) {
			case 1:
				deleteNpc(219363);
				spawn(219364, 763.0683f, 1446.5114f, 495.65198f, (byte) 90);
			break;
			case 2:
				deleteNpc(219363);
				spawn(219364, 763.0781f, 1191.3202f, 495.64584f, (byte) 30);
			break;
			case 3:
				deleteNpc(219363);
				spawn(219364, 893.29297f, 1190.8209f, 495.6457f, (byte) 30);
			break;
			case 4:
				deleteNpc(219363);
				spawn(219364, 893.3504f, 1446.1143f, 495.64215f, (byte) 91);
			break;
		}
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				protectorateFirstWave();
			}
		}, 60000);
	}
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 219352: //Invincible Shabokan.
			case 219353: //Brigade General Chantra.
			case 219355: //Traitor Kumbanda.
			case 219356: //Brigade General Laksyaka.
			case 219357: //Adjudant Anuhart.
			case 701541: //Brigade General Tahabata Chest.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053208, 1)); //Tiamat Guard's Eternal Armor Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					}
				}
			break;
			case 219354: //Brigade General Terath.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053208, 1)); //Tiamat Guard's Eternal Armor Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 3)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 100001398, 1)); //Terath's Sword.
				        break;
						case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 100501087, 1)); //Terath's Orb.
				        break;
					    case 3:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 101301030, 1)); //Terath's Polearm.
				        break;
					}
				}
			break;
			case 219392: //Laksyaka Elite Guard Captain.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000120, 1)); //Military Ward Key.
			break;
			case 219364: //Laksyaka Colonel.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000121, 1)); //Eternal Prison Key.
			break;
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 701501: //Balaur Medal Box.
			case 701527: //Stronghold Treasure Chest.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 186000201, 5)); //Protectorate Coin.
					}
				}
			break;
			case 802179: //Tiamat Stronghold Opportunity Bundle.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000051, 30)); //Major Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000052, 30)); //Greater Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000236, 50)); //Blood Mark.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000237, 50)); //Ancient Coin.
			break;
		}
	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * 玩家 / player
	 * npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 701523: //Research Center Acces Controler.
				doors.get(22).setOpen(true);
			break;
			case 701495:
			break;
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
			case 219369: //Protectorate Elite Fighter.
			case 219370: //Protectorate Elite Infantryman.
			case 219371: //Protectorate Elite Scout.
			case 219372: //Protectorate Elite Healer.
			case 219373: //Protectorate Elite Mounted Officer.
				protectorate++;
				if (protectorate == 4) {
				    protectorateSecondWave();
				} else if (protectorate == 8) {
				   protectorateThirdWave();
				}
			break;
		    case 219352: //Invincible Shabokan.
				// 贵族花园中出现了龙族勋章箱。 / A Balaur Medal Chest appeared in the Noble's Garden.
			    sendMsgByRace(1401614, Race.PC_ALL, 2000);
				spawn(701501, 1075.4409f, 1078.5071f, 787.685f, (byte) 16);
				spawn(283177, 1178.7662f, 1068.971f, 500.6963f, (byte) 0); //Tiamat Eye.
				Npc tiamatEye1 = getNpc(283177);
				// 无礼的守护者！竟敢摧毁我的副官！ / Insolent Daevas! Destroying my lieutenants!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye1, 1500679, tiamatEye1.getObjectId(), 0, 3000);
				// 拉克夏卡对我很有用。你会看到夺走我工具者的下场。 / Laksyaka was useful to me. You'll see what happens to those who take away my tools.
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye1, 1500680, tiamatEye1.getObjectId(), 0, 9000);
				// 我的副官临死所受痛苦，你将感受十倍！ / Whatever agony my lieutenants felt as they died, you will feel tenfold!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye1, 1500681, tiamatEye1.getObjectId(), 0, 15000);
				// 亲爱的龙主，请安息。让我为你复仇！ / Dear Dragon Lord, please rest in peace. Let me avenge you!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye1, 1500682, tiamatEye1.getObjectId(), 0, 21000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
						deleteNpc(283177);
						doors.get(48).setOpen(true);
						doors.get(369).setOpen(true);
						doors.get(706).setOpen(true);
						spawnStrongholdGatewaySecure();
				    }
			    }, 22000);
		    break;
		    case 219355: //Traitor Kumbanda.
				// 贵族花园中出现了龙族勋章箱。 / A Balaur Medal Chest appeared in the Noble's Garden.
				sendMsgByRace(1401614, Race.PC_ALL, 2000);
				spawn(701501, 1063.5973f, 1092.7402f, 787.685f, (byte) 107);
		    break;
			case 219356: //Brigade General Laksyaka.
			    spawn(283178, 628.3988f, 1319.0686f, 494.79846f, (byte) 119); //Tiamat Eye.
				Npc tiamatEye2 = getNpc(283178);
				// 无礼的守护者！竟敢摧毁我的副官！ / Insolent Daevas! Destroying my lieutenants!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye2, 1500679, tiamatEye2.getObjectId(), 0, 3000);
				// 拉克夏卡对我很有用。你会看到夺走我工具者的下场。 / Laksyaka was useful to me. You'll see what happens to those who take away my tools.
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye2, 1500680, tiamatEye2.getObjectId(), 0, 9000);
				// 我的副官临死所受痛苦，你将感受十倍！ / Whatever agony my lieutenants felt as they died, you will feel tenfold!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye2, 1500681, tiamatEye2.getObjectId(), 0, 15000);
				// 亲爱的龙主，请安息。让我为你复仇！ / Dear Dragon Lord, please rest in peace. Let me avenge you!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye2, 1500682, tiamatEye2.getObjectId(), 0, 21000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
						deleteNpc(283178);
						spawn(730622, 644.15436f, 1319.6982f, 487.51764f, (byte) 0, 15); //Central Transport Passage.
				    }
			    }, 22000);
			break;
		    case 219357: //Adjudant Anuhart.
				deleteNpc(283099); //Blade Storm.
				doors.get(37).setOpen(true);
			    doors.get(610).setOpen(true);
				// 贵族花园中出现了龙族勋章箱。 / A Balaur Medal Chest appeared in the Noble's Garden.
				sendMsgByRace(1401614, Race.PC_ALL, 2000);
				spawn(701501, 1077.1716f, 1058.1995f, 787.685f, (byte) 61);
		    break;
		    case 219358: //Brigade General Tahabata.
/* 			    switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 678.0355f, 1071.9641f, 497.75186f, (byte) 85); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 678.0355f, 1071.9641f, 497.75186f, (byte) 85); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				// 贵族花园中出现了龙族勋章箱。 / A Balaur Medal Chest appeared in the Noble's Garden.
				sendMsgByRace(1401614, Race.PC_ALL, 2000);
				spawn(701541, 677.35785f, 1069.5361f, 499.86716f, (byte) 0);
				spawn(283178, 644.7906f, 1068.6279f, 506.9512f, (byte) 119); //Tiamat Eye.
				Npc tiamatEye3 = getNpc(283178);
				// 无礼的守护者！竟敢摧毁我的副官！ / Insolent Daevas! Destroying my lieutenants!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye3, 1500679, tiamatEye3.getObjectId(), 0, 3000);
				// 拉克夏卡对我很有用。你会看到夺走我工具者的下场。 / Laksyaka was useful to me. You'll see what happens to those who take away my tools.
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye3, 1500680, tiamatEye3.getObjectId(), 0, 9000);
				// 我的副官临死所受痛苦，你将感受十倍！ / Whatever agony my lieutenants felt as they died, you will feel tenfold!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye3, 1500681, tiamatEye3.getObjectId(), 0, 15000);
				// 亲爱的龙主，请安息。让我为你复仇！ / Dear Dragon Lord, please rest in peace. Let me avenge you!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye3, 1500682, tiamatEye3.getObjectId(), 0, 21000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
						deleteNpc(283178);
						spawn(730622, 652.39172f, 1068.9602f, 497.58624f, (byte) 0, 82); //Central Transport Passage.
				    }
			    }, 22000);
		    break;
		    case 219353: //Brigade General Chantra.
			    doors.get(711).setOpen(true);
				// 贵族花园中出现了龙族勋章箱。 / A Balaur Medal Chest appeared in the Noble's Garden.
				sendMsgByRace(1401614, Race.PC_ALL, 2000);
				spawn(701541, 677.35785f, 1069.5361f, 499.86716f, (byte) 0);
		    break;
		    case 219354: //Brigade General Terath.
				spawnStrongholdTreasureChest();
				// 贵族花园中出现了龙族勋章箱。 / A Balaur Medal Chest appeared in the Noble's Garden.
				sendMsgByRace(1401614, Race.PC_ALL, 2000);
				spawn(701541, 677.35785f, 1069.5361f, 499.86716f, (byte) 0);
				sendMsg("Congratulation]: you finish <Tiamat Stronghold>");
				spawn(283180, 1029.9412f, 266.9634f, 416.4337f, (byte) 28); //Tiamat Eye.
				spawn(800464, 1119.9684f, 1069.8219f, 496.86157f, (byte) 0); //Reian Sorcerer.
				spawn(800464, 1120.9542f, 1068.1592f, 496.86157f, (byte) 15); //Reian Sorcerer.
				spawn(800465, 1120.6633f, 1071.6091f, 496.86157f, (byte) 112); //Reian Sorcerer.
				spawn(802179, 1030.0757f, 271.60504f, 409.08588f, (byte) 30); //Tiamat Stronghold Opportunity Bundle.
				spawn(730629, 1121.5267f, 1069.9308f, 500.24982f, (byte) 0, 555); //Central Transport Passage.
				Npc tiamatEye4 = getNpc(283180);
				// 无礼的守护者！竟敢摧毁我的副官！ / Insolent Daevas! Destroying my lieutenants!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye4, 1500679, tiamatEye4.getObjectId(), 0, 3000);
				// 拉克夏卡对我很有用。你会看到夺走我工具者的下场。 / Laksyaka was useful to me. You'll see what happens to those who take away my tools.
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye4, 1500680, tiamatEye4.getObjectId(), 0, 9000);
				// 我的副官临死所受痛苦，你将感受十倍！ / Whatever agony my lieutenants felt as they died, you will feel tenfold!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye4, 1500681, tiamatEye4.getObjectId(), 0, 15000);
				// 亲爱的龙主，请安息。让我为你复仇！ / Dear Dragon Lord, please rest in peace. Let me avenge you!
				GameFeatureServices.npcShoutsService().sendMsg(tiamatEye4, 1500682, tiamatEye4.getObjectId(), 0, 21000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
						deleteNpc(283180);
						spawn(730622, 1029.7988f, 267.03915f, 408.98651f, (byte) 0, 83); //Central Passage Teleport.
					}
				}, 22000);
		    break;
		}
	}
	
	private void spawnStrongholdTreasureChest() {
		spawn(701527, 1074.0504f, 1068.9313f, 785.9529f, (byte) 118);
	}
	
	private void raidProtectorate(final Npc npc, float x, float y, float z, boolean despawn) {
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}
	
	private void protectorateFirstWave() {
		raidProtectorate((Npc)spawn(219369, 1509.0016f, 1051.2535f, 491.42322f, (byte) 5), 1556.6959f, 1063.507f, 492.16028f, false); //Protectorate Elite Fighter.
		raidProtectorate((Npc)spawn(219370, 1508.4454f, 1085.6877f, 491.35217f, (byte) 118), 1556.8972f, 1072.9952f, 492.16028f, false); //Protectorate Elite Infantryman.
		raidProtectorate((Npc)spawn(219371, 1508.9395f, 1061.8333f, 491.48456f, (byte) 119), 1555.9574f, 1066.3362f, 492.16028f, false); //Protectorate Elite Scout.
		raidProtectorate((Npc)spawn(219372, 1508.3008f, 1073.0205f, 491.48438f, (byte) 3), 1555.0793f, 1070.4535f, 492.16028f, false); //Protectorate Elite Healer.
	}
	
	private void protectorateSecondWave() {
		raidProtectorate((Npc)spawn(219369, 1509.0016f, 1051.2535f, 491.42322f, (byte) 5), 1556.6959f, 1063.507f, 492.16028f, false); //Protectorate Elite Fighter.
		raidProtectorate((Npc)spawn(219370, 1508.4454f, 1085.6877f, 491.35217f, (byte) 118), 1556.8972f, 1072.9952f, 492.16028f, false); //Protectorate Elite Infantryman.
		raidProtectorate((Npc)spawn(219371, 1508.9395f, 1061.8333f, 491.48456f, (byte) 119), 1555.9574f, 1066.3362f, 492.16028f, false); //Protectorate Elite Scout.
		raidProtectorate((Npc)spawn(219372, 1508.3008f, 1073.0205f, 491.48438f, (byte) 3), 1555.0793f, 1070.4535f, 492.16028f, false); //Protectorate Elite Healer.
	}
	
	private void protectorateThirdWave() {
		raidProtectorate((Npc)spawn(219370, 1509.0016f, 1051.2535f, 491.42322f, (byte) 5), 1556.6959f, 1063.507f, 492.16028f, false); //Protectorate Elite Infantryman.
		raidProtectorate((Npc)spawn(219372, 1508.4454f, 1085.6877f, 491.35217f, (byte) 118), 1556.8972f, 1072.9952f, 492.16028f, false); //Protectorate Elite Healer.
		raidProtectorate((Npc)spawn(219373, 1508.9395f, 1061.8333f, 491.48456f, (byte) 119), 1555.9574f, 1066.3362f, 492.16028f, false); //Protectorate Elite Mounted Officer.
		raidProtectorate((Npc)spawn(219373, 1508.3008f, 1073.0205f, 491.48438f, (byte) 3), 1555.0793f, 1070.4535f, 492.16028f, false); //Protectorate Elite Mounted Officer.
	}
	
	private void moveToForward(final Npc npc, float x, float y, float z, boolean despawn) {
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
		if (despawn) {
		    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			    /**
			     * 处理 run。
			     * Handle run.
			     */
			    @Override
			    public void run() {
			  	    if (npc.getNpcId() == 800336) {
			  		    spawn(800338, 1104f, 1069f, 497f, (byte) 61);
			  		    Npc kahrun = getNpc(800338);
						Npc garnon = getNpc(800347);
						// 雷安士兵将守住此区域。你必须继续前进。快！ / The Reian Soldiers will secure this area. You must press on. Hurry!
					    GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500597, kahrun.getObjectId(), 0, 5000);
					    // 令人印象深刻！我以为守门人会更难对付。 / I'm impressed! I thought the gatekeeper would be a greater challenge.
						// 看来我们选对了冠军。 / I see we chose our champions well.
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500598, kahrun.getObjectId(), 0, 15000);
						// 更深处有增援。我们必须对付他们。 / There are reinforcements further in. We must deal with them.
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500599, kahrun.getObjectId(), 0, 25000);
					    // 你还在等什么？快去！ / What are you waiting for? Get going!
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500600, kahrun.getObjectId(), 0, 35000);
						// 这说不通。一定有办法从这里到达提亚马特的王座！ / This makes no sense. There must be a way to get to Tiamat's throne from here!
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500601, kahrun.getObjectId(), 0, 45000);
						// 加农？你怎么到这儿的？ / Garnon? How did you get here?
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500602, kahrun.getObjectId(), 0, 55000);
						// 你找到了？它到底在哪？ / You've found it? Where in the world was it?
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500603, kahrun.getObjectId(), 0, 65000);
						// 别浪费时间。最终之战即将到来。 / Let's not waste time. The final battle is upon us.
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500604, kahrun.getObjectId(), 0, 75000);
						// 卡伦大人！ / Lord Kahrun!
						GameFeatureServices.npcShoutsService().sendMsg(garnon, 1500605, garnon.getObjectId(), 0, 85000);
						// 我们发现了龙主避难所的入口！ / We've found the entrance to the Dragon Lord's Refuge!
						GameFeatureServices.npcShoutsService().sendMsg(garnon, 1500606, garnon.getObjectId(), 0, 95000);
						// 我们派往提亚玛兰塔之眼的侦察兵才是你该问的人。 / The scouts we sent to Tiamaranta's Eye are the ones you should be asking.
						GameFeatureServices.npcShoutsService().sendMsg(garnon, 1500607, garnon.getObjectId(), 0, 105000);
						// 提亚马特。交出希尔遗物并离开提亚玛兰塔。 / Tiamat. Hand over Siel's Relics and leave Tiamaranta.
						GameFeatureServices.npcShoutsService().sendMsg(garnon, 1500608, garnon.getObjectId(), 0, 115000);
						// 我的部队正在赶来。 / My forces are on their way.
						// 你真要与我们全面开战？这选择将给双方带来悲剧。 / Do you truly wish to face us in all-out battle? Such a choice will lead to tragedy for us all.
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500609, kahrun.getObjectId(), 0, 125000);
						// 那么，你选择了毁灭之路。现在你将见识雷安部族的真正力量。 / Well then, you've chosen the path of destruction. Now you will see the true power of the Reian Tribe
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500610, kahrun.getObjectId(), 0, 135000);
						// 啊啊啊！希……希尔的遗物…… / Aarrghh! Si...Siel's Relics...
						// 他们比我们想象的更强！快跑！别独自面对！ / They are more powerful than we ever imagined! Run! You must not try to face this alone.
						GameFeatureServices.npcShoutsService().sendMsg(kahrun, 1500611, kahrun.getObjectId(), 0, 145000);
			  	    }
				    npc.getController().onDelete();
			    }
		    }, 13000);
		}
	}
	
	private void spawnStrongholdGatewaySecure() {
		moveToForward((Npc)spawn(800463, 1201.272f, 1072.5137f, 491f, (byte) 61), 1130, 1072, 497.3f, false);
		moveToForward((Npc)spawn(800463, 1192.8656f, 1071.1085f, 491f, (byte) 61), 1112, 1070, 497, false);
		moveToForward((Npc)spawn(800463, 1208.4175f, 1071.1797f, 491f, (byte) 61), 1133, 1072.5f, 497.3f, false);
		moveToForward((Npc)spawn(800463, 1192.8656f, 1068.3411f, 491f, (byte) 61), 1114, 1067, 496.7f, false);
		moveToForward((Npc)spawn(800463, 1208.4175f, 1068.3979f, 491f, (byte) 61), 1133.32f, 1066.47f, 497.3f, false);
		moveToForward((Npc)spawn(800463, 1201.272f, 1066.2085f, 491f, (byte) 61), 1128.8f, 1067, 497.3f, false);
		moveToForward((Npc)spawn(800380, 1190.323f, 1068.1558f, 491.03488f, (byte) 61), 1108, 1066, 497.3f, false);
		moveToForward((Npc)spawn(800374, 1188.4259f, 1066.4757f, 491.55029f, (byte) 61), 1094, 1064, 497.4f, true);
		moveToForward((Npc)spawn(800374, 1188.2158f, 1074.2047f, 491.55029f, (byte) 61), 1092.5f, 1074.6f, 497.4f, true);
		moveToForward((Npc)spawn(800376, 1190.3859f, 1071.6548f, 491.03488f, (byte) 61), 1109, 1073, 497.2f, false);
		moveToForward((Npc)spawn(800461, 1184.7582f, 1068.6f, 491.03488f, (byte) 61), 1111, 1068.6f, 497.33f, false);
		moveToForward((Npc)spawn(800347, 1178.0425f, 1072.28f, 491.02545f, (byte) 61), 1106, 1072, 497.2f, false);
		moveToForward((Npc)spawn(800336, 1178.0559f, 1069.6f, 491.02545f, (byte) 61), 1104, 1069, 497, true);
	}
	
	/**
	 * 玩家进入区域时处理。
	 * Handle a player entering a zone.
	 *
	 * 玩家 / player
	 * zone
	 */
	@Override
	public void onEnterZone(Player player, ZoneInstance zone) {
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("LAKSYAKA_LEGION_HQ_300510000")) {
			if (!startSuramaEvent) {
				startSuramaEvent = true;
				spawn(800433, 725.93f, 1319.9f, 490.7f, (byte) 61);
			}
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
	/**
	 * 返回 npc。
	 * Return the npc.
	 *
	 * NPC
	 * result
	 */
	
	protected Npc getNpc(int npcId) {
		if (!isInstanceDestroyed) {
			return instance.getNpc(npcId);
		}
		return null;
	}
	
	private boolean isDead(Npc npc) {
		return (npc == null || npc.getLifeStats().isAlreadyDead());
	}
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		doors.clear();
		isInstanceDestroyed = true;
	}
}