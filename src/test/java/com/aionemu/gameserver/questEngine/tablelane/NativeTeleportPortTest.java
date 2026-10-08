package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * col9 进入装配面门禁（2026-10-08 实机 10034 空副本事故）：复用已注册实例（零分配零注册）/
 * 未命中时分配下一可用实例 + 注册 / 传送携带最终 instanceId——裸传送会固定 instanceId=1 落到
 * 副本默认空实例（从不 spawn）。装配逻辑经 {@link NativeTeleportPort#assembleInstanceEntry}
 * 注入面直测（player 由实现透传，纯装配用例传 {@code null}）。
 * <p>
 * Gate for the case-9 entry assembly: reuse without allocating or registering, allocate+register on
 * a miss, and the instance-carrying teleport (a bare teleport pins instanceId=1 and lands in the
 * never-spawned default instance).
 */
class NativeTeleportPortTest {

	@Test
	void reusesTheRegisteredInstanceWithoutAllocatingOrRegistering() {
		RecordingSource source = new RecordingSource(4);
		RecordingInstanceTeleport teleport = new RecordingInstanceTeleport();
		NativeTeleportPort.assembleInstanceEntry(null, 300160000, 794.99f, 918.98f, 154f, 213, source, teleport);
		assertEquals(List.of(), source.calls(), "命中注册实例：零分配零注册");
		assertEquals(List.of("teleport:300160000:4:794.99:918.98:155.0:71"), teleport.calls(),
			"传送携带注册实例 id（z 抬 1.0、heading 度→6 位）");
	}

	@Test
	void allocatesAndRegistersOnAMiss() {
		RecordingSource source = new RecordingSource(-1);
		source.allocated = 7;
		RecordingInstanceTeleport teleport = new RecordingInstanceTeleport();
		NativeTeleportPort.assembleInstanceEntry(null, 300160000, 794.99f, 918.98f, 154f, 213, source, teleport);
		assertEquals(List.of("allocate:300160000", "register:300160000:7"), source.calls(),
			"未命中：分配下一可用实例并注册玩家");
		assertEquals(List.of("teleport:300160000:7:794.99:918.98:155.0:71"), teleport.calls(),
			"传送携带新分配的实例 id（绝不 = 裸传送固定 1 的口径）");
	}

	/** 记录式解析面（复用命中值可配、分配值可配）。 / A recording instance source. */
	private static final class RecordingSource implements NativeTeleportPort.InstanceSource {

		private final List<String> calls = new ArrayList<>();
		private final int registered;
		private int allocated = 2;

		RecordingSource(int registered) {
			this.registered = registered;
		}

		@Override
		public int registeredInstanceId(Player player, int worldId) {
			return registered;
		}

		@Override
		public int allocateInstanceId(Player player, int worldId) {
			calls.add("allocate:" + worldId);
			return allocated;
		}

		@Override
		public void register(Player player, int worldId, int instanceId) {
			calls.add("register:" + worldId + ":" + instanceId);
		}

		List<String> calls() {
			return List.copyOf(calls);
		}
	}

	/** 记录式带实例 id 传送面。 / A recording instance-carrying teleport. */
	private static final class RecordingInstanceTeleport implements NativeTeleportPort.InstanceTeleport {

		private final List<String> calls = new ArrayList<>();

		@Override
		public void teleport(Player player, int worldId, int instanceId, float x, float y, float z,
				byte heading) {
			calls.add("teleport:" + worldId + ":" + instanceId + ":" + x + ":" + y + ":" + z + ":" + heading);
		}

		List<String> calls() {
			return List.copyOf(calls);
		}
	}
}
