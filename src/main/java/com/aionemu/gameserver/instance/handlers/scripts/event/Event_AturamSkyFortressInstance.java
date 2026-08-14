package com.aionemu.gameserver.instance.handlers.scripts.event;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 活动·阿图拉姆天空要塞副本事件处理器。
 * Instance event handler for Event Aturam Sky Fortress.
 *
 * @author Encom
 */

@InstanceID(300241000)
public class Event_AturamSkyFortressInstance extends GeneralInstanceHandler
{
	/** energy generators / energy generators */
		private int energyGenerators;
	/** balaur spy crystal / balaur spy crystal */
		private int balaurSpyCrystal;
	/** drakan chief of staff / drakan chief of staff */
		private int drakanChiefOfStaff;
	/** drakan petty officer / drakan petty officer */
		private int drakanPettyOfficer;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
	/** aturamskyfortress 任务 / aturam sky fortress task */
		private final List<Future<?>> aturamSkyFortressTask = new ArrayList<Future<?>>();
	
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
		spawnRings1();
		spawnRings2();
		spawnRings3();
		doors.get(307).setOpen(false);
		doors.get(308).setOpen(false);
		Npc npc = instance.getNpc(217371); //Weapon Hugen.
		if (npc != null) {
			npc.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
			GameEngineServices.skillEngine().getSkill(npc, 21571, 60, npc).useNoAnimationSkill();
		}
	}
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
		super.onInstanceCreate(instance);
		doors = instance.getDoors();
		if (movies.contains(467)) {
            return;
        }
		sendMovie(player, 467);
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
		switch (npcId) {
			case 219185: //Aldreen [Kaichin Engineering League]
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 110900259, 1)); //Shulack Work Clothes.
			break;
			case 217371: //Weapon Hugen.
				switch (Rnd.get(1, 3)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051566, 1)); //Hugen's Pants Box.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051567, 1)); //Hugen's Pauldrons Box.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051573, 1)); //Hugen's Belt Box.
					break;
				}
			break;
			case 217373: //Popuchin.
				switch (Rnd.get(1, 3)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051569, 1)); //Popuchin's Shoe Box.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051570, 1)); //Popuchin's Hairpin Box.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051572, 1)); //Popuchin's Ring Box.
					break;
				}
			break;
			case 217376: //Ashunatal Shadowslip.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
				switch (Rnd.get(1, 3)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051565, 1)); //Ashunatal's Jacket Box.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051568, 1)); //Ashunatal's Glove Box.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051571, 1)); //Ashunatal's Earrings Box.
					break;
				}
			break;
			case 701033: //Balaur Armament Box.
				switch (Rnd.get(1, 2)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000110, 5)); //Fine Life Serum.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000111, 5)); //Fine Mana Serum.
					break;
				}
			break;
		}
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * @param npc 死亡的 NPC / dead NPC
	 */
	@Override
	public void onDie(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 702654: //Dredgion Generator I.
				// 1 号能量发生器被摧毁。出现发生器守护者。 / Power Generator No.1 has been destroyed. A Power Generator Protector appears.
				sendMsgByRace(1402734, Race.PC_ALL, 3000);
				sp(282277, player.getX(), player.getY(), player.getZ(), (byte) 0, 3000, 0, null); //Craftsman Sutchin.
			break;
			case 702653: //Dredgion Generator II.
				// 2 号能量发生器被摧毁。出现发生器守护者。 / Power Generator No.2 has been destroyed. A Power Generator Protector appears.
				sendMsgByRace(1402735, Race.PC_ALL, 3000);
				sp(282280, player.getX(), player.getY(), player.getZ(), (byte) 0, 3000, 0, null); //Craftsman Wichichi.
			break;
			case 702650: //Dredgion Generator III.
				// 3 号能量发生器被摧毁。出现发生器守护者。 / Power Generator No.3 has been destroyed. A Power Generator Protector appears.
				sendMsgByRace(1402736, Race.PC_ALL, 3000);
				sp(282281, player.getX(), player.getY(), player.getZ(), (byte) 0, 3000, 0, null); //Craftsman Prichichi.
			break;
			case 702651: //Dredgion Generator IV.
				// 4 号能量发生器被摧毁。出现发生器守护者。 / Power Generator No.4 has been destroyed. A Power Generator Protector appears.
				sendMsgByRace(1402737, Race.PC_ALL, 3000);
				sp(282279, player.getX(), player.getY(), player.getZ(), (byte) 0, 3000, 0, null); //Craftsman Pituchin.
			break;
			case 702652: //Dredgion Generator V.
				doors.get(126).setOpen(true);
				// 5 号能量发生器被摧毁。出现发生器守护者。 / Power Generator No. 5 has been destroyed. A Power Generator Protector appears.
				sendMsgByRace(1402738, Race.PC_ALL, 3000);
				sp(282278, player.getX(), player.getY(), player.getZ(), (byte) 0, 3000, 0, null); //Craftsman Duduchin.
			break;
			case 217373: //Popuchin.
				sp(702664, 352.29132f, 424.08679f, 655.74670f, (byte) 0, 297, 3000, 0, null); //Activated Balaur Teleporter.
				sp(730375, 374.85000f, 424.32000f, 653.52000f, (byte) 0, 0, 3000, 0, null); //Popukin's Treasure Box.
			break;
			case 217343: //Talon Guardian.
				doors.get(68).setOpen(true);
				// 全部间谍水晶已摧毁，通往利爪实验室的门已打开。 / All Spy Crystals have been destroyed and the door to the Talon Laboratory has opened.
				sendMsgByRace(1402740, Race.PC_ALL, 0);
			break;
			case 701043: //Energy Generator.
				despawnNpc(npc);
				deleteNpc(701110); //Enhanced Barrier.
				// 能量发生器被摧毁，防护盾已消失。 / The Energy Generator has been destroyed and the Protective Shield has disappeared.
				sendMsgByRace(1400913, Race.PC_ALL, 0);
				// 外层防护墙已消失，武器 H 正从休眠中苏醒。 / The Outer Protective Wall is gone, and Weapon H is waking from its dormant state.
				sendMsgByRace(1400909, Race.PC_ALL, 5000);
			break;
			case 217371: //Weapon Hugen.
				sp(730392, 814.94000f, 303.36319f, 603.42773f, (byte) 0, 97, 3000, 0, null); //Activated Balaur Teleporter.
				sp(730390, 637.00262f, 497.52673f, 658.33716f, (byte) 0, 86, 3000, 0, null); //Shulack Flitter.
				sp(730374, 815.39700f, 288.39000f, 602.76400f, (byte) 91, 0, 3000, 0, null); //H-Core.
			break;
			case 217370: //Drakan Petty Officer.
				drakanPettyOfficer++;
				if (drakanPettyOfficer == 4) {
					doors.get(174).setOpen(true);
					doors.get(307).setOpen(true);
					doors.get(308).setOpen(true);
					startOfficerWalkerEvent();
				} else if (drakanPettyOfficer == 8) {
					doors.get(175).setOpen(true);
					startMarbataWalkerEvent();
				}
				despawnNpc(npc);
			break;
			case 217656: //Drakan Chief Of Staff.
				drakanChiefOfStaff++;
				if (drakanChiefOfStaff == 1) {
					startOfficerWalkerEvent();
				} else if (drakanChiefOfStaff == 2) {
					doors.get(178).setOpen(true);
				}
				despawnNpc(npc);
			break;
			case 217382: //Commander Barus.
				doors.get(230).setOpen(true);
				AbyssPointsService.addGp(player, 100);
				AbyssPointsService.addAp(player, 2000);
				// 通往阿舒纳塔尔准备室的门已打开。 / The door to Ashunatal's Ready Room is now open. You can see Ashunatal behind the door.
				sendMsgByRace(1401048, Race.PC_ALL, 2000);
			break;
			case 218577: //Marabata Watchman.
				// 阿图拉姆空中要塞指挥中心：击杀大量“指挥区龙人”后出现指挥官。 / At home in the Aturam Sky Fortress Command Center, the Commander appears when many "Command Area Drakan" are killed.
				despawnNpc(npc);
				spawn(217382, 258.3894f, 796.7554f, 901.6453f, (byte) 80); //Commander Barus.
			break;
			case 701029: //Energy Generator.
				Npc weaponHugen = instance.getNpc(217371); //Weapon Hugen.
				energyGenerators++;
				if (weaponHugen != null) {
					if (energyGenerators == 1) {
						// 能量发生器变得不稳定。 / The Energy Generator is becoming unstable.
						sendMsgByRace(1400910, Race.PC_ALL, 0);
					} else if (energyGenerators == 2) {
						// 能量发生器被摧毁，防护盾力量减弱。 / The Energy Generator has been destroyed and the power of the Protective Shield has been reduced.
						sendMsgByRace(1400911, Race.PC_ALL, 0);
					} else if (energyGenerators == 3) {
						// 能量发生器被摧毁，防护盾力量大幅减弱。 / The Energy Generator has been destroyed and the power of the Protective Shield has been greatly reduced.
						sendMsgByRace(1400912, Race.PC_ALL, 0);
					} else if (energyGenerators == 4) {
						weaponHugen.getEffectController().removeEffect(21571);
						sp(701043, 815.66711f, 249.08171f, 603.10529f, (byte) 0, 18, 3000, 0, null); //Barrier Controller.
					}
				}
				despawnNpc(npc);
			break;
			case 217376: //Ashunatal Shadowslip.
				AbyssPointsService.addAp(player, 2000);
				AbyssPointsService.addGp(player, 200);
				// 这里有巨大的苏卡纳装置。 / There is a huge Surkana device here.
				// 既然阿舒纳塔尔拼死保护它，就该摧毁它并干扰龙族计划。 / Since Ashunatal risked her life to protect it, you should destroy it and interfere with the Balaur's plans.
				sendMsgByRace(1401401, Race.PC_ALL, 2000);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Aturam Sky Fortress>");
            break;
			case 217369: //Drakan Crewhand.
			case 217368: //Drakan Combatant.
			case 217655: //Veteran Escort Officer.
				despawnNpc(npc);
			break;
		}
	}
	
	private void spawnRings1() {
        FlyRing f1 = new FlyRing(new FlyRingTemplate("ATURAM_SKY_FORTRESS_1", mapId,
        new Point3D(435.79208, 421.96408, 625.9659),
		new Point3D(435.13388, 424.68580, 658.3014),
        new Point3D(435.42896, 427.36942, 652.9659), 61), instanceId);
        f1.spawn();
    }
	private void spawnRings2() {
        FlyRing f2 = new FlyRing(new FlyRingTemplate("ATURAM_SKY_FORTRESS_2", mapId,
        new Point3D(819.41113, 212.29883, 605.6249),
		new Point3D(815.09174, 211.83253, 615.6375),
        new Point3D(810.67690, 212.15894, 605.6249), 90), instanceId);
        f2.spawn();
    }
	private void spawnRings3() {
        FlyRing f3 = new FlyRing(new FlyRingTemplate("ATURAM_SKY_FORTRESS_3", mapId,
        new Point3D(167.87286, 654.69824, 901.0089),
		new Point3D(172.81534, 652.12440, 913.8702),
        new Point3D(176.82208, 649.07650, 901.0089), 20), instanceId);
        f3.spawn();
    }
	
	/**
	 * 玩家通过飞行环时处理。
	 * Handle a player passing a flying ring.
	 *
	 * @param player 玩家 / player
	 * @param flyingRing 飞行环标识 / flying-ring id
	 * @return 是否已处理 / whether handled
	 */
	@Override
    public boolean onPassFlyingRing(Player player, String flyingRing) {
        if (flyingRing.equals("ATURAM_SKY_FORTRESS_1")) {
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
						removeEffects(player);
					}
				}
			});
		} else if (flyingRing.equals("ATURAM_SKY_FORTRESS_2")) {
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
						removeEffects(player);
					}
				}
			});
		} else if (flyingRing.equals("ATURAM_SKY_FORTRESS_3")) {
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
						doors.get(177).setOpen(true);
					}
				}
			});
		}
		return false;
	}
	
	private void rushWalk(final Npc npc) {
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
	
	private void startMarbataWalkerEvent() {
		// 等候室警报响起。高战力龙族正朝你而来！ / Alarms rang in the Waiting Room. High-powered Drakan are heading your way!
		sendMsgByRace(1401050, Race.PC_ALL, 0);
		rushWalk((Npc)spawn(218577, 193.45583f, 802.1455f, 900.7575f, (byte) 103)); //Marabata Watchman.
		rushWalk((Npc)spawn(217655, 198.34431f, 801.4107f, 900.66125f, (byte) 110)); //Veteran Escort Officer.
		rushWalk((Npc)spawn(217655, 197.13315f, 798.7863f, 900.6499f, (byte) 110)); //Veteran Escort Officer.
	}
	
	private void startOfficerWalkerEvent() {
		// 机组室门已打开。击杀龙族！ / The door of the Aircrew Room is now open. Kill the Drakan!
		sendMsgByRace(1401049, Race.PC_ALL, 0);
		rushWalk((Npc)spawn(217655, 146.53816f, 713.5974f, 901.0108f, (byte) 111)); //Veteran Escort Officer.
		rushWalk((Npc)spawn(217655, 144.84991f, 720.9318f, 901.0604f, (byte) 96)); //Veteran Escort Officer.
		rushWalk((Npc)spawn(217655, 146.19899f, 709.60455f, 901.0078f, (byte) 110)); //Veteran Escort Officer.
		rushWalk((Npc)spawn(217656, 144.11845f, 716.8327f, 901.046f, (byte) 100)); //Drakan Chief Of Staff.
		rushWalk((Npc)spawn(217369, 144.96825f, 712.83344f, 901.0133f, (byte) 110)); //Drakan Crewhand.
		rushWalk((Npc)spawn(217369, 144.75804f, 718.4293f, 901.05493f, (byte) 80)); //Drakan Crewhand.
	}
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(164000163, storage.getItemCountByItemId(164000163)); //Talon Summoning Device.
		storage.decreaseByItemId(164000202, storage.getItemCountByItemId(164000202)); //Bottomless Bucket.
	}
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(19407); //Powerful Defense.
		effectController.removeEffect(19408); //Strong Defense.
		effectController.removeEffect(19520); //Overclock.
		effectController.removeEffect(21807); //Board Swift Runner.
		effectController.removeEffect(21808); //Board Swift Runner.
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
		removeEffects(player);
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
		removeEffects(player);
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
		movies.clear();
		doors.clear();
	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * @param player 玩家 / player
	 * @param npc 目标 NPC / target NPC
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 205494: //Hariken's Supply Chest.
			    if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				despawnNpc(npc);
				addHarikenItems(player);
			break;
			case 731533: //Magma Tachysphere.
			    despawnNpc(npc);
				sp(731533, npc.getX(), npc.getY(), npc.getZ(), (byte) 0, 0, 20000, 0, null); //Board Swift Runner.
				GameEngineServices.skillEngine().getSkill(npc, 21807, 60, player).useNoAnimationSkill(); //Board Swift Runner.
			break;
			case 731534: //Magma Tachysphere.
			    despawnNpc(npc);
				sp(731534, npc.getX(), npc.getY(), npc.getZ(), (byte) 0, 0, 20000, 0, null); //Board Swift Runner.
				GameEngineServices.skillEngine().getSkill(npc, 21808, 60, player).useNoAnimationSkill(); //Board Swift Runner.
			break;
			case 730397: //Recharger.
			    despawnNpc(npc);
				// 能量覆盖你，你感到体格更强健。 / You feel more physically fit as the energy covers you.
				sendMsgByRace(1400926, Race.PC_ALL, 0);
				GameEngineServices.skillEngine().getSkill(npc, 19520, 51, player).useNoAnimationSkill(); //Overclock.
			break;
			case 730398: //Flagon.
				despawnNpc(npc);
				// 你从舒拉克饮料恢复了生命值。 / You have recovered HP from the Shulack Drink.
				sendMsgByRace(1400927, Race.PC_ALL, 0);
				if (player.getEffectController().hasAbnormalEffect(21807) ||
				    player.getEffectController().hasAbnormalEffect(21808)) {
					player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 100000);
					player.getLifeStats().increaseMp(SM_ATTACK_STATUS.TYPE.MP, 100000);
				} else {
					player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 20000);
					player.getLifeStats().increaseMp(SM_ATTACK_STATUS.TYPE.MP, 20000);
				}
			break;
			case 730410: //Warehouse Latch.
				doors.get(90).setOpen(true);
				// 墙上有个未标记的把手。敢的话就扳它。 / You see an unlabeled handle on the wall. Switch it if you dare.
				sendMsgByRace(1401027, Race.PC_ALL, 0);
				// 你被监控灯发现！敌人来了！ / You have been detected by the Monitoring Lamp! Enemies are coming!
				sendMsgByRace(1401028, Race.PC_ALL, 10000);
			break;
		}
	}
	
	private void addHarikenItems(Player player) {
		ItemService.addItem(player, 164000163, 1); //Talon Summoning Device.
		ItemService.addItem(player, 164000202, 1); //Bottomless Bucket.
	}
	
	private void stopInstanceTask() {
        for (Future<?> task : aturamSkyFortressTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	/**
	 * 延迟刷出 NPC。
	 * Delayed NPC spawn.
	 *
	 * @param npcId NPC ID / NPC id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * @param time 延迟毫秒 / delay millis
	 */

	protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
	/**
	 * 延迟刷出 NPC 并广播消息。
	 * Delayed NPC spawn with a broadcast message.
	 *
	 * @param npcId NPC ID / NPC id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * @param time 延迟毫秒 / delay millis
	 * @param msg 消息 ID / message id
	 * @param race 目标阵营 / target race
	 */

    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
	/**
	 * 延迟刷出带实体 ID 的 NPC 并广播消息。
	 * Delayed NPC spawn with entity id and a broadcast message.
	 *
	 * @param npcId NPC ID / NPC id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * @param entityId 实体 ID / entity id
	 * @param time 延迟毫秒 / delay millis
	 * @param msg 消息 ID / message id
	 * @param race 目标阵营 / target race
	 */

    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        aturamSkyFortressTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    spawn(npcId, x, y, z, h, entityId);
                    if (msg > 0) {
                        sendMsgByRace(msg, race, 0);
                    }
                }
            }
        }, time));
    }
	/**
	 * 延迟刷出带巡路线 NPC。
	 * Delayed NPC spawn with a walker route.
	 *
	 * @param npcId NPC ID / NPC id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * @param time 延迟毫秒 / delay millis
	 * @param walkerId 巡路线 ID / walker id
	 */

    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        aturamSkyFortressTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    Npc npc = (Npc) spawn(npcId, x, y, z, h);
                    npc.getSpawn().setWalkerId(walkerId);
                    WalkManager.startWalking((NpcAI2) npc.getAi2());
                }
            }
        }, time));
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
	 * 按阵营广播系统消息。
	 * Broadcasts a system message by race.
	 *
	 * @param msg 消息 ID / message id
	 * @param race 目标阵营 / target race
	 * @param time 延迟毫秒 / delay millis
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
	
	private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
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
}
