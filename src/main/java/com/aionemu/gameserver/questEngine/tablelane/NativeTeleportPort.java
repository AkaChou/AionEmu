package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 原生任务车道的传送端口：DD 附加动作 case 3（`Teleport To`）与 case 9（`Enter Instance`）的唯一出口。
 * <p>
 * case 3 原版执行器 → `IUserImp::Teleport(world, x, y, z+1, heading, 1)`（NP `NpcAIOrderFunc.cpp`），
 * 原版 z 抬 1.0 落地、heading = 度（宿主内部转 6 位朝向）；case 9 原版 = `User::EnterInstance` 按
 * `instance_creation` 落点别名进副本——本服等价面 = **复用已注册实例，否则下一可用实例 + 注册**
 * （与 typed 车道 `teleport-player-next-available-instance` 及旧 handler 的
 * `getNextAvailableInstance + registerPlayerWithInstance` 同语义）。若以无 instanceId 的裸传送进副本，
 * 会落到副本世界的默认空实例（`instanceId=1`，从不 spawn）——2026-10-08 实机 10034 即此形。
 * <p>
 * The teleport port of the native quest lane: the single exit for the DD Teleport (case 3) and
 * Enter Instance (case 9) extra actions. Case 3 mirrors the retail executor (`IUserImp::Teleport`
 * with z+1 and degree heading); case 9 mirrors `User::EnterInstance` — reuse the player's
 * registered instance, otherwise allocate the next available one and register the player (the same
 * semantics as the typed lane's `teleport-player-next-available-instance` and the retired handler
 * chain). A bare teleport without an instance id lands in the world's default instance
 * (`instanceId=1`), which is never spawned — the 2026-10-08 live shape for quest 10034.
 */
public interface NativeTeleportPort {

	/** 传送到世界坐标（z 抬 1.0；heading 为度）。 / Teleports to world coords (z+1; heading in degrees). */
	void teleport(Player player, int worldId, float x, float y, float z, int headingDegrees);

	/**
	 * 进入副本世界（z 抬 1.0；heading 为度）：命中玩家已注册的实例则复用；否则分配下一可用实例
	 * （创建期完成 spawn 装配）并把玩家注册进去，再带 instanceId 传送。非副本世界退回裸传送。
	 * Enters the instance world (z+1; degree heading): reuses the player's registered instance when
	 * present, otherwise allocates the next available instance (fully spawned at creation) and
	 * registers the player before teleporting with that instance id. A non-instance world falls back
	 * to the bare teleport.
	 */
	void enterInstance(Player player, int worldId, float x, float y, float z, int headingDegrees);

	/**
	 * 副本进入的解析面（复用已注册 / 分配下一可用 / 注册；生产 = {@link InstanceService}）。
	 * The instance-resolution seam (reuse / allocate / register; production = InstanceService).
	 */
	interface InstanceSource {

		/** 玩家在该世界已注册的实例 id；无注册返回 {@code -1}。 / The registered instance id, or -1. */
		int registeredInstanceId(Player player, int worldId);

		/** 分配下一可用实例（创建期完成 spawn 装配）并返回其 id。 / Allocates the next available instance. */
		int allocateInstanceId(Player player, int worldId);

		/** 把玩家注册进指定实例。 / Registers the player with the instance. */
		void register(Player player, int worldId, int instanceId);

		/** 生产实现。 / The production implementation. */
		static InstanceSource live() {
			return LiveSource.INSTANCE;
		}

		/** 生产实现持有者（InstanceService / 世界引导）。 / Live holder (InstanceService / world bootstrap). */
		final class LiveSource implements InstanceSource {

			static final LiveSource INSTANCE = new LiveSource();

			private LiveSource() {
			}

			@Override
			public int registeredInstanceId(Player player, int worldId) {
				WorldMapInstance registered = InstanceService.getRegisteredInstance(worldId, player.getObjectId());
				return registered == null ? -1 : registered.getInstanceId();
			}

			@Override
			public int allocateInstanceId(Player player, int worldId) {
				return InstanceService.getNextAvailableInstance(worldId).getInstanceId();
			}

			@Override
			public void register(Player player, int worldId, int instanceId) {
				WorldMapInstance instance = GameWorldBootstrapServices.world().getWorldMap(worldId)
					.getWorldMapInstanceById(instanceId);
				InstanceService.registerPlayerWithInstance(instance, player);
			}
		}
	}

	/** 带实例 id 的传送面（生产 = {@code TeleportService2.teleportTo} 六参重载）。 / The instance-carrying teleport seam. */
	interface InstanceTeleport {

		/** 传送（z 已含抬升、heading 已转 6 位朝向）。 / Teleports (z pre-lifted, heading pre-converted). */
		void teleport(Player player, int worldId, int instanceId, float x, float y, float z, byte heading);

		/** 生产实现。 / The production implementation. */
		static InstanceTeleport live() {
			return LiveTeleport.INSTANCE;
		}

		/** 生产实现持有者（TeleportService2）。 / Live holder (TeleportService2). */
		final class LiveTeleport implements InstanceTeleport {

			static final LiveTeleport INSTANCE = new LiveTeleport();

			private LiveTeleport() {
			}

			@Override
			public void teleport(Player player, int worldId, int instanceId, float x, float y, float z,
					byte heading) {
				TeleportService2.teleportTo(player, worldId, instanceId, x, y, z, heading);
			}
		}
	}

	/**
	 * 副本进入的共享装配（Live 与门禁共用）：命中已注册实例则复用（不分配、不注册），否则分配下一
	 * 可用实例并把玩家注册进去；最终以该实例 id 传送（z 抬 1.0、heading 度→6 位，与 case 3 同形）。
	 * The shared entry assembly (used by Live and the gate): reuse the registered instance without
	 * allocating or registering, otherwise allocate and register, then teleport with that instance id.
	 */
	static void assembleInstanceEntry(Player player, int worldId, float x, float y, float z, int headingDegrees,
			InstanceSource source, InstanceTeleport teleport) {
		int registered = source.registeredInstanceId(player, worldId);
		int instanceId = registered >= 0 ? registered : source.allocateInstanceId(player, worldId);
		if (registered < 0) {
			source.register(player, worldId, instanceId);
		}
		teleport.teleport(player, worldId, instanceId, x, y, z + 1.0f,
			(byte) Math.floorDiv(headingDegrees, 3));
	}

	/** 生产实现（TeleportService2）。 / The live implementation (TeleportService2). */
	static NativeTeleportPort live() {
		return Live.INSTANCE;
	}

	/** 生产实现持有者。 / Live implementation holder. */
	final class Live implements NativeTeleportPort {

		private static final NativeTeleportPort INSTANCE = new Live();

		private Live() {
		}

		@Override
		public void teleport(Player player, int worldId, float x, float y, float z, int headingDegrees) {
			TeleportService2.teleportTo(player, worldId, x, y, z + 1.0f, (byte) Math.floorDiv(headingDegrees, 3));
		}

		@Override
		public void enterInstance(Player player, int worldId, float x, float y, float z, int headingDegrees) {
			WorldMapTemplate template = DataManager.WORLD_MAPS_DATA.getTemplate(worldId);
			if (template == null || !template.isInstance()) {
				// 非副本世界（或模板缺失）：退回裸传送（照 TeleportService2.teleportToNpc 的既有判定口径，
				// 另防模板缺失 NPE）。
				// Not an instance world (or template missing): fall back to the bare teleport, mirroring
				// the existing TeleportService2.teleportToNpc check and additionally guarding the null template.
				teleport(player, worldId, x, y, z, headingDegrees);
				return;
			}
			assembleInstanceEntry(player, worldId, x, y, z, headingDegrees, InstanceSource.live(),
				InstanceTeleport.live());
		}
	}
}
