package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.services.instance.InstanceDeadlineScheduler;
import com.aionemu.gameserver.world.WorldMapInstance;

@AIName("IDTiamat_Tiamat_Timer_01")
public class IDTiamatTiamatTimer01AI2 extends NpcAI2 {

	private static final String DEADLINE_KEY = "retail.script.idtiamat_tiamat_timer";
	private static final long DURATION = 1_800_000;

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		WorldMapInstance instance = getPosition().getWorldMapInstance();
		long deadline = InstanceDeadlineScheduler.deadline(instance, DEADLINE_KEY);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + DURATION;
		}
		InstanceDeadlineScheduler.schedule(instance, DEADLINE_KEY, deadline, this::expire);
	}

	@Override
	public void onRetailMessage(int type, int param1, int param2, Creature sender, Creature parameter) {
		if (type == 201) {
			InstanceDeadlineScheduler.cancel(getPosition().getWorldMapInstance(), DEADLINE_KEY);
			if (getOwner().isSpawned()) {
				AI2Actions.deleteOwner(this);
			}
		}
	}

	private void expire() {
		WorldMapInstance instance = getPosition().getWorldMapInstance();
		for (int npcId : new int[] { 219362, 236277 }) {
			for (Npc tiamat : instance.getNpcs(npcId)) {
				if (tiamat.isSpawned()) {
					tiamat.getAi2().onRetailMessage(10010, 0, 0, getOwner(), getOwner());
				}
			}
		}
		AI2Actions.deleteOwner(this);
	}
}
