package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.*;
import java.util.concurrent.Future;


import com.aionemu.commons.utils.Rnd;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.ai.RetailPatternAI2;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai.RetailDynamicAreaEngine;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.summons.*;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 塔洛克空洞副本事件处理器。
 * Instance event handler for Talocs Hollow.
 * @author Encom
 */

@InstanceID(300190000)
public class TalocsHollowInstance extends GeneralInstanceHandler
{
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 已播放动画集合 / played-movie set */
	private final List<Integer> movies = new ArrayList<>();
		/** taloc 任务 / taloc task */
		private final List<Future<?>> talocTask = new ArrayList<>();
		/** 对象 / objects */
		private final Map<Integer, VisibleObject> objects = new LinkedHashMap<>();

	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
		doors = instance.getDoors();
		doors.get(49).setOpen(true);
		spawnHugeInsectEgg();
    }

	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
		switch (player.getRace()) {
			case ELYOS:
				if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				addTalocFruitE(player);
				addTalocTearsE(player);
				sendMovie(player, 434);
			break;
			case ASMODIANS:
				if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				addTalocFruitA(player);
				addTalocTearsA(player);
			    sendMovie(player, 438);
		    break;
		}
		// 你必须消灭塔洛克的敌人，才能获得强大物品。 / You must destroy the enemies of Taloc. It allows you to acquire objects with great power.
		sendMsgByRace(1400704, Race.PC_ALL, 5000);
		// 背包中有强大物品。使用塔洛克果实可变为强力形态。 / An object of great power waits in your cube. Transform into a mighty being with Taloc's Fruit.
		sendMsgByRace(1400752, Race.PC_ALL, 10000);
		// 背包中有强大物品。使用塔洛克之泪可发动强力空中攻击。 / An object of great power waits in your cube. Launch a powerful aerial attack with Taloc's Tears.
		sendMsgByRace(1400753, Race.PC_ALL, 15000);
		HTMLService.showHTML(player, GameStaticDataServices.htmlCache().getHTML("instances/talocHollow.xhtml"));
    }
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 * @param npc NPC / npc
	 */

	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		switch (npcId) {
			case 215456: //Shishir.
				registerDropItemIfAbsent(dropItems, npc, 185000088, 1); //Shishir's Corrosive Fluid.
				registerDropItemIfAbsent(dropItems, npc, 164000137, 1); //Shishir's Powerstone.
			break;
			case 215478: //Neith.
				registerDropItemIfAbsent(dropItems, npc, 185000108, 1); //Dorkin's Pocket Knife.
				registerDropItemIfAbsent(dropItems, npc, 164000139, 1); //Neith's Sleepstone.
			break;
			case 215482, 246241: //Gellmar / special-server Gellmar.
				registerDropItemIfAbsent(dropItems, npc, 164000138, 1); //Gellmar's Wardstone.
			break;
			case 215488, 246242: //Celestius / special-server Celestius.
				switch (Rnd.get(1, 5)) {
					case 1:
						registerDropItemIfAbsent(dropItems, npc, 190080005, 2); //低级随从契约。 / Lesser Minion Contract.
					break;
					case 2:
						registerDropItemIfAbsent(dropItems, npc, 190080006, 2); //高级随从契约。 / Greater Minion Contract.
					break;
					case 3:
						registerDropItemIfAbsent(dropItems, npc, 190080007, 2); //大型随从契约。 / Major Minion Contract.
					break;
					case 4:
						registerDropItemIfAbsent(dropItems, npc, 190080008, 2); //可爱随从契约。 / Cute Minion Contract.
					break;
					case 5:
						registerDropItemIfAbsent(dropItems, npc, 190200000, 50); //Minium.
					break;
				}
			break;
		}
	}

	/**
	 * 仅当基础掉落尚未包含目标物品时补充一条实例兜底掉落。
	 * Adds an instance fallback drop only when the base drop data does not already contain the item.
	 * <p>基础 NPC 掉落与任务掉落先于实例 Handler 注册；无条件追加会产生重复条目。索引暂用 1
	 * 占位，registerDrop 在释放掉落列表前会统一重排为唯一值。 /
	 * Base NPC and quest drops are registered before the instance handler; unconditional additions create
	 * duplicate entries. The index is a placeholder because registerDrop renumbers all entries before release.
	 * @param dropItems 当前掉落集合 / current drop set
	 * @param npc 死亡 NPC / dead NPC
	 * @param itemId 物品 ID / item id
	 * @param count 数量 / count
	 */
	private void registerDropItemIfAbsent(Set<DropItem> dropItems, Npc npc, int itemId, long count) {
		boolean alreadyRegistered = dropItems.stream()
			.anyMatch(dropItem -> dropItem.getDropTemplate().getItemId() == itemId);
		if (!alreadyRegistered) {
			dropItems.add(GameWorldServices.dropRegistrationService()
				.regDropItem(1, 0, npc.getObjectId(), itemId, count));
		}
	}

	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 * @param player 玩家 / player
	 * @param npc NPC / npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 700940: //Healing Plant.
				despawnNpc(npc);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 20000);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.MP, 20000);
			break;
			case 700941: //Huge Healing Plant.
				despawnNpc(npc);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 30000);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.MP, 30000);
			break;
		}
	}

    /**
     * 处理死亡事件。
     * Handle a death event.
     * @param npc NPC / npc
     */
    @Override
    public void onDie(Npc npc) {
		// 击杀者可能没有任何可归属玩家（无主或非玩家生物、宠物主人已离开已知列表），
		// 因此 player 允许为 null：只跳过玩家专属效果，世界推进照常执行。
		// The killer may have no attributable player (masterless or non-player creature, pet master gone from the known
		// list), so player may be null: only player-specific effects are skipped while world progression still runs.
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 215457: //Ancient Octanus.
				// 真端 Elim_Octaside_Door 在死亡事件中切换 1F Rush 条件；实例层兜底覆盖旧刷怪或 AI 未接管的情况。
				// Retail Elim_Octaside_Door switches the 1F rush condition on death; the instance fallback also covers legacy spawns or an AI that did not take over.
				sendMsgByRace(1400659, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "IDElim_1F_StartRush", 2, 0);
				// entity 51 is a static GEO placeable collision node, not a normal StaticDoor state pair.
				// entity 51 是静态 GEO 可放置碰撞节点，不是普通 StaticDoor 的门状态对。
				GameWorldServices.geoService().despawnPlaceableObject(instance.getMapId(), instance.getInstanceId(), 51);
			break;
			case 215480, 246240: //Queen Mosqua / special-server Queen Mosqua.
                deleteNpc(700738); //Huge Insect Egg.
				sendMovie(player, 435);
				// 解除召唤“恩盖乌斯与阿比拉”：无玩家归属时无法判定召唤主人，跳过但不阻断后续刷卵。
				// Release Summon "Engeius & Abyla": without a player attribution the owner is unknown, so skip it without
				// blocking the cracked-egg spawn below.
				if (player != null && player.getSummon() != null) {
					SummonsService.release(player.getSummon(), UnsummonType.UNSPECIFIED, false);
				}
				sp(700739, 653.63f, 838.66998f, 1304.72f, (byte) 0, 11, 0, 0, null); //Cracked Huge Insect Egg.
            break;
			case 215488, 246242: //Celestius / special-server Celestius.
				// 奖励只能发给可归属玩家；无归属时保留通关广播，不把奖励发给无关玩家。
				// Rewards require an attributable player; keep the completion broadcast and never reward an unrelated player.
				if (player != null) {
					ItemService.addItem(player, 188900011, 1); //Blessing Box Of Growth V.
					ItemService.addItem(player, 170170044, 1); //[Souvenir] Taloc's Komad Statue.
				}
				sendMsg("[Congratulation]: you finish <Taloc's Hollow>");
				// 真端由 Elim_ComadAe.on_killed_by_user 的 spawn 动作刷新卡斯帕的幻影；pattern 未接管时按同一份
				// 真端数据幂等补刷（先让 pattern 执行，实例内已有同模板 NPC 则跳过，不会产生第二份实体）。
				// Retail spawns Taloc's mirage from the Elim_ComadAe.on_killed_by_user spawn action; replay that same
				// retail data as an idempotent fallback when the pattern did not take over (the pattern runs first and an
				// existing copy in the instance is kept, so no second entity is created).
				spawnMirageIfPatternMissed(npc.getObjectTemplate().getTemplateId());
            break;
			case 700739: //Cracked Huge Insect Egg.
				// 真端 pattern `Elim_WindEventB` 用条件变量在卵的位置升起气流，并开启地面移动碰撞；
				// 实例层补一条幂等兜底，保证 pattern 未接管时气流视觉与移动碰撞仍然开启。
				// The retail pattern `Elim_WindEventB` raises the updraft at the egg through a condition
				// variable and switches on its ground moving collision; this idempotent instance-level
				// adapter keeps the wind visual and the collision alive when the pattern does not take over.
				RetailConditionSpawnEngine.setVariable(instance, "IDElim_2F_Wind", 1, 0);
				RetailDynamicAreaEngine.setEnabled(instance, "MOVING_COLLISION_WINDBOX", 100, true);
			break;
			case 700942: //Bug Fluid.
			    despawnNpc(npc);
			break;
        }
    }

	private void spawnHugeInsectEgg() {
	    SpawnTemplate IDElim2FEntity = SpawnEngine.addNewSingleTimeSpawn(300190000, 700738, 653.63f, 838.66998f, 1304.72f, (byte) 0);
		IDElim2FEntity.setEntityId(90);
		objects.put(700738, SpawnEngine.spawnObject(IDElim2FEntity, instanceId));
	}

	/**
	 * 真端 pattern 未接管时按同一份真端数据补刷卡斯帕的幻影。
	 * Replays the retail Taloc's mirage spawn when the pattern did not take over.
	 * <p>延迟一秒执行，让 Celestius 自己的 {@code on_killed_by_user} 动作先跑；实例内已有同模板 NPC 时
	 * {@link RetailPatternAI2#spawnRetailActionNpc} 直接返回，因此不会与 pattern 产生第二份实体。
	 * Runs one second later so Celestius' own {@code on_killed_by_user} actions run first; when an NPC of the same
	 * template already exists, {@link RetailPatternAI2#spawnRetailActionNpc} returns without spawning a second entity.
	 * @param ownerNpcId 死亡的 Celestius 模板 ID / template id of the dead Celestius
	 */
	private void spawnMirageIfPatternMissed(final int ownerNpcId) {
		talocTask.add(GameThreadPoolServices.threadPoolManager().schedule(() -> {
			if (!isInstanceDestroyed) {
				RetailPatternAI2.spawnRetailActionNpc(instance, ownerNpcId, "on_killed_by_user", "CaspaGhost_01");
			}
		}, 1000));
	}

	/**
	 * 玩家进入区域时处理。
	 * Handle a player entering a zone.
	 * @param player 玩家 / player
	 * @param zone 区域 / zone
	 */
	@Override
    public void onEnterZone(Player player, ZoneInstance zone) {
        if (zone.getAreaTemplate().getZoneName() == ZoneName.get("KINQUIDS_DEN_300190000")) {
            sendMovie(player, 463);
			// 烟雾正在释放。接触烟雾将破坏金奎德的屏障。 / Smoke is being discharged. Exposure to smoke will destroy Kinquid's Barrier.
			sendMsgByRace(1400660, Race.PC_ALL, 0);
	    } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("MOSQUAS_NEST_300190000")) {
			sendMovie(player, 464);
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("COCCOONING_CHAMBER_300190000")) {
			// 茧在蠕动——里面有东西！ / The cocoons are wriggling--something's inside!
			sendMsgByRace(1400475, Race.PC_ALL, 2000);
			// 你可救出茧中两名雷安之一。 / You can save one of the two Reians imprisoned in the cocoon.
			sendMsgByRace(1400630, Race.PC_ALL, 8000);
		}
    }

    private void sendMovie(Player player, int movie) {
        if (player == null) {
            return;
        }
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
    }

	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
		removeEffects(player);
		if (player.getSummon() != null) {
			SummonsService.release(player.getSummon(), UnsummonType.UNSPECIFIED, false);
		}
	}

	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
		removeEffects(player);
		if (player.getSummon() != null) {
			SummonsService.release(player.getSummon(), UnsummonType.UNSPECIFIED, false);
		}
	}

	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(182215618, storage.getItemCountByItemId(182215618)); // 卡斯帕的果实 / Taloc Fruit.
		storage.decreaseByItemId(182215593, storage.getItemCountByItemId(182215593)); // 卡斯帕的果实 / Taloc Fruit.
		storage.decreaseByItemId(182215619, storage.getItemCountByItemId(182215619)); // 卡斯帕的泪水 / Taloc's Tears.
		storage.decreaseByItemId(182215592, storage.getItemCountByItemId(182215592)); // 卡斯帕的泪水 / Taloc's Tears.
		storage.decreaseByItemId(164000137, storage.getItemCountByItemId(164000137)); //Shishir's Powerstone.
		storage.decreaseByItemId(164000138, storage.getItemCountByItemId(164000138)); //Gellmar's Wardstone.
		storage.decreaseByItemId(164000139, storage.getItemCountByItemId(164000139)); //Neith's Sleepstone.
	}

	private void addTalocFruitE(Player player) {
	    ItemService.addItem(player, 182215618, 1); // 卡斯帕的果实 / Taloc Fruit.
    }
	private void addTalocTearsE(Player player) {
        ItemService.addItem(player, 182215619, 1); // 卡斯帕的泪水 / Taloc's Tears.
    }
	private void addTalocFruitA(Player player) {
		ItemService.addItem(player, 182215593, 1); // 卡斯帕的果实 / Taloc Fruit.
    }
	private void addTalocTearsA(Player player) {
        ItemService.addItem(player, 182215592, 1); // 卡斯帕的泪水 / Taloc's Tears.
    }

	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(10251); // 卡斯帕的果实 / Taloc Fruit.
		effectController.removeEffect(10252); // 卡斯帕的果实 / Taloc Fruit.
	}

	private void despawnNpc(Npc npc) {
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
	 * 处理 sp。
	 * Handle sp.
	 * @param npcId NPC / NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * @param time 时间 / time
	 */

	protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
    /**
     * 处理 sp。
     * Handle sp.
     * @param npcId NPC / NPC
     * @param x X 坐标 / X
     * @param y Y 坐标 / Y
     * @param z Z 坐标 / Z
     * @param h 朝向 / h
     * @param time 时间 / time
     * @param msg 消息 / message
     * @param race 阵营 / race
     */

    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
    /**
     * 处理 sp。
     * Handle sp.
     * @param npcId NPC / NPC
     * @param x X 坐标 / X
     * @param y Y 坐标 / Y
     * @param z Z 坐标 / Z
     * @param h 朝向 / h
     * @param entityId 实体 ID / entity id
     * @param time 时间 / time
     * @param msg 消息 / message
     * @param race 阵营 / race
     */

    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        talocTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
     * @param npcId NPC / NPC
     * @param x X 坐标 / X
     * @param y Y 坐标 / Y
     * @param z Z 坐标 / Z
     * @param h 朝向 / h
     * @param time 时间 / time
     * @param walkerId 寻路器 ID / walkerId
     */

    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        talocTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
		instance.doOnAllPlayers(new Visitor<>() {
			/**
			 * 处理 visit。
			 * Handle visit.
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
				instance.doOnAllPlayers(new Visitor<>() {
                    /**
                     * 处理 visit。
                     * Handle visit.
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
		isInstanceDestroyed = true;
		talocTask.forEach(task -> task.cancel(false));
		talocTask.clear();
		movies.clear();
		doors.clear();
	}
}
