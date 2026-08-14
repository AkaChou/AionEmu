package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.schedule.RvrSchedule;
import com.aionemu.gameserver.configs.schedule.RvrSchedule.Rvr;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.rvr.RvrLocation;
import com.aionemu.gameserver.model.rvr.RvrStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.rvrspawns.RvrSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.rvrservice.DirectPortal;
import com.aionemu.gameserver.services.rvrservice.RvrStartRunnable;
import com.aionemu.gameserver.services.rvrservice.Rvrlf3df3;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 种族对战（RvR）服务，管理军团走廊、刷怪与倒计时广播。
 * Race vs Race service managing Legion Corridor, spawns, and countdown broadcasts.
 *
 * @author Rinzler (Encom)
 */
@Slf4j(topic = "com.aionemu.gameserver.services.SvsService")
public class RvrService {
	private static volatile ObjectProvider<RvrService> instanceProvider;
	private RvrSchedule rvrSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, RvrLocation> rvr;

	// 旅团将军的紧急命令 4.9.1 / Brigade General's Urgent Order 4.9.1
	private final ConcurrentMap<Integer, Rvrlf3df3<?>> activeRvr = new ConcurrentHashMap<Integer, Rvrlf3df3<?>>();
	// 重装特特兰/凯诺维坎 5.6 / Heavy Tetran/Kenovikan 5.6
	private Map<Integer, VisibleObject> adventPortal = new HashMap<>();
	private Map<Integer, VisibleObject> adventEffect = new HashMap<>();
	private Map<Integer, VisibleObject> adventControl = new HashMap<>();
	private Map<Integer, VisibleObject> adventDirecting = new HashMap<>();

	/**
	 * 初始化 RvR 地点并按和平状态刷怪。
	 * Initializes RvR locations and spawns them in the peace state.
	 */
	public void initRvrLocations() {
		if (CustomConfig.RVR_ENABLED) {
			rvr = DataManager.RVR_DATA.getRvrLocations();
			for (RvrLocation loc : getRvrLocations().values()) {
				spawn(loc, RvrStateType.PEACE);
			}
			log.info(I18n.get("log.50bcec05725e", rvr.size()));
		} else {
			rvr = Collections.emptyMap();
		}
	}

	/**
	 * 初始化 RvR 并加载定时计划。
	 * Initializes RvR and loads the schedule.
	 */
	public void initRvr() {
		if (CustomConfig.RVR_ENABLED) {
			log.info(I18n.get("log.7869fd4b7e68"));
		}
		reloadSchedule();
	}

	/**
	 * 重载 RvR Cron 计划（先取消旧任务）。
	 * Reloads the RvR cron schedule (cancels previous tasks first).
	 */
	public synchronized void reloadSchedule() {
		RvrSchedule newSchedule = CustomConfig.RVR_ENABLED ? RvrSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		rvrSchedule = newSchedule;
		if (rvrSchedule != null) {
			for (Rvr rvr : rvrSchedule.getRvrsList()) {
				for (String rvrTime : rvr.getRvrTimes()) {
					Runnable task = new RvrStartRunnable(rvr.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, rvrTime);
				}
			}
		}
	}

	/**
	 * 启动指定 ID 的种族对战活动。
	 * Starts the Race vs Race event for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startRvr(final int id) {
		if (CustomConfig.RVR_ENABLED) {
			Rvrlf3df3<?> directPortal = new DirectPortal(rvr.get(id));
			if (activeRvr.putIfAbsent(id, directPortal) != null) {
				return;
			}
			directPortal.start();
			rvrCountdownMsg(id);
			LF6RvrCountdownMsg(id);
			DF6RvrCountdownMsg(id);
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					stopRvr(id);
				}
			}, CustomConfig.RVR_DURATION * 3600 * 1000);
		}
	}

	/**
	 * 停止指定 ID 的种族对战活动。
	 * Stops the Race vs Race event for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopRvr(int id) {
		Rvrlf3df3<?> directPortal = activeRvr.remove(id);
		if (directPortal == null || directPortal.isFinished()) {
			return;
		}
		directPortal.stop();
	}

	/**
	 * 按状态在地点刷出对应 NPC。
	 * Spawns NPCs for the location according to the given state.
	 *
	 * location
	 * state type
	 */
	public void spawn(RvrLocation loc, RvrStateType rstate) {
		if (rstate.equals(RvrStateType.RVR)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getRvrSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				RvrSpawnTemplate rvrtemplate = (RvrSpawnTemplate) st;
				if (rvrtemplate.getRStateType().equals(rstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(rvrtemplate, 1));
				}
			}
		}
	}

		/**
	 * 广播军团走廊倒计时系统消息。
	 * Broadcasts Legion Corridor countdown system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean rvrCountdownMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 旅团将军的紧急命令。 / Brigade General's Urgent Order.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_DIRECT_PORTAL, 0);
					// 军团走廊已开启。 / The Legion's Corridor has opened.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_DIRECT_PORTAL_OPEN,
							20000);
					// 军团通道关闭后将自动返回。 / When the Legion's Corridor closes, you will automatically return to the
					// 你��时的入口。 / entrance where you came from.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_RVR_DIRECT_PORTAL_CLOSE_COMPULSION_TELEPORT, 60000);
					// 军团走廊将在 45 分钟后关闭。关闭后 / The Legion's Corridor will close in 45 minutes. Once the corridor is closed,
					// 联盟将自动解散，成员将自动 / the Alliance is automatically disbanded and members are automatically
					// 返回。 / returned.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_TIMER_NOTICE_01,
							900000);
					// 军团走廊将在 30 分钟后关闭。关闭后 / The Legion's Corridor will close in 30 minutes. Once the corridor is closed,
					// 联盟将自动解散，成员将自动 / the Alliance is automatically disbanded and members are automatically
					// 返回。 / returned.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_TIMER_NOTICE_02,
							1800000);
					// 军团走廊将在 15 分钟后关闭。关闭后 / The Legion's Corridor will close in 15 minutes. Once the corridor is closed,
					// 联盟将自动解散，成员将自动 / the Alliance is automatically disbanded and members are automatically
					// 返回。 / returned.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_TIMER_NOTICE_03,
							2700000);
					// 军团走廊将在 10 分钟后关闭。关闭后 / The Legion's Corridor will close in 10 minutes. Once the corridor is closed,
					// 联盟将自动解散，成员将自动 / the Alliance is automatically disbanded and members are automatically
					// 返回。 / returned.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_TIMER_NOTICE_04,
							3000000);
					// 军团走廊将在 5 分钟后关闭。关闭后 / The Legion's Corridor will close in 5 minutes. Once the corridor is closed,
					// 联盟将自动解散，成员将自动 / the Alliance is automatically disbanded and members are automatically
					// 返回。 / returned.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_TIMER_NOTICE_05,
							3300000);
					// 军团走廊将在 1 分钟后关闭。关闭后 / The Legion's Corridor will close in 1 minutes. Once the corridor is closed,
					// 联盟将自动解散，成员将自动 / the Alliance is automatically disbanded and members are automatically
					// 返回。 / returned.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_TIMER_NOTICE_06,
							3540000);
					// 炽天使防御商人维里内克已出现在因特尔蒂卡要塞。 / Seraphim Defender Merchant Wirinerk has appeared at Heiron Fortress.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_L_WIN, 3558000);
					// 谢迪姆防御商人吉鲁内克已出现在贝鲁斯兰要塞。 / Shedim Defender Merchant Girunerk has appeared at Beluslan Fortress.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_D_WIN, 3600000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	// 伊卢玛。 / Iluma.
	/**
	 * 广播 LF6 G1 阶段 1 刷怪系统消息。
	 * Broadcasts LF6 G1 phase-1 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean LF6G1Spawn01Msg(int id) {
		switch (id) {
		case 3:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 魔族战舰将在 10 分钟后入侵。 / An Asmodian warship will invade in 10 minutes.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_G1_Spawn_01);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 LF6 G1 阶段 2 刷怪系统消息。
	 * Broadcasts LF6 G1 phase-2 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean LF6G1Spawn02Msg(int id) {
		switch (id) {
		case 3:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 魔族战舰将在 5 分钟后入侵。 / An Asmodian warship will invade in 5 minutes.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_G1_Spawn_02);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 LF6 G1 阶段 3 刷怪系统消息。
	 * Broadcasts LF6 G1 phase-3 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean LF6G1Spawn03Msg(int id) {
		switch (id) {
		case 3:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 魔族战舰将在 3 分钟后入侵。 / An Asmodian warship will invade in 3 minutes.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_G1_Spawn_03);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 LF6 G1 阶段 4 刷怪系统消息。
	 * Broadcasts LF6 G1 phase-4 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean LF6G1Spawn04Msg(int id) {
		switch (id) {
		case 3:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 魔族战舰将在 1 分钟后入侵。 / An Asmodian warship will invade in 1 minute.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_G1_Spawn_04, 0);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 LF6 G1 阶段 5 刷怪系统消息。
	 * Broadcasts LF6 G1 phase-5 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean LF6G1Spawn05Msg(int id) {
		switch (id) {
		case 3:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 魔族战舰入侵。 / Asmodian warship Invasion.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_G1_Spawn_05, 0);
					if (player.getRace() == Race.ELYOS) {
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of the Valley
						// 失落的。 / of the Lost.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_B_G2_Spawn_Chat_MSG, 10000);
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of the Coast of
						// 光剥夺者。 / the Light-Deprived.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_B2_G2_Spawn_Chat_MSG, 20000);
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of the
						// 五彩沼泽。 / Five-colored Marshland.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_C_G2_Spawn_Chat_MSG, 30000);
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of Black Wind
						// 山谷。 / Valley.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_D_G2_Spawn_Chat_MSG, 40000);
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of the Serene
						// 精灵之森。 / Forest of Spirits.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_E_G2_Spawn_Chat_MSG, 50000);
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of the Forest
						// 休眠生命。 / of Dormant Life.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_F_G2_Spawn_Chat_MSG, 60000);
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of the Ancient
						// 生命神殿。 / Temple of Life.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_F2_G2_Spawn_Chat_MSG, 70000);
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of the Plateau
						// 西风的。 / of Zephyr.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_G_G2_Spawn_Chat_MSG, 80000);
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of the Krall
						// 奥德矿。 / Aether Mine.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_H_G2_Spawn_Chat_MSG, 90000);
						// 执政官突击护卫舰即将抵达天空岛。 / The Archon Assault Frigate will soon arrive at the Sky Island of Red Mushroom
						// 山谷。 / Valley.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LF6_I_G2_Spawn_Chat_MSG, 100000);
						// 魔族护卫舰指挥官已抵达。 / The Asmodian Frigate Commander has arrived.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_G1_Boss_Spawn_01,
								110000);
					}
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 LF6 G2 事件开始系统消息。
	 * Broadcasts LF6 G2 event-start system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean LF6EventG2Start02Msg(int id) {
		switch (id) {
		case 3:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 军官落败后，魔族士兵正在撤退。 / The Asmodian Troopers are retreating after the defeat of their officers.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_LF6_Event_G2_Start_01,
							1800000);
					// 魔族部队侦察完成后即将返回。 / The Asmodian Troopers will shortly return after completing reconnaissance.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_LF6_Event_G2_Start_03,
							1820000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 LF6 侧 RvR 倒计时系统消息。
	 * Broadcasts LF6-side RvR countdown system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean LF6RvrCountdownMsg(int id) {
		switch (id) {
		case 3:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 天族护卫舰入侵将在 10 分钟后结束。 / The Elyos frigate invasion will end in 10 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_Evett_G1_Time_End_01,
							3000000);
					// 天族护卫舰入侵即将结束。 / The Elyos frigate invasion is about to end.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_Evett_G1_Time_End_02,
							3300000);
					// 天族护卫舰入侵已结束。 / The Elyos frigate invasion has ended.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_Evett_G1_Time_End_03,
							3540000);
					// 对天族战舰的防御失败。魔族已进攻。 / The defense against the Elyos warship failed. The Asmodians have attacked
					// 艾瑞尔圣所。 / Ariel's Sanctuary.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_LF6_Event_G1_Defence_Failed,
							3600000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	// 诺斯珀德。 / Norsvold.
	/**
	 * 广播 DF6 G1 阶段 1 刷怪系统消息。
	 * Broadcasts DF6 G1 phase-1 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean DF6G1Spawn01Msg(int id) {
		switch (id) {
		case 4:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 天族战舰将在 10 分钟后入侵。 / An Elyos warship will invade in 10 minutes.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_G1_Spawn_01);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 DF6 G1 阶段 2 刷怪系统消息。
	 * Broadcasts DF6 G1 phase-2 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean DF6G1Spawn02Msg(int id) {
		switch (id) {
		case 4:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 天族战舰将在 5 分钟后入侵。 / An Elyos warship will invade in 5 minutes.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_G1_Spawn_02);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 DF6 G1 阶段 3 刷怪系统消息。
	 * Broadcasts DF6 G1 phase-3 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean DF6G1Spawn03Msg(int id) {
		switch (id) {
		case 4:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 天族战舰将在 3 分钟后入侵。 / An Elyos warship will invade in 3 minutes.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_G1_Spawn_03);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 DF6 G1 阶段 4 刷怪系统消息。
	 * Broadcasts DF6 G1 phase-4 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean DF6G1Spawn04Msg(int id) {
		switch (id) {
		case 4:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 天族战舰将在 1 分钟后入侵。 / An Elyos warship will invade in 1 minute.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_G1_Spawn_04, 10000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 DF6 G1 阶段 5 刷怪系统消息。
	 * Broadcasts DF6 G1 phase-5 spawn system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean DF6G1Spawn05Msg(int id) {
		switch (id) {
		case 4:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 天族战舰入侵。 / Elyos warship Invasion.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_G1_Spawn_05, 0);
					if (player.getRace() == Race.ASMODIANS) {
						// 守护者突击护卫舰即将抵达……的天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of the
						// 羽枝森林。 / Feather Bough Forest.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_B_G2_Spawn_Chat_MSG, 10000);
						// 守护者突击护卫舰即将抵达……的天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of the
						// 斯皮里图斯领地。 / Territory of Spiritus.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_B2_G2_Spawn_Chat_MSG, 20000);
						// 守护者突击护卫舰即将抵达天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of the Cursed
						// 峡谷。 / Canyon.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_C_G2_Spawn_Chat_MSG, 30000);
						// 守护者突击护卫舰即将抵达天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of Kalidag
						// 峡谷。 / Canyon.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_D_G2_Spawn_Chat_MSG, 40000);
						// 守护者突击护卫舰即将抵达……的天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of the
						// 审判高原。 / Plateau of Judgment.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_E_G2_Spawn_Chat_MSG, 50000);
						// 守护者突击护卫舰即将抵达天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of the Blue
						// 幻象森林。 / Illusion Forest.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_F_G2_Spawn_Chat_MSG, 60000);
						// 守护者突击护卫舰即将抵达天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of the Ruins
						// 失落时间的。 / of Lost Time.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_F2_G2_Spawn_Chat_MSG, 70000);
						// 守护者突击护卫舰即将抵达天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of the Lake
						// 生命的。 / of Life.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_G_G2_Spawn_Chat_MSG, 80000);
						// 守护者突击护卫舰即将抵达天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of Black Mane
						// 山脉。 / Mountains.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_H_G2_Spawn_Chat_MSG, 90000);
						// 守护者突击护卫舰即将抵达天空岛。 / The Guardian Assault Frigate will soon arrive at the Sky Island of Saphora
						// 森林。 / Forest.
						PacketSendUtility.playerSendPacketTime(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DF6_I_G2_Spawn_Chat_MSG, 100000);
						// 天族护卫舰指挥官已抵达。 / The Elyos Frigate Commander has arrived.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_DF6_G1_Boss_Spawn_01,
								110000);
					}
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 DF6 G2 事件开始系统消息。
	 * Broadcasts DF6 G2 event-start system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean DF6EventG2Start02Msg(int id) {
		switch (id) {
		case 4:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 军官落败后，埃托斯正在撤退。 / The Aetos are retreating after the defeat of their officers.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_DF6_Event_G2_Start_01,
							1900000);
					// 天族部队侦察完成后即将返回。 / The Elyos Troopers will shortly return after completing reconnaissance.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_DF6_Event_G2_Start_03,
							1920000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 DF6 侧 RvR 倒计时系统消息。
	 * Broadcasts DF6-side RvR countdown system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean DF6RvrCountdownMsg(int id) {
		switch (id) {
		case 4:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 魔族护卫舰入侵将在 10 分钟后结束。 / The Asmodian frigate invasion will end in 10 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_Evett_G1_Time_End_01,
							3050000);
					// 魔族护卫舰入侵即将结束。 / The Asmodian frigate invasion is about to end.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_Evett_G1_Time_End_02,
							3290000);
					// 魔族护卫舰入侵已结束。 / The Asmodian frigate invasion has ended.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LF6_Evett_G1_Time_End_03,
							3530000);
					// 对魔族战舰的防御失败。天族已进攻。 / The defense against the Asmodian warship failed. The Elyos have attacked
					// 阿兹菲尔圣所。 / Azphel's Sanctuary.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_DF6_Event_G1_Defence_Failed,
							3590000);
				}
			});
			return true;
		default:
			return false;
		}
	}

		/**
	 * 广播 F6 突袭开始系统消息。
	 * Broadcasts F6 raid-start system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean F6RaidStart(int id) {
		switch (id) {
		case 5:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 执政官武器入侵。 / Archon's Weapon Invasion.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_Start_LF6);
					// 古代武器入侵。 / Ancient's Weapon Invasion.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_Start_DF6);
					// 诺斯珀德塔碎片回收行动。 / Norsvold Tower Fragment Retrieval Operation.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_InvasionStart_Light);
					// 伊卢玛塔碎片回收行动。 / Iluma Tower Fragment Retrieval Operation.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_InvasionStart_Dark);
					// 入侵者出现，正通过碎片能量强化。摧毁 / An intruder has appeared, strengthening via the fragment's energy. Destroy
					// 返回前全部碎片。 / all fragments before you return.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_BossSpawn__MSG,
							20000);
					// 凯诺维坎已进入该区域。 / Kenovikan has entered the region.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_Spawn_Start_MSG,
							30000);
					// 特特兰已进入该区域。 / Tetran has entered the region.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_Spawn_Start_Dark_MSG, 40000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 F6 突袭 5 分钟倒计时系统消息。
	 * Broadcasts F6 raid 5-minute countdown system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean F6RaidStart5Minute(int id) {
		switch (id) {
		case 5:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 空间扭曲了……你应该去看看。 / Space has been distorted... You should look into that.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_Warning_MSG, 0);
					// 敌对阵营的渗透行动入口即将开放。 / The infiltration operation entrance for the opposing faction will open soon.
					// 请参与此行动。 / Please participate in this operation.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_Spawn_DF6_Attack_MSG, 20000);
					// 检测到特特兰的入侵。 / Tetran's intrusion was detected.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_Spawn_DF6_5minute_MSG, 40000);
					// 检测到凯诺维坎的入侵。 / Kenovikan's intrusion was detected.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_Spawn_LF6_5minute_MSG, 50000);
					// 凯诺维坎即将到达。阻止魔族入侵！ / Kenovikan will arrive soon. Stop the Asmodian invasion!
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_ST_BossSpawn_MSG,
							70000);
					// 特特兰即将到达。阻止天族入侵！ / Tetran will arrive soon. Stop the Elyos invasion!
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_F6_Raid_ST_Dark_BossSpawn_MSG, 80000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出 RvR 入侵控制类特效/NPC。
	 * Spawns RvR advent control effect/NPC.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已刷出 / whether spawned
	 */
	public boolean adventControlSP(int id) {
		switch (id) {
		case 5:
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210100000, 702529, 2722.799f, 1424.293f, 227.375f, (byte) 53),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220110000, 702529, 2478.824f, 1804.861f, 216.271f, (byte) 56),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出 RvR 入侵视觉特效。
	 * Spawns RvR advent visual effect.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已刷出 / whether spawned
	 */
	public boolean adventEffectSP(int id) {
		switch (id) {
		case 5:
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210100000, 702549, 2722.799f, 1424.293f, 227.375f, (byte) 53),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220110000, 702549, 2478.824f, 1804.861f, 216.271f, (byte) 56),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出 RvR 入侵传送门。
	 * Spawns RvR advent portal.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已刷出 / whether spawned
	 */
	public boolean adventPortalSP(int id) {
		switch (id) {
		case 5:
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210100000, 702550, 2722.799f, 1424.293f, 227.375f, (byte) 53),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220110000, 702550, 2478.824f, 1804.861f, 216.271f, (byte) 56),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出 RvR 入侵引导/指向特效。
	 * Spawns RvR advent directing effect.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已刷出 / whether spawned
	 */
	public boolean adventDirectingSP(int id) {
		switch (id) {
		case 5:
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210100000, 855231, 2722.799f, 1424.293f, 227.375f, (byte) 53),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220110000, 855231, 2478.824f, 1804.861f, 216.271f, (byte) 56),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 清除地点已刷出的 NPC。
	 * Despawns NPCs previously spawned at the location.
	 *
	 * location
	 */
	public void despawn(RvrLocation loc) {
		if (loc.getSpawned() == null) {
			return;
		}
		for (VisibleObject obj : new ArrayList<VisibleObject>(loc.getSpawned())) {
			Npc spawned = (Npc) obj;
			spawned.setDespawnDelayed(true);
			if (spawned.getAggroList().getList().isEmpty()) {
				spawned.getController().cancelTask(TaskId.RESPAWN);
				obj.getController().onDelete();
			}
		}
		loc.getSpawned().clear();
	}

	/**
	 * 判断指定 RvR 是否进行中。
	 * Checks whether the RvR with the given id is in progress.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否进行中 / whether in progress
	 */
	public boolean isRvrInProgress(int id) {
		return activeRvr.containsKey(id);
	}

	/**
	 * 获取进行中的 RvR 实例映射。
	 * Returns the map of active RvR instances.
	 *
	 * @return 活动实例映射 / active instances map
	 */
	public Map<Integer, Rvrlf3df3<?>> getActiveRvr() {
		return activeRvr;
	}

	/**
	 * 获取活动持续时长（小时）。
	 * Returns the event duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.RVR_DURATION;
	}

	/**
	 * 按 ID 获取 RvR 地点。
	 * Returns the RvR location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public RvrLocation getRvrLocation(int id) {
		return rvr.get(id);
	}

	/**
	 * 获取全部 RvR 地点。
	 * Returns all RvR locations.
	 *
	 * locations map
	 */
	public Map<Integer, RvrLocation> getRvrLocations() {
		return rvr;
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static RvrService getInstance() {
		ObjectProvider<RvrService> provider = instanceProvider;
		if (provider == null) {
			return RvrServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> RvrServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<RvrService> instanceProvider) {
		RvrService.instanceProvider = instanceProvider;
	}

	private static class RvrServiceHolder {
		private static final RvrService INSTANCE = new RvrService();
	}
}
