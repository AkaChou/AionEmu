package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.NpcParty;
import com.aionemu.gameserver.dataholders.RetailAiData.NpcPartyMember;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.world.WorldMapInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class RetailNpcPartyEngine {

	private RetailNpcPartyEngine() {
	}

	public static void initialize(WorldMapInstance instance) {
		if (DataManager.RETAIL_AI_DATA == null) {
			return;
		}
		List<SpawnTemplate> templates = createSpawnTemplates(instance.getMapId(),
			DataManager.RETAIL_AI_DATA.getNpcParties(instance.getMapId()));
		templates.forEach(template -> SpawnEngine.spawnObject(template, instance.getInstanceId()));
		if (!templates.isEmpty()) {
			log.info("Spawned {} retail NPC party members in world {} instance {}", templates.size(), instance.getMapId(),
				instance.getInstanceId());
		}
	}

	static List<SpawnTemplate> createSpawnTemplates(int worldId, List<NpcParty> parties) {
		List<SpawnTemplate> templates = new ArrayList<>();
		for (NpcParty party : parties) {
			String partyId = worldId + ":" + party.token();
			for (NpcPartyMember member : party.members()) {
				SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(worldId, member.id(), member.x(), member.y(),
					member.z(), (byte) 0);
				template.setNpcPartyId(partyId);
				templates.add(template);
			}
		}
		return templates;
	}
}
