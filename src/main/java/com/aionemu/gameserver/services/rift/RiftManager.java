package com.aionemu.gameserver.services.rift;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.controllers.RVController;
import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.rift.RiftLocation;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.NpcKnownList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * 裂隙管理器，负责裂隙/漩涡 NPC 的生成模板登记与实例生成。
 * Rift manager responsible for spawn-template registration and rift/vortex NPC spawning.
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class RiftManager {

	private static volatile ObjectProvider<RiftManager> instanceProvider;
	private static List<Npc> rifts = new CopyOnWriteArrayList<Npc>();
	private static Map<String, SpawnTemplate> riftGroups = new HashMap<String, SpawnTemplate>();

	/**
	 * 登记裂隙生成组模板（按锚点名索引）。
	 * Registers rift spawn-group templates (indexed by anchor name).
	 *
	 * Spawn group
	 */
	public static void addRiftSpawnTemplate(SpawnGroup2 spawn) {
		if (spawn.hasPool()) {
			SpawnTemplate template = spawn.getSpawnTemplates().get(0);
			riftGroups.put(template.getAnchor(), template);
		}
		else {
			for (SpawnTemplate template : spawn.getSpawnTemplates()) {
				riftGroups.put(template.getAnchor(), template);
			}
		}
	}

	/**
	 * 在裂隙位置生成主从端裂隙 NPC。
	 * Spawns master/slave rift NPCs at a rift location.
	 *
	 * @param loc 裂隙位置 / Rift location
	 */
	public void spawnRift(RiftLocation loc) {
		RiftEnum rift = RiftEnum.getRift(loc.getId());
		spawnRift(rift, null, loc);
	}

	/**
	 * 在漩涡位置生成次元漩涡主从端 NPC。
	 * Spawns master/slave vortex NPCs at a vortex location.
	 *
	 * @param loc 漩涡位置 / Vortex location
	 */
	public void spawnVortex(VortexLocation loc) {
		RiftEnum rift = RiftEnum.getVortex(loc.getDefendersRace());
		spawnRift(rift, loc, null);
	}

	private void spawnRift(RiftEnum rift, VortexLocation vl, RiftLocation rl) {
		SpawnTemplate masterTemplate = riftGroups.get(rift.getMaster());
		SpawnTemplate slaveTemplate = riftGroups.get(rift.getSlave());

		if (masterTemplate == null || slaveTemplate == null) {
			return;
		}

		int spawned = 0;
		int instanceCount = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(masterTemplate.getWorldId()).getInstanceCount();

		if (slaveTemplate.hasPool()) {
			slaveTemplate = slaveTemplate.changeTemplate(1);

		}

		for (int i = 1; i <= instanceCount; i++) {
			Npc slave = spawnInstance(i, slaveTemplate, new RVController(null, rift));
			Npc master = spawnInstance(i, masterTemplate, new RVController(slave, rift));

			if (rift.isVortex()) {
				vl.setVortexController((RVController) master.getController());
				spawned = vl.getSpawned().size();
				vl.getSpawned().add(master);
				vl.getSpawned().add(slave);
			}
            else {
               rl.getSpawned().add(master);
               rl.getSpawned().add(slave);
           }
           spawned += 2;
       }
       log.info(I18n.get("log.918c2ef9c33c", rift.name(), spawned));
	}

	private Npc spawnInstance(int instance, SpawnTemplate template, RVController controller) {
		NpcTemplate masterObjectTemplate = DataManager.NPC_DATA.getNpcTemplate(template.getNpcId());
		Npc npc = new Npc(GameWorldBootstrapServices.idFactory().nextId(), controller, template, masterObjectTemplate);

		npc.setKnownlist(new NpcKnownList(npc));
		npc.setEffectController(new EffectController(npc));

		World world = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		world.storeObject(npc);
		world.setPosition(npc, template.getWorldId(), instance, template.getX(), template.getY(), template.getZ(), template.getHeading());
		world.spawn(npc);
		rifts.add(npc);

		return npc;
	}

	/**
	 * 返回当前已生成裂隙 NPC 的快照副本。
	 * Returns a snapshot copy of currently spawned rift NPCs.
	 *
	 * Rift NPC list
	 */
	public static List<Npc> getSpawned() {
        synchronized (rifts) {
        return new ArrayList<>(rifts);
    }
    }

	/**
	 * 获取 {@link RiftManager} 单例（优先 Spring 提供的实例）。
	 * Returns the {@link RiftManager} singleton (prefers Spring-provided instance).
	 *
	 * @return 管理器实例 / Manager instance
	 */
	public static RiftManager getInstance() {
		ObjectProvider<RiftManager> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> RiftManagerHolder.INSTANCE);
		}
		return RiftManagerHolder.INSTANCE;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<RiftManager> provider) {
		instanceProvider = provider;
	}

	private static class RiftManagerHolder {
		private static final RiftManager INSTANCE = new RiftManager();
	}
}
