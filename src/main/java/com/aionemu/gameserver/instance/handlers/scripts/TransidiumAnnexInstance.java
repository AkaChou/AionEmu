package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.Set;

/**
 * 超质附件副本事件处理器。
 * Instance event handler for Transidium Annex.
 *
 * @author Encom
 */

@InstanceID(400030000)
public class TransidiumAnnexInstance extends GeneralInstanceHandler
{
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
	/** hangar barricade / hangar barricade */
		private int hangarBarricade;
	/** transidium annex base / transidium annex base */
		private int transidiumAnnexBase;
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
			case 277224: //Ahserion.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053117, 1)); //Ahserion's Glory Reward Box.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188056852, 1)); //Ahserion's Equipment Box.
				        break;
					}
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
		hangarBarricade = runtimeState().getInt("transidium.hangar_barricade", 0);
		transidiumAnnexBase = runtimeState().getInt("transidium.base", 0);
		String race = runtimeState().get("transidium.race");
		spawnRace = race == null ? null : Race.valueOf(race);
		Npc npc = instance.getNpc(277224); //Ahserion.
		if (npc != null && hangarBarricade < 4) {
			GameEngineServices.skillEngine().getSkill(npc, 21571, 60, npc).useNoAnimationSkill(); //Ereshkigal's Reign.
		}
		long deadline = runtimeState().getLong("transidium.start_deadline", 0);
		if (runtimeState().getBoolean("transidium.start_open", false)) {
			openFirstDoors();
		} else if (deadline > 0) {
			scheduleDeadline("start", deadline, this::openStart);
		}
		if (runtimeState().getBoolean("transidium.complete", false)) {
			spawnCompletion();
		}
		long returnDeadline = runtimeState().getLong("transidium.return_deadline", 0);
		if (!runtimeState().getBoolean("transidium.return_complete", false) && returnDeadline > 0) {
			scheduleDeadline("return", returnDeadline, this::finishReturn);
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
		if (spawnRace == null) {
			spawnRace = player.getRace();
			runtimeState().put("transidium.race", spawnRace.name());
		}
		if (runtimeState().getLong("transidium.start_deadline", 0) == 0) {
			// 正在加载进阶走廊护盾……请稍候。 / Loading the Advance Corridor Shield... Please wait.
			sendMsg(1402252, 0, false, 25, 10000);
			// 进阶走廊护盾已激活。 / The Advance Corridor Shield has been activated.
			// 若保护装置被摧毁，通道将消失并返回要塞。 / If the protection device is destroyed, the corridor will disappear and you will return to the fortress.
			sendMsg(1402637, 0, false, 25, 20000);
			// 成员招募窗口已过，无法再招募成员。 / The member recruitment window has passed. You cannot recruit any more members.
			sendMsg(1401181, 0, false, 25, 50000);
			// 特兰西迪姆附楼效果削弱了机库路障。 / The effect of the Transidium Annex has weakened the Hangar Barricade.
			sendMsg(1402638, 0, false, 25, 1200000);
			long deadline = System.currentTimeMillis() + 60_000;
			runtimeState().put("transidium.start_deadline", deadline);
			scheduleDeadline("start", deadline, this::openStart);
		}
	}

	private void openStart() {
		runtimeState().put("transidium.start_open", true);
		openFirstDoors();
		sendMsg(1401838);
		sendQuestionWindow();
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
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("CHARIOT_HANGAR_1_400030000")) {
			transidiumAnnexBase = 1;
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("CHARIOT_HANGAR_2_400030000")) {
			transidiumAnnexBase = 2;
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("IGNUS_ENGINE_HANGAR_1_400030000")) {
            transidiumAnnexBase = 3;
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("IGNUS_ENGINE_HANGAR_2_400030000")) {
			transidiumAnnexBase = 4;
		}
		runtimeState().put("transidium.base", transidiumAnnexBase);
    }
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
    public void onDie(Npc npc) {
		Player mostPlayerDamage = npc.getAggroList().getMostPlayerDamage();
        if (mostPlayerDamage == null) {
            return;
        }
		Race race = mostPlayerDamage.getRace();
		switch (npc.getObjectTemplate().getTemplateId()) {
			// 贝卢斯进阶走廊护盾。 / Belus Advance Corridor Shield.
			case 297306:
				beginReturn(1402270, 400020000);
			break;
			// 阿斯皮达进阶走廊护盾。 / Aspida Advance Corridor Shield.
			case 297307:
				beginReturn(1402271, 400040000);
			break;
			// 阿塔纳托斯进阶走廊护盾。 / Atanatos Advance Corridor Shield.
			case 297308:
				beginReturn(1402272, 400050000);
			break;
			// 迪西隆进阶走廊护盾。 / Disillon Advance Corridor Shield.
			case 297309:
				beginReturn(1402273, 400060000);
			break;
			case 297310: //Chariot Hangar I Controller.
		        despawnNpc(npc);
				if (transidiumAnnexBase == 1) {
				    if (race.equals(Race.ELYOS)) {
					    deleteNpc(804118);
						// 战车机库 I 控制器已被摧毁。 / Chariot Hangar I Controller has been destroyed.
							sendMsg(1402262);
					    spawn(804116, 335.55713f, 512.7856f, 683.0075f, (byte) 61); //Elyos Chariot Hangar I Flag.
				    } else if (race.equals(Race.ASMODIANS)) {
					    deleteNpc(804118);
						// 战车机库 I 控制器已被摧毁。 / Chariot Hangar I Controller has been destroyed.
							sendMsg(1402262);
					    spawn(804114, 335.55713f, 512.7856f, 683.0075f, (byte) 61); //Elyos Chariot Hangar I Flag.
				    }
				}
			break;
			case 297311: //Chariot Hangar II Controller.
			    despawnNpc(npc);
				if (transidiumAnnexBase == 2) {
				    if (race.equals(Race.ELYOS)) {
					    deleteNpc(804123);
						// 战车机库 II 控制器已被摧毁。 / Chariot Hangar II Controller has been destroyed.
							sendMsg(1402263);
					    spawn(804121, 681.18427f, 513.76154f, 683.0339f, (byte) 0); //Elyos Chariot Hangar II Flag.
				    } else if (race.equals(Race.ASMODIANS)) {
					    deleteNpc(804123);
						// 战车机库 II 控制器已被摧毁。 / Chariot Hangar II Controller has been destroyed.
							sendMsg(1402263);
					    spawn(804119, 681.18427f, 513.76154f, 683.0339f, (byte) 0); //Asmodians Chariot Hangar II Flag.
				    }
				}
			break;
			case 297312: //Ignus Engine Hangar I Controller.
			    despawnNpc(npc);
				if (transidiumAnnexBase == 3) {
				    if (race.equals(Race.ELYOS)) {
					    deleteNpc(804128);
						// 伊格努斯引擎机库 I 控制器已被摧毁。 / Ignus Engine Hangar I Controller has been destroyed.
							sendMsg(1402264);
					    spawn(804126, 508.25092f, 339.45773f, 683.0075f, (byte) 91); //Elyos Ignus Engine Hangar I Flag.
				    } else if (race.equals(Race.ASMODIANS)) {
					    deleteNpc(804128);
						// 伊格努斯引擎机库 I 控制器已被摧毁。 / Ignus Engine Hangar I Controller has been destroyed.
							sendMsg(1402264);
					    spawn(804124, 508.25092f, 339.45773f, 683.0075f, (byte) 91); //Asmodians Ignus Engine Hangar I Flag.
				    }
				}
			break;
			case 297313: //Ignus Engine Hangar II Controller.
				despawnNpc(npc);
				if (transidiumAnnexBase == 4) {
				    if (race.equals(Race.ELYOS)) {
					    deleteNpc(804133);
						// 伊格努斯引擎机库 II 控制器已被摧毁。 / Ignus Engine Hangar II Controller has been destroyed.
							sendMsg(1402265);
					    spawn(804131, 508.54236f, 686.10504f, 683.0075f, (byte) 30); //Elyos Ignus Engine Hangar II Flag.
				    } else if (race.equals(Race.ASMODIANS)) {
					    deleteNpc(804133);
						// 伊格努斯引擎机库 II 控制器已被摧毁。 / Ignus Engine Hangar II Controller has been destroyed.
							sendMsg(1402265);
					    spawn(804129, 508.54236f, 686.10504f, 683.0075f, (byte) 30); //Asmodians Ignus Engine Hangar II Flag.
				    }
				}
			break;
			case 277229: //Hangar Barricade.
				Npc ahserion = instance.getNpc(277224); //Ereshkigal's Reign.
				hangarBarricade++;
				runtimeState().put("transidium.hangar_barricade", hangarBarricade);
				if (ahserion != null) {
				    if (hangarBarricade == 1) {
				    } else if (hangarBarricade == 2) {
				    } else if (hangarBarricade == 3) {
				    } else if (hangarBarricade == 4) {
					    ahserion.getEffectController().removeEffect(21571); //Ereshkigal's Reign.
				    }
				}
				despawnNpc(npc);
			break;
			case 277224: //Ahserion.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Transidium Annex>");
				runtimeState().put("transidium.complete", true);
				spawnCompletion();
	            break;
			}
		}

	private void beginReturn(int messageId, int worldId) {
		if (runtimeState().getInt("transidium.return_world", 0) != 0) {
			return;
		}
		runtimeState().put("transidium.return_world", worldId);
		long deadline = System.currentTimeMillis() + 15_000L;
		runtimeState().put("transidium.return_deadline", deadline);
		sendMsg(messageId, 0, false, 25, 2_000);
		sendMsg(1402641, 0, false, 25, 7_000);
		sendMsg(1402642, 0, false, 25, 12_000);
		scheduleDeadline("return", deadline, this::finishReturn);
	}

	private void finishReturn() {
		if (runtimeState().getBoolean("transidium.return_complete", false)) {
			return;
		}
		int worldId = runtimeState().getInt("transidium.return_world", 0);
		if (worldId == 0) {
			return;
		}
		runtimeState().put("transidium.return_complete", true);
		instance.doOnAllPlayers(
			player -> TeleportService2.teleportTo(player, worldId, 1023.73315f, 1023.5483f, 1530.4855f, (byte) 27));
	}

		private void spawnCompletion() {
		int pasha = spawnRace == Race.ASMODIANS ? 804750 : 804749;
		spawn(pasha, 499.92294f, 512.67365f, 675.0881f, (byte) 0);
	}
	
	private void sendQuestionWindow() {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_MSG_SVS_DIRECT_PORTAL_OPEN_NOTICE, 0, 0));
			}
		});
	}
	/**
	 * 打开指定门。
	 * Open the given door.
	 *
	 * doorId
	 */
	
	protected void openDoor(int doorId) {
		setDoorState(doorId, true);
    }
	/**
	 * 处理 openFirstDoors。
	 * Handle openFirstDoors.
	 */
	
	protected void openFirstDoors() {
	    openDoor(176);
		openDoor(177);
		openDoor(178);
		openDoor(179);
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
			case 277225: //Belus Camp Defense Cannon.
			case 277226: //Aspida Camp Defense Cannon.
			case 277227: //Atanatos Camp Defense Cannon.
			case 277228: //Disilon Camp Defense Cannon.
			    despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21652, 60, player).useNoAnimationSkill(); //Armaments Thief.
			break;
			//** ///////// / /////////* *//
			//** ///////// / /////////* *//
			case 297331: //Belus Chariot.
			    despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21582, 60, player).useNoAnimationSkill(); //Board The Chariot.
			break;
			case 297332: //Aspida Chariot.
			    despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21589, 60, player).useNoAnimationSkill(); //Board The Chariot.
			break;
			case 297333: //Atanatos Chariot.
			    despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21590, 60, player).useNoAnimationSkill(); //Board The Chariot.
			break;
			case 297334: //Disilon Chariot.
			    despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21591, 60, player).useNoAnimationSkill(); //Board The Chariot.
			break;
			//** ///////// / /////////* *//
			//** ///////// / /////////* *//
			case 297472: //Belus Chariot.
			    despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21579, 60, player).useNoAnimationSkill(); //Board The Ignus Engine.
			break;
			case 297473: //Aspida Chariot.
                despawnNpc(npc);			
				GameEngineServices.skillEngine().getSkill(npc, 21586, 60, player).useNoAnimationSkill(); //Board The Ignus Engine.
			break;
			case 297474: //Atanatos Chariot.
			    despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21587, 60, player).useNoAnimationSkill(); //Board The Ignus Engine.
			break;
			case 297475: //Disilon Chariot.
				despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21588, 60, player).useNoAnimationSkill(); //Board The Ignus Engine.
			break;
		}
	}
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(21728);
		effectController.removeEffect(21729);
		effectController.removeEffect(21730);
		effectController.removeEffect(21731);
		effectController.removeEffect(21579);
		effectController.removeEffect(21582);
		effectController.removeEffect(21586);
		effectController.removeEffect(21587);
		effectController.removeEffect(21588);
		effectController.removeEffect(21589);
		effectController.removeEffect(21590);
		effectController.removeEffect(21591);
	}
	/**
	 * 移除指定 NPC。
	 * Despawn the given NPC.
	 *
	 * npc
	 */
	
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
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
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
		removeEffects(player);
	}
	
}
