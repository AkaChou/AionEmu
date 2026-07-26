package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.DataDrivenQuest;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataDrivenQuestData", propOrder = "steps")
public class DataDrivenQuestData extends XMLQuest {

	@XmlAttribute(name = "start_type", required = true)
	private String startType;
	@XmlAttribute(name = "start_ids")
	private List<Integer> startIds;
	@XmlAttribute(name = "start_dialog_id")
	private int startDialogId;
	@XmlAttribute(name = "end_npc_ids")
	private List<Integer> endNpcIds;
	@XmlAttribute(name = "world_id")
	private int worldId;
	@XmlAttribute(name = "reset_world_id")
	private int resetWorldId;
	@XmlAttribute(name = "start_item_id")
	private int startItemId;
	@XmlAttribute(name = "start_give_item_id")
	private int startGiveItemId;
	@XmlAttribute(name = "start_give_item_count")
	private int startGiveItemCount;
	@XmlAttribute(name = "start_remove_item_id")
	private int startRemoveItemId;
	@XmlAttribute(name = "start_remove_item_count")
	private int startRemoveItemCount;
	@XmlAttribute(name = "complete_on_start")
	private boolean completeOnStart;
	@XmlElement(name = "step")
	private List<Step> steps;

	public String getStartType() {
		return startType;
	}

	public List<Integer> getStartIds() {
		return startIds == null ? List.of() : startIds;
	}

	public int getStartDialogId() {
		return startDialogId;
	}

	public List<Integer> getEndNpcIds() {
		return endNpcIds == null ? List.of() : endNpcIds;
	}

	public int getWorldId() {
		return worldId;
	}

	public int getResetWorldId() {
		return resetWorldId;
	}

	public int getStartItemId() {
		return startItemId;
	}

	public int getStartGiveItemId() {
		return startGiveItemId;
	}

	public int getStartGiveItemCount() {
		return startGiveItemCount;
	}

	public int getStartRemoveItemId() {
		return startRemoveItemId;
	}

	public int getStartRemoveItemCount() {
		return startRemoveItemCount;
	}

	public boolean isCompleteOnStart() {
		return completeOnStart;
	}

	public List<Step> getSteps() {
		if (steps == null) {
			steps = new ArrayList<>();
		}
		return steps;
	}

	@Override
	public void register(QuestEngine questEngine) {
		questEngine.addQuestHandler(new DataDrivenQuest(this));
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "DataDrivenQuestStep", propOrder = "spawns")
	public static class Step {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;
		@XmlAttribute(name = "type", required = true)
		private String type;
		@XmlAttribute(name = "ids")
		private List<Integer> ids;
		@XmlAttribute(name = "action_ids")
		private List<Integer> actionIds;
		@XmlAttribute(name = "delete_action_target")
		private boolean deleteActionTarget;
		@XmlAttribute(name = "amount")
		private int amount;
		@XmlAttribute(name = "dialog_id")
		private int dialogId;
		@XmlAttribute(name = "advance_dialog_id")
		private int advanceDialogId;
		@XmlAttribute(name = "movie")
		private int movie;
		@XmlAttribute(name = "teleport_world_id")
		private int teleportWorldId;
		@XmlAttribute(name = "teleport_x")
		private int teleportX;
		@XmlAttribute(name = "teleport_y")
		private int teleportY;
		@XmlAttribute(name = "teleport_z")
		private int teleportZ;
		@XmlAttribute(name = "teleport_heading")
		private int teleportHeading;
		@XmlAttribute(name = "world_id")
		private int worldId;
		@XmlAttribute(name = "item_id")
		private int itemId;
		@XmlAttribute(name = "give_item_id")
		private int giveItemId;
		@XmlAttribute(name = "give_item_count")
		private int giveItemCount;
		@XmlAttribute(name = "remove_item_id")
		private int removeItemId;
		@XmlAttribute(name = "remove_item_count")
		private int removeItemCount;
		@XmlAttribute(name = "timer_seconds")
		private int timerSeconds;
		@XmlAttribute(name = "timer_destination_progress")
		private int timerDestinationProgress;

		public String getType() {
			return type;
		}

		public List<Spawn> getSpawns() {
			return spawns == null ? List.of() : spawns;
		}

		public List<Integer> getIds() {
			return ids == null ? List.of() : ids;
		}

		public List<Integer> getActionIds() {
			return actionIds == null ? List.of() : actionIds;
		}

		public boolean isDeleteActionTarget() {
			return deleteActionTarget;
		}

		public int getAmount() {
			return amount;
		}

		public int getDialogId() {
			return dialogId;
		}

		public int getAdvanceDialogId() {
			return advanceDialogId;
		}

		public int getMovie() {
			return movie;
		}

		public int getTeleportWorldId() {
			return teleportWorldId;
		}

		public int getTeleportX() {
			return teleportX;
		}

		public int getTeleportY() {
			return teleportY;
		}

		public int getTeleportZ() {
			return teleportZ;
		}

		public int getTeleportHeading() {
			return teleportHeading;
		}

		public int getWorldId() {
			return worldId;
		}

		public int getItemId() {
			return itemId;
		}

		public int getGiveItemId() {
			return giveItemId;
		}

		public int getGiveItemCount() {
			return giveItemCount;
		}

		public int getRemoveItemId() {
			return removeItemId;
		}

		public int getRemoveItemCount() {
			return removeItemCount;
		}

		public int getTimerSeconds() {
			return timerSeconds;
		}

		public int getTimerDestinationProgress() {
			return timerDestinationProgress;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "DataDrivenQuestSpawn")
	public static class Spawn {

		@XmlAttribute(name = "npc_id", required = true)
		private int npcId;
		@XmlAttribute(name = "count", required = true)
		private int count;
		@XmlAttribute(name = "lifetime_seconds", required = true)
		private int lifetimeSeconds;
		@XmlAttribute(name = "relative")
		private boolean relative;
		@XmlAttribute(name = "x")
		private float x;
		@XmlAttribute(name = "y")
		private float y;
		@XmlAttribute(name = "z")
		private float z;
		@XmlAttribute(name = "heading")
		private int heading;

		public int getNpcId() {
			return npcId;
		}

		public int getCount() {
			return count;
		}

		public int getLifetimeSeconds() {
			return lifetimeSeconds;
		}

		public boolean isRelative() {
			return relative;
		}

		public float getX() {
			return x;
		}

		public float getY() {
			return y;
		}

		public float getZ() {
			return z;
		}

		public int getHeading() {
			return heading;
		}
	}
}
