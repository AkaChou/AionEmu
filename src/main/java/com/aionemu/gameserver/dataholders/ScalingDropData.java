package com.aionemu.gameserver.dataholders;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@XmlRootElement(name = "scaling_drops")
@XmlAccessorType(XmlAccessType.FIELD)
public class ScalingDropData {

	@XmlElement(name = "npc")
	private List<NpcScalingDrop> npcDrops = new ArrayList<>();
	@XmlTransient
	private Map<Integer, NpcScalingDrop> byNpcId = Collections.emptyMap();

	public ScalingDropData() {
	}

	public NpcScalingDrop getDrop(int npcId) {
		return byNpcId.get(npcId);
	}

	public int size() {
		return npcDrops == null ? 0 : npcDrops.size();
	}

	@SuppressWarnings("unused")
	private void afterUnmarshal(jakarta.xml.bind.Unmarshaller unmarshaller, Object parent) {
		byNpcId = index();
	}

	private Map<Integer, NpcScalingDrop> index() {
		return npcDrops == null ? Collections.emptyMap() : npcDrops.stream()
			.collect(Collectors.toUnmodifiableMap(NpcScalingDrop::getNpcId, drop -> drop));
	}

	public void rebuildIndex() {
		byNpcId = index();
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class NpcScalingDrop {
		@XmlAttribute(name = "id", required = true)
		private int npcId;
		@XmlAttribute
		private String source;
		@XmlElement(name = "set")
		private List<ScalingDropSet> sets = new ArrayList<>();

		public int getNpcId() {
			return npcId;
		}

		public List<ScalingDropSet> getSets() {
			return sets == null ? Collections.emptyList() : sets;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ScalingDropSet {
		@XmlAttribute(required = true)
		private int rate;
		@XmlAttribute(name = "min_level", required = true)
		private int minLevel;
		@XmlAttribute(name = "max_level", required = true)
		private int maxLevel;
		@XmlAttribute
		private Race race = Race.PC_ALL;
		@XmlAttribute(name = "class_filter")
		private String classFilter;
		@XmlElement(name = "item", required = true)
		private List<ScalingDropItem> items = new ArrayList<>();

		public int getRate() {
			return rate;
		}

		public List<ScalingDropItem> getItems() {
			return items == null ? Collections.emptyList() : items;
		}

		public boolean matches(Player player) {
			if (player.getLevel() < minLevel || player.getLevel() > maxLevel) {
				return false;
			}
			if (race != null && race != Race.PC_ALL && race != player.getRace()) {
				return false;
			}
			return classFilter == null || classFilter.isBlank() || classMatches(player.getPlayerClass());
		}

		private boolean classMatches(PlayerClass playerClass) {
			String filter = classFilter.toUpperCase(Locale.ROOT).replace(' ', '_');
			if (filter.contains(playerClass.name())) {
				return true;
			}
			try {
				return filter.contains(PlayerClass.getStartingClassFor(playerClass).name());
			} catch (IllegalArgumentException ignored) {
				return false;
			}
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ScalingDropItem {
		@XmlAttribute(required = true)
		private int id;
		@XmlAttribute(required = true)
		private long count;
		@XmlAttribute(required = true)
		private int weight;

		public int getId() {
			return id;
		}

		public long getCount() {
			return count;
		}

		public int getWeight() {
			return weight;
		}
	}
}
