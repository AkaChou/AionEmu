package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 欧比斯碎片副本事件处理器。
 * Instance event handler for Abyssal Splinter.
 *
 * @author Encom
 */

@InstanceID(300220000)
public class AbyssalSplinterInstance extends GeneralInstanceHandler {
	/** 发光水虫计数 / luminous waterworm count */
	private int luminousWaterworm;
	/** 巨型奥德碎片 / huge aether fragment */
		private int hugeAetherFragment;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** abyssalsplinter 任务 / abyssal splinter task */
		private final List<Future<?>> abyssalSplinterTask = new ArrayList<Future<?>>();
	
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
     * 玩家进入副本时处理。
     * Handle a player entering the instance.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onEnterInstance(Player player) {
        super.onEnterInstance(player);
        abyssalBlessing();
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
			case 216945: //Enos Watcher.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000104, 1)); //Abyssal Fragment.
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
			case 216945: //Enos Watcher.
				doors.get(15).setOpen(true);
				doors.get(16).setOpen(true);
				doors.get(18).setOpen(true);
				doors.get(69).setOpen(true);
			break;
			case 216948: //Rukril.
			case 216949: //Ebonsoul.
			    Npc rukril = instance.getNpc(216948); //Rukril.
			    Npc ebonsoul = instance.getNpc(216949); //Ebonsoul.
			    if (isDead(rukril) && isDead(ebonsoul)) {
					// 出现了一个宝箱。 / A treasure chest has appeared.
					sendMsgByRace(1400636, Race.PC_ALL, 3000);
		            spawn(700934, 408.10938f, 650.9015f, 439.28332f, (byte) 66); // 创世宝箱 / Genesis Treasure Box
		            spawn(700934, 402.40375f, 655.55237f, 439.26288f, (byte) 33); // 创世宝箱 / Genesis Treasure Box
		            spawn(700934, 406.74445f, 655.5914f, 439.2548f, (byte) 100); // 创世宝箱 / Genesis Treasure Box
		            spawn(700936, 404.891f, 650.2943f, 439.2548f, (byte) 130); // Abyssal Treasure Box
					sp(700955, npc.getX(), npc.getY(), npc.getZ(), (byte) 0, 3000, 0, null); //Huge Aether Fragment.
				}
				despawnNpc(npc);
			break;
			case 216950: //Kaluva 4Th Fragment.
			    despawnNpc(npc);
				// 出现了一个宝箱。 / A treasure chest has appeared.
				sendMsgByRace(1400636, Race.PC_ALL, 3000);
		        spawn(700934, 601.2931f, 584.66705f, 422.9955f, (byte) 6); // 创世宝箱 / Genesis Treasure Box
		        spawn(700934, 597.2156f, 583.95416f, 423.3474f, (byte) 66); // 创世宝箱 / Genesis Treasure Box
		        spawn(700934, 602.9586f, 589.2678f, 422.8296f, (byte) 100); // 创世宝箱 / Genesis Treasure Box
		        spawn(700935, 598.82776f, 588.25946f, 422.7739f, (byte) 113); // Abyssal Treasure Box
				sp(700955, npc.getX(), npc.getY(), npc.getZ(), (byte) 0, 3000, 0, null); //Huge Aether Fragment.
			break;
			case 216951: //Pazuzu.
			    despawnNpc(npc);
				// 出现了一个宝箱。 / A treasure chest has appeared.
				sendMsgByRace(1400636, Race.PC_ALL, 3000);
		        spawn(700934, 651.53204f, 357.085f, 466.1315f, (byte) 66); // 创世宝箱 / Genesis Treasure Box
		        spawn(700934, 647.00446f, 357.2484f, 465.8960f, (byte) 0); // 创世宝箱 / Genesis Treasure Box
		        spawn(700934, 653.8384f, 360.39508f, 466.4391f, (byte) 100); // 创世宝箱 / Genesis Treasure Box
		        spawn(700860, 649.24286f, 361.33755f, 466.0427f, (byte) 33); // Abyssal Treasure Box
                if (Rnd.chance(12))
			    spawn(700861, 661.061f, 357.587f, 465.991f, (byte) 100, 67); // Pazuzu's Treasure Box
				sp(700955, npc.getX(), npc.getY(), npc.getZ(), (byte) 0, 3000, 0, null); //Huge Aether Fragment.
			break;
			case 216952: //Yamennes Blindsight.
			    despawnNpc(npc);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Abyssal Splinter>");
		        spawn(700934, 326.978f, 729.8414f, 197.7078f, (byte) 16); // 创世宝箱 / Genesis Treasure Box
		        spawn(700934, 326.5296f, 735.13324f, 197.6681f, (byte) 66); // 创世宝箱 / Genesis Treasure Box
		        spawn(700934, 329.8462f, 738.41095f, 197.7329f, (byte) 3); // 创世宝箱 / Genesis Treasure Box
		        spawn(700937, 330.891f, 733.2943f, 197.6404f, (byte) 113); // Abyssal Treasure Box
				spawn(730317, 308.19241f, 756.48370f, 196.75534f, (byte) 0, 123); //Abyssal Splinter Exit.
			break;
			case 216960: //Yamennes Painflare.
			    despawnNpc(npc);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Abyssal Splinter>");
		        spawn(700934, 326.978f, 729.8414f, 197.7078f, (byte) 16); // 创世宝箱 / Genesis Treasure Box
		        spawn(700934, 326.5296f, 735.13324f, 197.6681f, (byte) 66); // 创世宝箱 / Genesis Treasure Box
		        spawn(700934, 329.8462f, 738.41095f, 197.7329f, (byte) 3); // 创世宝箱 / Genesis Treasure Box
		        spawn(700938, 330.891f, 733.2943f, 197.6404f, (byte) 113); // Abyssal Treasure Box
				spawn(730317, 308.19241f, 756.48370f, 196.75534f, (byte) 0, 123); //Abyssal Splinter Exit.
			break;
			case 700955: //Huge Aether Fragment.
				hugeAetherFragment++;
				if (hugeAetherFragment == 1) {
					// 巨大奥德碎片被摧毁，神器已不稳定！ / The destruction of the Huge Aether Fragment has destabilized the artifact!
				    sendMsgByRace(1400689, Race.PC_ALL, 0);
				} else if (hugeAetherFragment == 2) {
					// 巨大奥德碎片被摧毁，神器守护者进入警戒！ / The destruction of the Huge Aether Fragment has put the artifact protector on alert!
				    sendMsgByRace(1400690, Race.PC_ALL, 0);
				} else if (hugeAetherFragment == 3) {
					// 巨大奥德碎片被摧毁导致神器异常，神器守护者暴怒！ / The destruction of the Huge Aether Fragment has caused abnormality on the artifact. The artifact protector is furious!
				    sendMsgByRace(1400691, Race.PC_ALL, 0);
				}
				despawnNpc(npc);
			break;
			case 281909: //Luminous Waterworm.
                Npc pazuzu = instance.getNpc(216951); //Pazuzu.
				luminousWaterworm++;
				if (pazuzu != null) {
					if (luminousWaterworm == 5) {
                        pazuzu.getEffectController().removeEffect(19291); //Replenishment.
                    }
                }
				despawnNpc(npc);
            break;
		}
	}
	
	private void abyssalBlessing() {
		for (Player p: instance.getPlayersInside()) {
			SkillTemplate st =  DataManager.SKILL_DATA.getSkillTemplate(19283); //Abyssal Blessing.
			Effect e = new Effect(p, p, st, 1, st.getEffectsDuration(9));
			e.initialize();
			e.applyEffect();
		}
	}
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000104, storage.getItemCountByItemId(185000104)); //Abyssal Fragment.
	}
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(19283); //Abyssal Blessing.
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
		doors.clear();
	}
	
	private void stopInstanceTask() {
        for (Future<?> task : abyssalSplinterTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 */
	
	protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 * message
	 * 阵营 / race
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * entity id
	 * time
	 * message
	 * 阵营 / race
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        abyssalSplinterTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 * walkerId
	 */
	
	protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        abyssalSplinterTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
	
	private boolean isDead(Npc npc) {
		return (npc == null || npc.getLifeStats().isAlreadyDead());
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
