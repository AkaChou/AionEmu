package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.commons.utils.Rnd;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.Set;

/**
 * 龙先知巢穴副本事件处理器。
 * Instance event handler for Drakenseer Lair.
 *
 * @author Encom
 */

@InstanceID(301620000)
public class DrakenseerLairInstance extends GeneralInstanceHandler
{
	private static final long TIME_LIMIT = 10 * 60_000L;
	
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
			case 220450: //Akhal The Oracle.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166030005, 5)); //淬炼溶液。 / Tempering Solution.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166040001, 1)); //Essence Core Solution.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058413, 1)); //? ?  ??.
                        switch (Rnd.get(1, 4)) {
				            case 1:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057624, 1)); //Oracle's Illusion Godstone Bundle.
				            break;
					        case 2:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057625, 1)); //Oracle Greater Enchant Supplement Bundle.
				            break;
							case 3:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057626, 1)); //Oracle Ancient Relic Bundle.
				            break;
							case 4:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057627, 1)); //Arkhal's Accessory Box.
				            break;
						} switch (Rnd.get(1, 2)) {
				            case 1:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054910, 1)); //Akhal's Weapon Box.
				            break;
					        case 2:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054911, 1)); //Akhal's Armor Box.
				            break;
						}
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
		spawnDrakenseerLairRings();
		// 你已进入龙视者之巢。 / You have entered Drakenseer's Lair.
		sendMsg(1403376, 0, false, 25, 5000);
		Npc npc = getNpc(220450); //Akhal The Oracle.
		if (npc != null && !runtimeState().getBoolean("drakenseer.unsealed", false)) {
			GameEngineServices.skillEngine().getSkill(npc, 21791, 60, npc).useNoAnimationSkill(); //Turning Tide.
		}
		if (runtimeState().getBoolean("drakenseer.complete", false)) {
			spawnExit();
			return;
		}
		long deadline = runtimeState().getLong("drakenseer.deadline", 0);
		if (deadline > 0 && !runtimeState().getBoolean("drakenseer.unsealed", false)
				&& !runtimeState().getBoolean("drakenseer.expired", false)) {
			if (deadline <= System.currentTimeMillis()) {
				expireChallenge();
			} else {
				scheduleChallenge(deadline);
			}
		}
		long unsealMessageDeadline = runtimeState().getLong("drakenseer.unseal_message_deadline", 0);
		if (unsealMessageDeadline > 0) {
			scheduleDeadline("unseal_message", unsealMessageDeadline, () -> sendMsg(1403381));
		}
	}

	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		long deadline = runtimeState().getLong("drakenseer.deadline", 0);
		if (deadline > System.currentTimeMillis() && !runtimeState().getBoolean("drakenseer.unsealed", false)
				&& !runtimeState().getBoolean("drakenseer.expired", false)) {
			PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(0, (int) ((deadline - System.currentTimeMillis()) / 1000)));
		}
	}
	
	/**
	 * 玩家通过飞行环时处理。
	 * Handle a player passing a flying ring.
	 *
	 * 玩家 / player
	 * @param flyingRing 飞行环标识 / flying-ring id
	 * result
	 */
	@Override
    public boolean onPassFlyingRing(Player player, String flyingRing) {
		if (flyingRing.equals("DRAKENSEER_LAIR") && runtimeState().getLong("drakenseer.deadline", 0) == 0
				&& !runtimeState().getBoolean("drakenseer.unsealed", false)
				&& !runtimeState().getBoolean("drakenseer.expired", false)) {
			long deadline = System.currentTimeMillis() + TIME_LIMIT;
			runtimeState().put("drakenseer.deadline", deadline);
			scheduleChallenge(deadline);
			instance.doOnAllPlayers(p -> {
				if (p.isOnline()) {
					PacketSendUtility.sendPacket(p, new SM_QUEST_ACTION(0, 600));
					// 10 分钟内摧毁护盾导管并击败阿卡哈尔。 / Destroy the Shielding Conduits within 10 minutes and defeat Akhal.
					PacketSendUtility.sendPacket(p, new SM_SYSTEM_MESSAGE(1403377));
				}
			});
		}
		return false;
	}
	
	private void spawnDrakenseerLairRings() {
        FlyRing f1 = new FlyRing(new FlyRingTemplate("DRAKENSEER_LAIR", mapId,
        new Point3D(283.44757, 342.6241, 336.25607),
        new Point3D(276.73062, 339.42966, 345.29074),
        new Point3D(270.43948, 340.3889, 336.3338), 93), instanceId);
        f1.spawn();
    }
	private void scheduleChallenge(long deadline) {
		scheduleIfFuture("start_message", deadline - 9 * 60_000L, 1403375);
		scheduleIfFuture("warning_1", deadline - 60_000L, 1403382);
		scheduleDeadline("expire", deadline, this::expireChallenge);
	}

	private void scheduleIfFuture(String key, long deadline, int messageId) {
		if (deadline > System.currentTimeMillis()) {
			scheduleDeadline(key, deadline, () -> sendMsg(messageId));
		}
	}

	private void expireChallenge() {
		if (runtimeState().getBoolean("drakenseer.expired", false)) {
			return;
		}
		runtimeState().put("drakenseer.expired", true);
		instance.doOnAllPlayers(this::onExitInstance);
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
    public void onDie(Npc npc) {
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 857974: //Balaur Abyss Gate Enhancer A.
			case 857975: //Balaur Abyss Gate Enhancer B.
			case 857976: //Balaur Abyss Gate Enhancer C.
				if (runtimeState().getBoolean("drakenseer.unsealed", false)) {
					despawnNpc(npc);
					break;
				}
				int killed = Math.min(3, runtimeState().getInt("drakenseer.enhancers", 0) + 1);
				runtimeState().put("drakenseer.enhancers", killed);
				if (killed == 1) {
					// 还剩两个护盾导管。 / Two Shielding Conduits remain.
				    sendMsg(1403379);
				} else if (killed == 2) {
					// 还剩一个护盾导管。 / One Shielding Conduit remains.
					sendMsg(1403380);
				} else if (killed == 3) {
					runtimeState().put("drakenseer.unsealed", true);
					cancelChallenge();
					// 全部护盾导管被摧毁后，阿卡哈尔终于出现。 / With all the Shielding Conduits destroyed, Akhal finally appears.
					long messageDeadline = System.currentTimeMillis() + 2_000;
					runtimeState().put("drakenseer.unseal_message_deadline", messageDeadline);
					scheduleDeadline("unseal_message", messageDeadline, () -> sendMsg(1403381));
					Npc akhalTheOracle = getNpc(220450); //Akhal The Oracle.
					if (akhalTheOracle != null) {
						akhalTheOracle.getEffectController().removeEffect(21791); //Turning Tide.
					}
					instance.doOnAllPlayers(p -> {
						if (p.isOnline()) {
							PacketSendUtility.sendPacket(p, new SM_QUEST_ACTION(0, 0));
						}
					});
				}
				despawnNpc(npc);
			break;
			case 220450: //Akhal The Oracle.
				runtimeState().put("drakenseer.complete", true);
				cancelChallenge();
			    spawnExit();
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Drakenseer's Lair>");
			break;
		}
	}

	private void cancelChallenge() {
		cancelDeadline("start_message");
		cancelDeadline("warning_1");
		cancelDeadline("expire");
	}

	private void spawnExit() {
		if (getNpc(806240) == null) {
			spawn(806240, 299.1905f, 258.07004f, 319.67477f, (byte) 110); //Drakenseer's Lair Exit.
		}
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
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
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		//“玩家名”已离开战斗。 / "Player Name" has left the battle.
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
	}
}
