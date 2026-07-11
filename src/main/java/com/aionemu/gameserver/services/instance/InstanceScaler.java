package com.aionemu.gameserver.services.instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.aionemu.gameserver.configs.main.InstanceConfig;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 副本数值缩放器，按在线人数调整副本 NPC 属性。
 * Instance stat scaler adjusting NPC stats by player count.
 */
public final class InstanceScaler implements StatOwner {

	private static final InstanceScaler INSTANCE = new InstanceScaler();
	private static final Map<WorldMapInstance, Scaling> scalings = Collections.synchronizedMap(new WeakHashMap<>());

	private InstanceScaler() {
	}

	/**
	 * 玩家数量变化时重算。
	 * Recalculates when player count changes.
	 *
	 * instance
	 */
	public static void onPlayersChanged(WorldMapInstance instance) {
		int maxPlayers = InstanceService.getMaxPlayers(instance.getMapId());
		if (!canScale(instance, maxPlayers)) {
			return;
		}
		Scaling scaling = scalings.computeIfAbsent(instance, ignored -> new Scaling());
		synchronized (scaling) {
			int playerCount = countPlayers(instance);
			if (scaling.update(maxPlayers, playerCount)) {
				for (Npc npc : instance.getNpcs()) {
					scaleNpc(npc, scaling, shouldScale(npc, instance));
				}
			}
		}
	}

	/**
	 * 重载配置。
	 * Reloads configuration.
	 */
	public static void reload() {
		synchronized (scalings) {
			for (WorldMapInstance instance : new ArrayList<>(scalings.keySet())) {
				instance.getNpcs().forEach(npc -> npc.getGameStats().endEffect(INSTANCE));
			}
			scalings.clear();
		}
		if (InstanceConfig.SCALING_ENABLE) {
			Set<WorldMapInstance> instances = new HashSet<>();
			for (Player player : GameWorldBootstrapServices.world().getAllPlayers()) {
				if (player.isSpawned()) {
					instances.add(player.getPosition().getWorldMapInstance());
				}
			}
			instances.forEach(InstanceScaler::onPlayersChanged);
		}
	}

	/**
	 * 刷怪前应用缩放。
	 * Applies scaling before spawn.
	 *
	 * npc
	 */
	public static void onBeforeSpawn(Npc npc) {
		WorldMapInstance instance = npc.getPosition().getWorldMapInstance();
		if (!canScale(instance, InstanceService.getMaxPlayers(instance.getMapId()))) {
			return;
		}
		Scaling scaling = scalings.get(instance);
		if (scaling != null) {
			synchronized (scaling) {
				scaleNpc(npc, scaling, shouldScale(npc, instance));
			}
		}
	}

	private static boolean canScale(WorldMapInstance instance, int maxPlayers) {
		return InstanceConfig.SCALING_ENABLE && maxPlayers > 1 && instance.getParent().isInstanceType()
				&& !InstanceConfig.isScalingExcluded(instance.getMapId());
	}

	private static int countPlayers(WorldMapInstance instance) {
		return (int) instance.getPlayersInside().stream().filter(player -> player.getAccessLevel() == 0).count();
	}

	private static boolean shouldScale(Npc npc, WorldMapInstance instance) {
		return !npc.getLifeStats().isAlreadyDead() && instance.getPlayersInside().stream()
				.filter(player -> player.getAccessLevel() == 0)
				.anyMatch(npc::isEnemyFrom);
	}

	private static void scaleNpc(Npc npc, Scaling scaling, boolean apply) {
		npc.getGameStats().endEffect(INSTANCE);
		if (apply && !scaling.statFunctions.isEmpty()) {
			npc.getGameStats().addEffect(INSTANCE, scaling.statFunctions);
		}
	}

	static float calculateMultiplier(int maxPlayers, float floor, int playerCount) {
		return Math.max(floor, (float) Math.min(playerCount, maxPlayers) / maxPlayers);
	}

	static class Scaling {
		private int playerCount = -1;
		private List<InstanceScalerStatFunction> statFunctions = Collections.emptyList();

		boolean update(int maxPlayers, int currentPlayerCount) {
			if (playerCount == currentPlayerCount) {
				return false;
			}
			playerCount = currentPlayerCount;
			statFunctions = createStatFunctions(maxPlayers, currentPlayerCount);
			return true;
		}

		private List<InstanceScalerStatFunction> createStatFunctions(int maxPlayers, int currentPlayerCount) {
			List<InstanceScalerStatFunction> functions = new ArrayList<>();
			float hpMultiplier = calculateMultiplier(maxPlayers, InstanceConfig.SCALING_HP_FLOOR, currentPlayerCount);
			float damageMultiplier = calculateMultiplier(maxPlayers, InstanceConfig.SCALING_DMG_FLOOR, currentPlayerCount);
			if (hpMultiplier != 1) {
				functions.add(new InstanceScalerStatFunction(StatEnum.MAXHP, hpMultiplier));
			}
			if (damageMultiplier != 1) {
				functions.add(new InstanceScalerStatFunction(StatEnum.PHYSICAL_ATTACK, damageMultiplier));
				functions.add(new InstanceScalerStatFunction(StatEnum.MAGICAL_ATTACK, damageMultiplier));
				functions.add(new InstanceScalerStatFunction(StatEnum.BOOST_SPELL_ATTACK, damageMultiplier));
			}
			return functions;
		}
	}

	private static class InstanceScalerStatFunction extends StatFunction {
		private final float rate;

		private InstanceScalerStatFunction(StatEnum stat, float rate) {
			this.stat = stat;
			this.rate = rate;
		}

		@Override
		/**
		 * 应用效果。
		 * Applies the effect.
		 *
		 * stat
		 */
		public void apply(Stat2 stat) {
			stat.setBaseRate(stat.getBaseRate() * rate);
			stat.setBonusRate(stat.getBonusRate() * rate);
		}

		@Override
		/**
		 * getPriority 方法。
		 * getPriority method.
		 * result
		 */
		public int getPriority() {
			return 120;
		}
	}
}
