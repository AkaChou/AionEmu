package com.aionemu.gameserver.instance.handlers.scripts.illuminaryObelisk;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 辉耀方尖碑副本事件处理器。
 * Instance event handler for Illuminary Obelisk.
 *
 * @author Encom
 */

@InstanceID(301230000)
public class IlluminaryObeliskInstance extends GeneralInstanceHandler
{
	/** 开始时间 / start time */
	private long startTime;
	/** 视频种族 / video race */
		private Race videoRace;
	/** illuminary wave / illuminary wave */
		private int illuminaryWave;
	/** 副本计时器 / instance timer */
		private Future<?> instanceTimer;
	// 东部护盾波次。 / Eastern Shield Wave.
	/** eastern 任务 e1 / eastern task e1 */
		private Future<?> easternTaskE1;
	/** eastern 任务 e2 / eastern task e2 */
		private Future<?> easternTaskE2;
	/** eastern 任务 e3 / eastern task e3 */
		private Future<?> easternTaskE3;
	/** eastern 任务 e4 / eastern task e4 */
		private Future<?> easternTaskE4;
	// 西部护盾波次。 / Western Shield Wave.
	/** western 任务 w1 / western task w1 */
		private Future<?> westernTaskW1;
	/** western 任务 w2 / western task w2 */
		private Future<?> westernTaskW2;
	/** western 任务 w3 / western task w3 */
		private Future<?> westernTaskW3;
	/** western 任务 w4 / western task w4 */
		private Future<?> westernTaskW4;
	// 南部护盾波次。 / Southern Shield Wave.
	/** southern 任务 s1 / southern task s1 */
		private Future<?> southernTaskS1;
	/** southern 任务 s2 / southern task s2 */
		private Future<?> southernTaskS2;
	/** southern 任务 s3 / southern task s3 */
		private Future<?> southernTaskS3;
	/** southern 任务 s4 / southern task s4 */
		private Future<?> southernTaskS4;
	// 北部护盾波次。 / Northern Shield Wave.
	/** northern 任务 n1 / northern task n1 */
		private Future<?> northernTaskN1;
	/** northern 任务 n2 / northern task n2 */
		private Future<?> northernTaskN2;
	/** northern 任务 n3 / northern task n3 */
		private Future<?> northernTaskN3;
	/** northern 任务 n4 / northern task n4 */
		private Future<?> northernTaskN4;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
	/** illuminary task1 / illuminary task1 */
		private final List<Future<?>> illuminaryTask1 = new ArrayList<>();
	/** illuminary task2 / illuminary task2 */
		private final List<Future<?>> illuminaryTask2 = new ArrayList<>();
	/** illuminary task3 / illuminary task3 */
		private final List<Future<?>> illuminaryTask3 = new ArrayList<>();
	/** illuminary task4 / illuminary task4 */
		private final List<Future<?>> illuminaryTask4 = new ArrayList<>();
	
   /**
	 * 奖励：成功捕获 Boss 后有几率获得…… / Reward: After a successful capture of the boss you will get a small chance of obtaining mythical wings, and a variety of items. Boxes are for all the members and the wings only for one person in the group
	 */
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 702018: //Supply Box.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053100, 1)); //Pure Dynatoum's Equipment Crux Box.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052830, 1)); //Dynatoum's Brazen Weapon Box.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052831, 1)); //Dynatoum's Brazen Armor Box.
				        break;
					}
				}
			break;
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
		   /**
	 * 每台“护盾发生器”需要 3 个理念物品，共 12 个，可在副本各处找到 / Each "Shield Generator" unit needs 3 ide items, 12 items in total, you can find them all around the instance
	 */
			case 730884: //Flourishing Idium.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000289, 3));
			break;
		   /**
	 * 用于大炮的炸弹出现在副本各处的箱子中，位置每次不同，请一并收集 / Bombs to use the cannons appear in chests around the instance in a different place every time, collect them too
	 */
			case 730885: //Danuar Cannonballs.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000290, 3));
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
	
	private void startIlluminaryTimer() {
		// 弱化防护盾将在 30 分钟后消失。 / The weakened protective shield will disappear in 30 minutes.
		this.sendMessage(1402129, 1 * 60 * 1000);
		// 弱化防护盾将在 25 分钟后消失。 / The weakened protective shield will disappear in 25 minutes.
		this.sendMessage(1402130, 5 * 60 * 1000);
		// 弱化防护盾将在 20 分钟后消失。 / The weakened protective shield will disappear in 20 minutes.
		this.sendMessage(1402131, 10 * 60 * 1000);
		// 弱化防护盾将在 15 分钟后消失。 / The weakened protective shield will disappear in 15 minutes.
		this.sendMessage(1402132, 15 * 60 * 1000);
		// 弱化防护盾将在 10 分钟后消失。 / The weakened protective shield will disappear in 10 minutes.
		this.sendMessage(1402133, 20 * 60 * 1000);
		// 弱化防护盾将在 5 分钟后消失。 / The weakened protective shield will disappear in 5 minutes.
		this.sendMessage(1402134, 25 * 60 * 1000);
		// 弱化防护盾将在 1 分钟后消失。 / The weakened protective shield will disappear in 1 minute.
		this.sendMessage(1402235, 29 * 60 * 1000);
		// 覆盖光明方尖碑的防护盾已消失。帕希德破坏部队开始猛烈轰炸。 / The protective shield covering the Illuminary Obelisk has disappeared. The Pashid Destruction Unit's intense bombing commences.
		this.sendMessage(1402236, 30 * 60 * 1000);
		// 迪纳图姆摧毁了护盾生成中枢的传送装置。 / The Dynatoum has destroyed the teleport device of the shield generation hub.
		this.sendMessage(1402212, 31 * 60 * 1000);
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
		if (instanceTimer == null) {
			startTime = System.currentTimeMillis();
			instanceTimer = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					startIlluminaryTimer();
					doors.get(129).setOpen(true);
				}
			}, 30000); //...30Sec
		}
		final int illuminaryVideo = videoRace == Race.ASMODIANS ? 895 : 894;
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, illuminaryVideo));
	}
	
   /**
	 * 护盾单元：建议在……后开始为护盾充能。 / Shield Units: Its a good idea to start powering the shields once you have 6 ide shield items. Help collect the remaining pieces together in the other bridges. Once you charge a shield with one of the items, a wave of monster will appear, help that person and kill the mobs. Protect the shield units from monsters while you charge them up to the 3rd phase. Once all shields are at the 3rd phase no more monsters will spawn
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 702010: //Eastern Shield Generator.
			    if (player.getInventory().decreaseByItemId(164000289, 3)) {
					startEasternTask();
					startEasternShield1();
					// 东部护盾能量发生器附近开启了欧比斯之门。 / An Abyss Gate has opened near the eastern power shield generator.
					// 帕希德破坏部队渗透进行中。 / Infiltration by Pashid Destruction Unit is underway.
					sendMsgByRace(1402224, Race.PC_ALL, 1000);
					spawn(702014, 255.7926f, 338.22058f, 325.56473f, (byte) 0, 60); //Pashid Infiltration Gate.
				} else {
					// 需要结晶伊迪姆碎片为发生器充能。 / You need a Crystalline Idium Piece to charge the generator.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402211));
				}
			break;
			case 702011: //Western Shield Generator.
			    if (player.getInventory().decreaseByItemId(164000289, 3)) {
					startWesternTask();
					startWesternShield1();
					// 西部护盾能量发生器附近开启了欧比斯之门。 / An Abyss Gate has opened near the western power shield generator.
					// 帕希德破坏部队渗透进行中。 / Infiltration by Pashid Destruction Unit is underway.
					sendMsgByRace(1402225, Race.PC_ALL, 1000);
					spawn(702015, 255.7034f, 171.83853f, 325.81653f, (byte) 0, 18); //Pashid Infiltration Gate.
				} else {
					// 需要结晶伊迪姆碎片为发生器充能。 / You need a Crystalline Idium Piece to charge the generator.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402211));
				}
			break;
			case 702012: //Southern Shield Generator.
			    if (player.getInventory().decreaseByItemId(164000289, 3)) {
					startSouthernTask();
					startSouthernShield1();
					// 南部护盾能量发生器附近开启了欧比斯之门。 / An Abyss Gate has opened near the southern power shield generator.
					// 帕希德破坏部队渗透进行中。 / Infiltration by Pashid Destruction Unit is underway.
					sendMsgByRace(1402226, Race.PC_ALL, 1000);
					spawn(702016, 343.12021f, 254.10585f, 291.62302f, (byte) 0, 34); //Pashid Infiltration Gate.
				} else {
					// 需要结晶伊迪姆碎片为发生器充能。 / You need a Crystalline Idium Piece to charge the generator.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402211));
				}
			break;
			case 702013: //Northern Shield Generator.
			    if (player.getInventory().decreaseByItemId(164000289, 3)) {
					startNorthernTask();
					startNorthernShield1();
					// 北部护盾能量发生器附近开启了欧比斯之门。 / An Abyss Gate has opened near the northern power shield generator.
					// 帕希德破坏部队渗透进行中。 / Infiltration by Pashid Destruction Unit is underway.
					sendMsgByRace(1402227, Race.PC_ALL, 1000);
					spawn(702017, 169.55626f, 254.52907f, 293.04276f, (byte) 0, 17); //Pashid Infiltration Gate.
				} else {
					// 需要结晶伊迪姆碎片为发生器充能。 / You need a Crystalline Idium Piece to charge the generator.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402211));
				}
			break;
			case 730886: //Shield Control Room Teleporter.
				instance.doOnAllPlayers(new Visitor<Player>() {
					/**
					 * 处理 visit。
					 * Handle visit.
					 *
					 * @param player 玩家 / player
					 */
					@Override
					public void visit(Player player) {
						if (player.isOnline()) {
							illuminaryToDynatoum(player);
						}
					}
				});
			break;
		   /**
	 * 防御加农：每个护盾单元配有防御加农。 / Defense Cannons: Each Shield Unit has a defense cannon that can be used. This cannons do powerful wide area damage attacks. In order to use them you need to have Bomb items. When a shield is charged completely a cannon will spawn to help in the defense of the area. Determining a person to use the cannon and positioning before the mobs come is a recommended. Bombs to use the cannons appear in chests around the instance in a different place every time, collect them too
	 */
			case 702009: //Danuar Cannon.
			case 702021: //Danuar Cannon.
			case 702022: //Danuar Cannon.
			case 702023: //Danuar Cannon.
			    despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21511, 60, player).useNoAnimationSkill();
			break;
		}
	}
	
   /**
	 * 若“护盾”被摧毁，须从第 1 阶段重新开始。 / If a "Shield" is destroyed, you must start again from the 1st phase You can heal the shield with a restoration skill
	 */
	@Override
    public void onDie(Npc npc) {
        Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 702010: //Eastern Shield Generator.
		        despawnNpc(npc);
				stopInstanceTask1();
				deleteNpc(702014); //Eastern Pashid Infiltration Gate.
				deleteNpc(702218); //Eastern Defence Charge 01.
				deleteNpc(702219); //Eastern Defence Charge 02.
				deleteNpc(702220); //Eastern Defence Charge 03.
				killNpc(getNpcs(233720)); //Pashid Destruction Unit Combatant.
			    killNpc(getNpcs(233721)); //Pashid Destruction Unit Ambusher.
			    killNpc(getNpcs(233722)); //Pashid Destruction Unit Mage.
				killNpc(getNpcs(233723)); //Pashid Destruction Unit Beastmaster.
				killNpc(getNpcs(233724)); //Pashid Destruction Unit Beastmaster.
				killNpc(getNpcs(233725)); //Pashid Destruction Unit Rearguard.
				killNpc(getNpcs(233726)); //Pashid Destruction Unit Striker.
				killNpc(getNpcs(233727)); //Pashid Destruction Unit Drummer.
				killNpc(getNpcs(233728)); //Pashid Destruction Unit Elite Combatant.
				killNpc(getNpcs(233729)); //Pashid Destruction Unit Elite Ambusher.
				killNpc(getNpcs(233730)); //Pashid Destruction Unit Elite Mage.
				killNpc(getNpcs(233731)); //Pashid Destruction Unit Elite Beastmaster.
				killNpc(getNpcs(233732)); //Pashid Destruction Unit Elite Healer.
				killNpc(getNpcs(233733)); //Pashid Destruction Unit Elite Rearguard.
				killNpc(getNpcs(233734)); //Pashid Destruction Unit Elite Striker.
				// 东部护盾能量发生器已被摧毁。 / The eastern shield power generator has been destroyed.
				sendMsgByRace(1402139, Race.PC_ALL, 0);
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						spawn(702010, 255.47392f, 293.56177f, 321.18497f, (byte) 89); //Eastern Shield Generator.
				    }
			    }, 10000);
			break;
		    case 702011: //Western Shield Generator.
		        despawnNpc(npc);
				stopInstanceTask2();
				deleteNpc(702015); //Western Pashid Infiltration Gate.
				deleteNpc(702221); //Western Defence Charge 01.
				deleteNpc(702222); //Western Defence Charge 02.
				deleteNpc(702223); //Western Defence Charge 03.
				killNpc(getNpcs(233720)); //Pashid Destruction Unit Combatant.
			    killNpc(getNpcs(233721)); //Pashid Destruction Unit Ambusher.
			    killNpc(getNpcs(233722)); //Pashid Destruction Unit Mage.
				killNpc(getNpcs(233723)); //Pashid Destruction Unit Beastmaster.
				killNpc(getNpcs(233724)); //Pashid Destruction Unit Beastmaster.
				killNpc(getNpcs(233725)); //Pashid Destruction Unit Rearguard.
				killNpc(getNpcs(233726)); //Pashid Destruction Unit Striker.
				killNpc(getNpcs(233727)); //Pashid Destruction Unit Drummer.
				killNpc(getNpcs(233728)); //Pashid Destruction Unit Elite Combatant.
				killNpc(getNpcs(233729)); //Pashid Destruction Unit Elite Ambusher.
				killNpc(getNpcs(233730)); //Pashid Destruction Unit Elite Mage.
				killNpc(getNpcs(233731)); //Pashid Destruction Unit Elite Beastmaster.
				killNpc(getNpcs(233732)); //Pashid Destruction Unit Elite Healer.
				killNpc(getNpcs(233733)); //Pashid Destruction Unit Elite Rearguard.
				killNpc(getNpcs(233734)); //Pashid Destruction Unit Elite Striker.
				// 西部护盾能量发生器已被摧毁。 / The western shield power generator has been destroyed.
				sendMsgByRace(1402140, Race.PC_ALL, 0);
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						spawn(702011, 255.55742f, 216.03549f, 321.21344f, (byte) 30); //Western Shield Generator.
				    }
			    }, 10000);
			break;
		    case 702012: //Southern Shield Generator.
		        despawnNpc(npc);
				stopInstanceTask3();
				deleteNpc(702016); //Southern Pashid Infiltration Gate.
				deleteNpc(702224); //Southern Defence Charge 01.
				deleteNpc(702225); //Southern Defence Charge 02.
				deleteNpc(702226); //Southern Defence Charge 03.
				killNpc(getNpcs(233720)); //Pashid Destruction Unit Combatant.
			    killNpc(getNpcs(233721)); //Pashid Destruction Unit Ambusher.
			    killNpc(getNpcs(233722)); //Pashid Destruction Unit Mage.
				killNpc(getNpcs(233723)); //Pashid Destruction Unit Beastmaster.
				killNpc(getNpcs(233724)); //Pashid Destruction Unit Beastmaster.
				killNpc(getNpcs(233725)); //Pashid Destruction Unit Rearguard.
				killNpc(getNpcs(233726)); //Pashid Destruction Unit Striker.
				killNpc(getNpcs(233727)); //Pashid Destruction Unit Drummer.
				killNpc(getNpcs(233728)); //Pashid Destruction Unit Elite Combatant.
				killNpc(getNpcs(233729)); //Pashid Destruction Unit Elite Ambusher.
				killNpc(getNpcs(233730)); //Pashid Destruction Unit Elite Mage.
				killNpc(getNpcs(233731)); //Pashid Destruction Unit Elite Beastmaster.
				killNpc(getNpcs(233732)); //Pashid Destruction Unit Elite Healer.
				killNpc(getNpcs(233733)); //Pashid Destruction Unit Elite Rearguard.
				killNpc(getNpcs(233734)); //Pashid Destruction Unit Elite Striker.
				// 南部护盾能量发生器已被摧毁。 / The southern shield power generator has been destroyed.
				sendMsgByRace(1402141, Race.PC_ALL, 0);
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						spawn(702012, 294.20718f, 254.60352f, 295.7729f, (byte) 60); //Southern Shield Generator.
				    }
			    }, 10000);
			break;
		    case 702013: //Northern Shield Generator.
		        despawnNpc(npc);
				stopInstanceTask4();
				deleteNpc(702017); //Northern Pashid Infiltration Gate.
				deleteNpc(702227); //Northern Defence Charge 01.
				deleteNpc(702228); //Northern Defence Charge 02.
				deleteNpc(702229); //Northern Defence Charge 03.
				killNpc(getNpcs(233720)); //Pashid Destruction Unit Combatant.
			    killNpc(getNpcs(233721)); //Pashid Destruction Unit Ambusher.
			    killNpc(getNpcs(233722)); //Pashid Destruction Unit Mage.
				killNpc(getNpcs(233723)); //Pashid Destruction Unit Beastmaster.
				killNpc(getNpcs(233724)); //Pashid Destruction Unit Beastmaster.
				killNpc(getNpcs(233725)); //Pashid Destruction Unit Rearguard.
				killNpc(getNpcs(233726)); //Pashid Destruction Unit Striker.
				killNpc(getNpcs(233727)); //Pashid Destruction Unit Drummer.
				killNpc(getNpcs(233728)); //Pashid Destruction Unit Elite Combatant.
				killNpc(getNpcs(233729)); //Pashid Destruction Unit Elite Ambusher.
				killNpc(getNpcs(233730)); //Pashid Destruction Unit Elite Mage.
				killNpc(getNpcs(233731)); //Pashid Destruction Unit Elite Beastmaster.
				killNpc(getNpcs(233732)); //Pashid Destruction Unit Elite Healer.
				killNpc(getNpcs(233733)); //Pashid Destruction Unit Elite Rearguard.
				killNpc(getNpcs(233734)); //Pashid Destruction Unit Elite Striker.
				// 北部护盾能量发生器已被摧毁。 / The northern shield power generator has been destroyed.
				sendMsgByRace(1402142, Race.PC_ALL, 0);
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						spawn(702013, 216.97739f, 254.4616f, 295.77353f, (byte) 0); //Northern Shield Generator.
				    }
			    }, 10000);
			break;
			case 233720: //Pashid Destruction Unit Combatant.
			case 233721: //Pashid Destruction Unit Ambusher.
			case 233722: //Pashid Destruction Unit Mage.
			case 233723: //Pashid Destruction Unit Beastmaster.
			case 233724: //Pashid Destruction Unit Healer.
			case 233725: //Pashid Destruction Unit Rearguard.
			case 233726: //Pashid Destruction Unit Striker.
			case 233727: //Pashid Destruction Unit Drummer.
			case 233728: //Pashid Destruction Unit Elite Combatant.
			case 233729: //Pashid Destruction Unit Elite Ambusher.
			case 233730: //Pashid Destruction Unit Elite Mage.
			case 233731: //Pashid Destruction Unit Elite Beastmaster.
			case 233732: //Pashid Destruction Unit Elite Healer.
			case 233733: //Pashid Destruction Unit Elite Rearguard.
			case 233734: //Pashid Destruction Unit Elite Striker.
				despawnNpc(npc);
			break;
			case 233740: //Test Weapon Dynatoum.
				despawnNpc(npc);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Illuminary Obelisk>");
				spawn(702018, 258.84213f, 251.32626f, 455.12192f, (byte) 105); //Supply Box.
				spawn(730905, 255.36038f, 254.56577f, 455.12015f, (byte) 105); //Illuminary Obelisk Exit.
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 252.05019f, 257.85583f, 455.12195f, (byte) 105); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 252.05019f, 257.85583f, 455.12195f, (byte) 105); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
			break;
		}
    }
	
	//===========================//
	// === 东部护盾任务 === / === Eastern Shield Task ===//
	//===========================//
	protected void startEasternTask() {
		illuminaryTask1.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startEasternShield2();
				easternTaskE1.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				spawn(702218, 255.56438f, 297.59488f, 321.39154f, (byte) 29); //Eastern Defence Charge 01.
            }
        }, 120000)); //...2Min
		illuminaryTask1.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startEasternShield3();
				easternTaskE2.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				spawn(702219, 255.56438f, 297.59488f, 321.39154f, (byte) 29); //Eastern Defence Charge 02.
            }
        }, 240000)); //...4Min
		illuminaryTask1.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startEasternShield4();
				easternTaskE3.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
            }
        }, 360000)); //...6Min
		illuminaryTask1.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				spawn(702220, 255.56438f, 297.59488f, 321.39154f, (byte) 29); //Eastern Defence Charge 03.
				instance.doOnAllPlayers(new Visitor<Player>() {
				    /**
				     * 处理 visit。
				     * Handle visit.
				     *
				     * @param player 玩家 / player
				     */
				    @Override
				    public void visit(Player player) {
						illuminaryWave++;
						stopInstance1(player);
						easternTaskE4.cancel(true);
				    }
			    });
            }
        }, 480000)); //...8Min
	}
	
	//===========================//
	// === 西部护盾任务 === / === Western Shield Task ===//
	//===========================//
	protected void startWesternTask() {
		illuminaryTask2.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startWesternShield2();
				westernTaskW1.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				spawn(702221, 255.38777f, 212.00926f, 321.37292f, (byte) 90); //Western Defence Charge 01.
            }
        }, 120000)); //...2Min
		illuminaryTask2.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startWesternShield3();
				westernTaskW2.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				spawn(702222, 255.38777f, 212.00926f, 321.37292f, (byte) 90); //Western Defence Charge 02.
            }
        }, 240000)); //...4Min
		illuminaryTask2.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startWesternShield4();
				westernTaskW3.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
            }
        }, 360000)); //...6Min
		illuminaryTask2.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				spawn(702223, 255.38777f, 212.00926f, 321.37292f, (byte) 90); //Western Defence Charge 03.
				instance.doOnAllPlayers(new Visitor<Player>() {
				    /**
				     * 处理 visit。
				     * Handle visit.
				     *
				     * @param player 玩家 / player
				     */
				    @Override
				    public void visit(Player player) {
						illuminaryWave++;
						stopInstance2(player);
						westernTaskW4.cancel(true);
				    }
			    });
            }
        }, 480000)); //...8Min
	}
	
	//==========================//
	// == 南部护盾任务 == / == Southern Shield Task ==//
	//==========================//
	protected void startSouthernTask() {
		illuminaryTask3.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startSouthernShield2();
				southernTaskS1.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				spawn(702224, 298.13452f, 254.48087f, 295.93027f, (byte) 119); //Southern Defence Charge 01.
            }
        }, 120000)); //...2Min
		illuminaryTask3.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startSouthernShield3();
				southernTaskS2.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				spawn(702225, 298.13452f, 254.48087f, 295.93027f, (byte) 119); //Southern Defence Charge 02.
            }
        }, 240000)); //...4Min
		illuminaryTask3.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startSouthernShield4();
				southernTaskS3.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
            }
        }, 360000)); //...6Min
		illuminaryTask3.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				spawn(702226, 298.13452f, 254.48087f, 295.93027f, (byte) 119); //Southern Defence Charge 03.
				instance.doOnAllPlayers(new Visitor<Player>() {
				    /**
				     * 处理 visit。
				     * Handle visit.
				     *
				     * @param player 玩家 / player
				     */
				    @Override
				    public void visit(Player player) {
						illuminaryWave++;
						stopInstance3(player);
						southernTaskS4.cancel(true);
				    }
			    });
            }
        }, 480000)); //...8Min
	}
	
	//==========================//
	// == 北部护盾任务 == / == Northern Shield Task ==//
	//==========================//
	protected void startNorthernTask() {
		illuminaryTask4.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startNorthernShield2();
				northernTaskN1.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				spawn(702227, 212.96484f, 254.4526f, 295.90784f, (byte) 60); //Northern Defence Charge 01.
            }
        }, 120000)); //...2Min
		illuminaryTask4.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startNorthernShield3();
				northernTaskN2.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				spawn(702228, 212.96484f, 254.4526f, 295.90784f, (byte) 60); //Northern Defence Charge 02.
            }
        }, 240000)); //...4Min
		illuminaryTask4.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startNorthernShield4();
				northernTaskN3.cancel(true);
				//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
				sendMsgByRace(1402832, Race.PC_ALL, 0);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
            }
        }, 360000)); //...6Min
		illuminaryTask4.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				spawn(702229, 212.96484f, 254.4526f, 295.90784f, (byte) 60); //Northern Defence Charge 03.
				instance.doOnAllPlayers(new Visitor<Player>() {
				    /**
				     * 处理 visit。
				     * Handle visit.
				     *
				     * @param player 玩家 / player
				     */
				    @Override
				    public void visit(Player player) {
						illuminaryWave++;
						stopInstance4(player);
						northernTaskN4.cancel(true);
				    }
			    });
            }
        }, 480000)); //...8Min
	}
	
   /**
	 * 充能阶段越高，刷新的怪物越强。 / The higher the phase of the charge will spawn more difficult monsters, in the 3rd phase elite monsters will spawn. Charging a shield to the 3rd phase continuously can be hard because of all the mobs you will have to handle. A few easy monsters will spawn after a certain time if you leave the shield unit alone. After all units have been charged to the 3rd phase, defeat the remaining monsters. *************************** Eastern Shield Generator * **************************
	 */
	private void startEasternShield1() {
		easternTaskE1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 252.24573f, 333.1747f, 325.59268f, (byte) 90));
				rushIlluminary((Npc)spawn(233721, 254.23112f, 333.21808f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233722, 258.46628f, 333.40833f, 325.51834f, (byte) 90));
				rushIlluminary((Npc)spawn(233723, 256.2306f, 333.3805f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233724, 259.83197f, 333.34024f, 325.64847f, (byte) 90));
			}
		}, 1000);
		easternTaskE1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 252.24573f, 333.1747f, 325.59268f, (byte) 90));
				rushIlluminary((Npc)spawn(233726, 254.23112f, 333.21808f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233727, 258.46628f, 333.40833f, 325.51834f, (byte) 90));
				rushIlluminary((Npc)spawn(233728, 256.2306f, 333.3805f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233729, 259.83197f, 333.34024f, 325.64847f, (byte) 90));
			}
		}, 30000);
	}
	private void startEasternShield2() {
		easternTaskE2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233730, 252.24573f, 333.1747f, 325.59268f, (byte) 90));
				rushIlluminary((Npc)spawn(233731, 254.23112f, 333.21808f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233732, 258.46628f, 333.40833f, 325.51834f, (byte) 90));
				rushIlluminary((Npc)spawn(233733, 256.2306f, 333.3805f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233734, 259.83197f, 333.34024f, 325.64847f, (byte) 90));
			}
		}, 1000);
		easternTaskE2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 252.24573f, 333.1747f, 325.59268f, (byte) 90));
				rushIlluminary((Npc)spawn(233721, 254.23112f, 333.21808f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233722, 258.46628f, 333.40833f, 325.51834f, (byte) 90));
				rushIlluminary((Npc)spawn(233723, 256.2306f, 333.3805f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233724, 259.83197f, 333.34024f, 325.64847f, (byte) 90));
			}
		}, 30000);
	}
	private void startEasternShield3() {
		easternTaskE3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 252.24573f, 333.1747f, 325.59268f, (byte) 90));
				rushIlluminary((Npc)spawn(233726, 254.23112f, 333.21808f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233727, 258.46628f, 333.40833f, 325.51834f, (byte) 90));
				rushIlluminary((Npc)spawn(233728, 256.2306f, 333.3805f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233729, 259.83197f, 333.34024f, 325.64847f, (byte) 90));
			}
		}, 1000);
		easternTaskE3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233730, 252.24573f, 333.1747f, 325.59268f, (byte) 90));
				rushIlluminary((Npc)spawn(233731, 254.23112f, 333.21808f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233732, 258.46628f, 333.40833f, 325.51834f, (byte) 90));
				rushIlluminary((Npc)spawn(233733, 256.2306f, 333.3805f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233734, 259.83197f, 333.34024f, 325.64847f, (byte) 90));
			}
		}, 30000);
	}
	private void startEasternShield4() {
		easternTaskE4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 252.24573f, 333.1747f, 325.59268f, (byte) 90));
				rushIlluminary((Npc)spawn(233721, 254.23112f, 333.21808f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233722, 258.46628f, 333.40833f, 325.51834f, (byte) 90));
				rushIlluminary((Npc)spawn(233723, 256.2306f, 333.3805f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233724, 259.83197f, 333.34024f, 325.64847f, (byte) 90));
			}
		}, 1000);
		easternTaskE4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 252.24573f, 333.1747f, 325.59268f, (byte) 90));
				rushIlluminary((Npc)spawn(233726, 254.23112f, 333.21808f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233727, 258.46628f, 333.40833f, 325.51834f, (byte) 90));
				rushIlluminary((Npc)spawn(233728, 256.2306f, 333.3805f, 325.49332f, (byte) 90));
				rushIlluminary((Npc)spawn(233729, 259.83197f, 333.34024f, 325.64847f, (byte) 90));
			}
		}, 30000);
	}
	
   /**
	 * 西部护盾发生器 / ************************* Western Shield Generator
	 */
	private void startWesternShield1() {
		westernTaskW1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 258.78595f, 176.05591f, 325.59268f, (byte) 30));
				rushIlluminary((Npc)spawn(233721, 257.29633f, 176.01747f, 325.55893f, (byte) 30));
				rushIlluminary((Npc)spawn(233722, 253.48524f, 175.99721f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233723, 255.67467f, 176.00883f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233724, 251.44252f, 175.98637f, 325.64847f, (byte) 30));
			}
		}, 1000);
		westernTaskW1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 258.78595f, 176.05591f, 325.59268f, (byte) 30));
				rushIlluminary((Npc)spawn(233726, 257.29633f, 176.01747f, 325.55893f, (byte) 30));
				rushIlluminary((Npc)spawn(233727, 253.48524f, 175.99721f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233728, 255.67467f, 176.00883f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233729, 251.44252f, 175.98637f, 325.64847f, (byte) 30));
			}
		}, 30000);
	}
	private void startWesternShield2() {
		westernTaskW2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233730, 258.78595f, 176.05591f, 325.59268f, (byte) 30));
				rushIlluminary((Npc)spawn(233731, 257.29633f, 176.01747f, 325.55893f, (byte) 30));
				rushIlluminary((Npc)spawn(233732, 253.48524f, 175.99721f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233733, 255.67467f, 176.00883f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233734, 251.44252f, 175.98637f, 325.64847f, (byte) 30));
			}
		}, 1000);
		westernTaskW2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 258.78595f, 176.05591f, 325.59268f, (byte) 30));
				rushIlluminary((Npc)spawn(233721, 257.29633f, 176.01747f, 325.55893f, (byte) 30));
				rushIlluminary((Npc)spawn(233722, 253.48524f, 175.99721f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233723, 255.67467f, 176.00883f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233724, 251.44252f, 175.98637f, 325.64847f, (byte) 30));
			}
		}, 30000);
	}
	private void startWesternShield3() {
		westernTaskW3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 258.78595f, 176.05591f, 325.59268f, (byte) 30));
				rushIlluminary((Npc)spawn(233726, 257.29633f, 176.01747f, 325.55893f, (byte) 30));
				rushIlluminary((Npc)spawn(233727, 253.48524f, 175.99721f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233728, 255.67467f, 176.00883f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233729, 251.44252f, 175.98637f, 325.64847f, (byte) 30));
			}
		}, 1000);
		westernTaskW3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233730, 258.78595f, 176.05591f, 325.59268f, (byte) 30));
				rushIlluminary((Npc)spawn(233731, 257.29633f, 176.01747f, 325.55893f, (byte) 30));
				rushIlluminary((Npc)spawn(233732, 253.48524f, 175.99721f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233733, 255.67467f, 176.00883f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233734, 251.44252f, 175.98637f, 325.64847f, (byte) 30));
			}
		}, 30000);
	}
	private void startWesternShield4() {
		westernTaskW4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 258.78595f, 176.05591f, 325.59268f, (byte) 30));
				rushIlluminary((Npc)spawn(233721, 257.29633f, 176.01747f, 325.55893f, (byte) 30));
				rushIlluminary((Npc)spawn(233722, 253.48524f, 175.99721f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233723, 255.67467f, 176.00883f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233724, 251.44252f, 175.98637f, 325.64847f, (byte) 30));
			}
		}, 1000);
		westernTaskW4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 258.78595f, 176.05591f, 325.59268f, (byte) 30));
				rushIlluminary((Npc)spawn(233726, 257.29633f, 176.01747f, 325.55893f, (byte) 30));
				rushIlluminary((Npc)spawn(233727, 253.48524f, 175.99721f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233728, 255.67467f, 176.00883f, 325.49332f, (byte) 30));
				rushIlluminary((Npc)spawn(233729, 251.44252f, 175.98637f, 325.64847f, (byte) 30));
			}
		}, 30000);
	}
	
	/**
	 * 南部护盾发生器 / ************************ Southern Shield Generator
	 */
	private void startSouthernShield1() {
		southernTaskS1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 336.21823f, 258.05798f, 292.4295f, (byte) 60));
				rushIlluminary((Npc)spawn(233721, 336.28296f, 256.22827f, 292.3325f, (byte) 60));
				rushIlluminary((Npc)spawn(233722, 336.35062f, 252.48618f, 292.33862f, (byte) 60));
				rushIlluminary((Npc)spawn(233723, 336.3128f, 254.57924f, 292.33252f, (byte) 60));
				rushIlluminary((Npc)spawn(233724, 336.38608f, 250.51807f, 292.46326f, (byte) 60));
			}
		}, 1000);
		southernTaskS1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 336.21823f, 258.05798f, 292.4295f, (byte) 60));
				rushIlluminary((Npc)spawn(233726, 336.28296f, 256.22827f, 292.3325f, (byte) 60));
				rushIlluminary((Npc)spawn(233727, 336.35062f, 252.48618f, 292.33862f, (byte) 60));
				rushIlluminary((Npc)spawn(233728, 336.3128f, 254.57924f, 292.33252f, (byte) 60));
				rushIlluminary((Npc)spawn(233729, 336.38608f, 250.51807f, 292.46326f, (byte) 60));
			}
		}, 30000);
	}
	private void startSouthernShield2() {
		southernTaskS2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233730, 336.21823f, 258.05798f, 292.4295f, (byte) 60));
				rushIlluminary((Npc)spawn(233731, 336.28296f, 256.22827f, 292.3325f, (byte) 60));
				rushIlluminary((Npc)spawn(233732, 336.35062f, 252.48618f, 292.33862f, (byte) 60));
				rushIlluminary((Npc)spawn(233733, 336.3128f, 254.57924f, 292.33252f, (byte) 60));
				rushIlluminary((Npc)spawn(233734, 336.38608f, 250.51807f, 292.46326f, (byte) 60));
			}
		}, 1000);
		southernTaskS2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 336.21823f, 258.05798f, 292.4295f, (byte) 60));
				rushIlluminary((Npc)spawn(233721, 336.28296f, 256.22827f, 292.3325f, (byte) 60));
				rushIlluminary((Npc)spawn(233722, 336.35062f, 252.48618f, 292.33862f, (byte) 60));
				rushIlluminary((Npc)spawn(233723, 336.3128f, 254.57924f, 292.33252f, (byte) 60));
				rushIlluminary((Npc)spawn(233724, 336.38608f, 250.51807f, 292.46326f, (byte) 60));
			}
		}, 30000);
	}
	private void startSouthernShield3() {
		southernTaskS3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 336.21823f, 258.05798f, 292.4295f, (byte) 60));
				rushIlluminary((Npc)spawn(233726, 336.28296f, 256.22827f, 292.3325f, (byte) 60));
				rushIlluminary((Npc)spawn(233727, 336.35062f, 252.48618f, 292.33862f, (byte) 60));
				rushIlluminary((Npc)spawn(233728, 336.3128f, 254.57924f, 292.33252f, (byte) 60));
				rushIlluminary((Npc)spawn(233729, 336.38608f, 250.51807f, 292.46326f, (byte) 60));
			}
		}, 1000);
		southernTaskS3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233730, 336.21823f, 258.05798f, 292.4295f, (byte) 60));
				rushIlluminary((Npc)spawn(233731, 336.28296f, 256.22827f, 292.3325f, (byte) 60));
				rushIlluminary((Npc)spawn(233732, 336.35062f, 252.48618f, 292.33862f, (byte) 60));
				rushIlluminary((Npc)spawn(233733, 336.3128f, 254.57924f, 292.33252f, (byte) 60));
				rushIlluminary((Npc)spawn(233734, 336.38608f, 250.51807f, 292.46326f, (byte) 60));
			}
		}, 30000);
	}
	private void startSouthernShield4() {
		southernTaskS4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 336.21823f, 258.05798f, 292.4295f, (byte) 60));
				rushIlluminary((Npc)spawn(233721, 336.28296f, 256.22827f, 292.3325f, (byte) 60));
				rushIlluminary((Npc)spawn(233722, 336.35062f, 252.48618f, 292.33862f, (byte) 60));
				rushIlluminary((Npc)spawn(233723, 336.3128f, 254.57924f, 292.33252f, (byte) 60));
				rushIlluminary((Npc)spawn(233724, 336.38608f, 250.51807f, 292.46326f, (byte) 60));
			}
		}, 1000);
		southernTaskS4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 336.21823f, 258.05798f, 292.4295f, (byte) 60));
				rushIlluminary((Npc)spawn(233726, 336.28296f, 256.22827f, 292.3325f, (byte) 60));
				rushIlluminary((Npc)spawn(233727, 336.35062f, 252.48618f, 292.33862f, (byte) 60));
				rushIlluminary((Npc)spawn(233728, 336.3128f, 254.57924f, 292.33252f, (byte) 60));
				rushIlluminary((Npc)spawn(233729, 336.38608f, 250.51807f, 292.46326f, (byte) 60));
			}
		}, 30000);
	}
	
	/**
	 * 北部护盾发生器 / ************************* Northern Shield Generator
	 */
	private void startNorthernShield1() {
		northernTaskN1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 176.56479f, 251.09068f, 292.42026f, (byte) 119));
				rushIlluminary((Npc)spawn(233721, 176.4995f, 252.93555f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233722, 176.41188f, 257.24088f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233723, 176.4588f, 254.93521f, 292.33252f, (byte) 0));
				rushIlluminary((Npc)spawn(233724, 176.37492f, 259.05646f, 292.55435f, (byte) 0));
			}
		}, 1000);
		northernTaskN1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 176.56479f, 251.09068f, 292.42026f, (byte) 119));
				rushIlluminary((Npc)spawn(233726, 176.4995f, 252.93555f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233727, 176.41188f, 257.24088f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233728, 176.4588f, 254.93521f, 292.33252f, (byte) 0));
				rushIlluminary((Npc)spawn(233729, 176.37492f, 259.05646f, 292.55435f, (byte) 0));
			}
		}, 30000);
	}
	private void startNorthernShield2() {
		northernTaskN2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233730, 176.56479f, 251.09068f, 292.42026f, (byte) 119));
				rushIlluminary((Npc)spawn(233731, 176.4995f, 252.93555f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233732, 176.41188f, 257.24088f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233733, 176.4588f, 254.93521f, 292.33252f, (byte) 0));
				rushIlluminary((Npc)spawn(233734, 176.37492f, 259.05646f, 292.55435f, (byte) 0));
			}
		}, 1000);
		northernTaskN2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 176.56479f, 251.09068f, 292.42026f, (byte) 119));
				rushIlluminary((Npc)spawn(233721, 176.4995f, 252.93555f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233722, 176.41188f, 257.24088f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233723, 176.4588f, 254.93521f, 292.33252f, (byte) 0));
				rushIlluminary((Npc)spawn(233724, 176.37492f, 259.05646f, 292.55435f, (byte) 0));
			}
		}, 30000);
	}
	private void startNorthernShield3() {
		northernTaskN3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 176.56479f, 251.09068f, 292.42026f, (byte) 119));
				rushIlluminary((Npc)spawn(233726, 176.4995f, 252.93555f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233727, 176.41188f, 257.24088f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233728, 176.4588f, 254.93521f, 292.33252f, (byte) 0));
				rushIlluminary((Npc)spawn(233729, 176.37492f, 259.05646f, 292.55435f, (byte) 0));
			}
		}, 1000);
		northernTaskN3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233730, 176.56479f, 251.09068f, 292.42026f, (byte) 119));
				rushIlluminary((Npc)spawn(233731, 176.4995f, 252.93555f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233732, 176.41188f, 257.24088f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233733, 176.4588f, 254.93521f, 292.33252f, (byte) 0));
				rushIlluminary((Npc)spawn(233734, 176.37492f, 259.05646f, 292.55435f, (byte) 0));
			}
		}, 30000);
	}
	private void startNorthernShield4() {
		northernTaskN4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233720, 176.56479f, 251.09068f, 292.42026f, (byte) 119));
				rushIlluminary((Npc)spawn(233721, 176.4995f, 252.93555f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233722, 176.41188f, 257.24088f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233723, 176.4588f, 254.93521f, 292.33252f, (byte) 0));
				rushIlluminary((Npc)spawn(233724, 176.37492f, 259.05646f, 292.55435f, (byte) 0));
			}
		}, 1000);
		northernTaskN4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				rushIlluminary((Npc)spawn(233725, 176.56479f, 251.09068f, 292.42026f, (byte) 119));
				rushIlluminary((Npc)spawn(233726, 176.4995f, 252.93555f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233727, 176.41188f, 257.24088f, 292.3325f, (byte) 0));
				rushIlluminary((Npc)spawn(233728, 176.4588f, 254.93521f, 292.33252f, (byte) 0));
				rushIlluminary((Npc)spawn(233729, 176.37492f, 259.05646f, 292.55435f, (byte) 0));
			}
		}, 30000);
	}
	
	private void rushIlluminary(final Npc npc) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					for (Player player: instance.getPlayersInside()) {
						npc.setTarget(player);
						((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
						npc.setState(1);
						npc.getMoveController().moveToTargetObject();
						PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
					}
				}
			}
		}, 1000);
	}
	
   /**
	 * 激活封印：当所有护盾单元充能至…… / Activate The Seal: When all shield units have been charged up to the 3rd phase, you can activate the passage to the final boss. When you activate the seal the final boss will appear and the fight will begin
	 */
	private void shieldControl() {
		if (illuminaryWave == 4) {
			deleteNpc(702010); //Eastern Shield Generator.
			deleteNpc(702011); //Western Shield Generator.
			deleteNpc(702012); //Southern Shield Generator.
			deleteNpc(702013); //Northern Shield Generator.
			deleteNpc(702014); //Eastern Pashid Infiltration Gate.
			deleteNpc(702015); //Western Pashid Infiltration Gate.
			deleteNpc(702016); //Southern Pashid Infiltration Gate.
			deleteNpc(702017); //Northern Pashid Infiltration Gate.
			// 护盾已激活，帕希德破坏部队正在撤退。 / The shield is activated and the Pashid Destruction Unit is retreating.
			// 护盾控制室传送器已出现。 / The Shield Control Room Teleporter has appeared.
			sendMsgByRace(1402202, Race.PC_ALL, 0);
			// 护盾室传送装置已出现。 / Shield Chamber Teleport Device appeared.
			sendMsgByRace(1403146, Race.PC_ALL, 10000);
			// 护盾完成。 / Shield Complete.
			spawn(702217, 255.31036f, 254.66649f, 455.12018f, (byte) 91);
			// 护盾防御完成。 / Shield Defence Complete.
			spawn(702287, 255.13590f, 254.21944f, 337.96027f, (byte) 109);
			// 护盾控制室传送器。 / Shield Control Room Teleporter.
			spawn(730886, 255.47392f, 293.56177f, 321.18497f, (byte) 89);
			spawn(730886, 255.55742f, 216.03549f, 321.21344f, (byte) 30);
			spawn(730886, 294.20718f, 254.60352f, 295.77290f, (byte) 60);
			spawn(730886, 216.97739f, 254.46160f, 295.77353f, (byte) 0);
		}
	}
	
	private void illuminaryToDynatoum(Player player) {
		teleport(player, 266.04742f, 244.20813f, 455.17575f, (byte) 45);
	}
	
	private void teleport(float x, float y, float z, byte h) {
		for (Player playerInside: instance.getPlayersInside()) {
			if (playerInside.isOnline()) {
				illuminaryToDynatoum(playerInside);
			}
		}
	}
	
	protected void teleport(Player player, float x, float y, float z, byte h) {
		TeleportService2.teleportTo(player, mapId, instanceId, x, y, z, h);
	}
	
	protected void stopInstance1(Player player) {
		shieldControl();
		stopInstanceTask1();
	}
	protected void stopInstance2(Player player) {
		shieldControl();
		stopInstanceTask2();
	}
	protected void stopInstance3(Player player) {
		shieldControl();
		stopInstanceTask3();
	}
	protected void stopInstance4(Player player) {
		shieldControl();
		stopInstanceTask4();
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
	
	private void sendMessage(final int msgId, long delay) {
        if (delay == 0) {
            this.sendMsg(msgId);
        } else {
            GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
                /**
                 * 处理 run。
                 * Handle run.
                 */
                public void run() {
                    sendMsg(msgId);
                }
            }, delay);
        }
    }
	
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
	
	private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
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
	 * 移除相关物品。
	 * Remove related items.
	 *
	 * @param player 玩家 / player
	 */
	
	public void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(164000289, storage.getItemCountByItemId(164000289));
		storage.decreaseByItemId(164000290, storage.getItemCountByItemId(164000290));
    }
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		stopInstanceTask1();
		stopInstanceTask2();
		stopInstanceTask3();
		stopInstanceTask4();
		isInstanceDestroyed = true;
		doors.clear();
		movies.clear();
	}
	
	private void stopInstanceTask1() {
		for (Future<?> task : illuminaryTask1) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	private void stopInstanceTask2() {
		for (Future<?> task : illuminaryTask2) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	private void stopInstanceTask3() {
		for (Future<?> task : illuminaryTask3) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	private void stopInstanceTask4() {
		for (Future<?> task : illuminaryTask4) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	
	protected void despawnNpc(Npc npc) {
        if (npc != null) {
            npc.getController().onDelete();
        }
    }
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	protected void killNpc(List<Npc> npcs) {
        for (Npc npc: npcs) {
            npc.getController().die();
        }
    }
	
	protected List<Npc> getNpcs(int npcId) {
		if (!isInstanceDestroyed) {
			return instance.getNpcs(npcId);
		}
		return null;
	}
	/**
	 * 玩家请求退出副本时处理。
	 * Handle a player exit request.
	 *
	 * @param player 玩家 / player
	 */
	
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
}
