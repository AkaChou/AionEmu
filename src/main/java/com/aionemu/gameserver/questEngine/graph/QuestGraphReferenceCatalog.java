package com.aionemu.gameserver.questEngine.graph;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.aionemu.gameserver.dataholders.FlyRingData;
import com.aionemu.gameserver.dataholders.FlyPathData;
import com.aionemu.gameserver.dataholders.HousingObjectData;
import com.aionemu.gameserver.dataholders.ItemData;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.dataholders.RecipeData;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.SpawnsData2;
import com.aionemu.gameserver.dataholders.TitleData;
import com.aionemu.gameserver.dataholders.WindstreamData;
import com.aionemu.gameserver.dataholders.WalkerData;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.dataholders.ZoneData;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler.References;

/** 从已加载的正式静态数据构造任务图 compiler 引用闭包。 / Builds quest-graph compiler reference closure from loaded formal static data. */
public final class QuestGraphReferenceCatalog {

	private QuestGraphReferenceCatalog() {
	}

	/** 构造一次编译 generation 使用的不可变引用快照。 / Builds an immutable reference snapshot for one compiler generation. */
	public static References build(QuestsData quests, NpcData npcs, ItemData items, HousingObjectData housingObjects,
			TitleData titles, ZoneData zones, WorldMapsData worlds, SkillData skills, RecipeData recipes,
			WindstreamData windstreams, FlyRingData flyingRings, FlyPathData flyPaths, WalkerData walkers, SpawnsData2 spawns) {
		Objects.requireNonNull(quests, "quests");
		Objects.requireNonNull(npcs, "npcs");
		Objects.requireNonNull(items, "items");
		Objects.requireNonNull(housingObjects, "housingObjects");
		Objects.requireNonNull(titles, "titles");
		Objects.requireNonNull(zones, "zones");
		Objects.requireNonNull(worlds, "worlds");
		Objects.requireNonNull(flyPaths, "flyPaths");
		Objects.requireNonNull(walkers, "walkers");
		Objects.requireNonNull(spawns, "spawns");
		Set<Integer> questIds = quests.getQuestsData().stream().map(quest -> quest.getId()).collect(Collectors.toCollection(LinkedHashSet::new));
		Set<Integer> npcIds = integerKeys(npcs.getNpcData().keys());
		Set<Integer> itemIds = integerKeys(items.getItemData().keys());
		Set<Integer> housingObjectIds = integerKeys(housingObjects.getTemplateIds());
		Set<Integer> titleIds = integerKeys(titles.getTitleData().keys());
		Set<String> zoneNames = zones.getZones().values().stream().flatMap(java.util.Collection::stream)
			.map(ZoneInfo::getZoneTemplate).map(zone -> zone.getName().name()).collect(Collectors.toCollection(LinkedHashSet::new));
		Set<Integer> worldIds = new LinkedHashSet<>();
		Set<Integer> instanceWorldIds = new LinkedHashSet<>();
		worlds.forEach(world -> {
			worldIds.add(world.getMapId());
			if (world.isInstance()) {
				instanceWorldIds.add(world.getMapId());
			}
		});
		QuestGraphMovementReferenceCatalog.MovementReferences movement = QuestGraphMovementReferenceCatalog.build(windstreams, flyingRings);
		QuestGraphCraftSkillReferenceCatalog craft = QuestGraphCraftSkillReferenceCatalog.build(recipes);
		Set<Integer> movieIds = QuestGraphMovieReferenceCatalog.build();
		Set<String> walkerIds = walkers.getTemplates().stream().map(template -> template.getRouteId())
			.collect(Collectors.toCollection(LinkedHashSet::new));
		Set<Integer> staticSpawnNpcIds = collectStaticSpawnReferences(spawns, worldIds).stream()
			.map(StaticSpawnReference::npcId).collect(Collectors.toUnmodifiableSet());
		return new References(questIds, npcIds, itemIds, housingObjectIds, titleIds, zoneNames, movieIds,
			movement.windstreamRoutes(), movement.flyingRings(), QuestGraphSkillReferenceCatalog.build(skills), worldIds,
			instanceWorldIds, QuestGraphRecipeReferenceCatalog.build(recipes), craft.craftSkillIds(), walkerIds,
			staticSpawnNpcIds, QuestGraphFlightReferenceCatalog.build(flyPaths));
	}

	/** 表示一张正式世界地图中的静态 NPC 刷新点引用。 / Represents a static NPC spawn-point reference in one formal world. */
	public record StaticSpawnReference(int worldId, int npcId) {
		/** 校验世界与 NPC 模板 ID。 / Validates the world and NPC template identifiers. */
		public StaticSpawnReference {
			if (worldId <= 0 || npcId <= 0) {
				throw new IllegalArgumentException("Static spawn reference is invalid");
			}
		}
	}

	/** 收集带世界维度且确有坐标 spot 的普通静态 NPC 刷新引用。 / Collects world-qualified regular static NPC spawn references with an actual coordinate spot. */
	static Set<StaticSpawnReference> collectStaticSpawnReferences(SpawnsData2 spawns, Set<Integer> worldIds) {
		Objects.requireNonNull(spawns, "spawns");
		Objects.requireNonNull(worldIds, "worldIds");
		return worldIds.stream().flatMap(worldId -> spawns.getSpawnsByWorldId(worldId).stream())
			.filter(group -> !group.getSpawnTemplates().isEmpty())
			.map(group -> new StaticSpawnReference(group.getWorldId(), group.getNpcId()))
			.collect(Collectors.toUnmodifiableSet());
	}

	/** 收集确有坐标 spot 的普通静态 NPC 刷新模板。 / Collects regular static NPC spawn templates that have an actual coordinate spot. */
	static Set<Integer> collectStaticSpawnNpcIds(SpawnsData2 spawns, Set<Integer> worldIds) {
		return collectStaticSpawnReferences(spawns, worldIds).stream().map(StaticSpawnReference::npcId)
			.collect(Collectors.toUnmodifiableSet());
	}

	private static Set<Integer> integerKeys(int[] keys) {
		return Arrays.stream(keys).boxed().collect(Collectors.toUnmodifiableSet());
	}
}
