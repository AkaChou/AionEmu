package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(300240000)
public class AturamSkyFortressInstance extends GeneralInstanceHandler {

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		spawnRing("ATURAM_SKY_FORTRESS_1", new Point3D(435.79208, 421.96408, 625.9659),
			new Point3D(435.13388, 424.68580, 658.3014), new Point3D(435.42896, 427.36942, 652.9659), 61);
		spawnRing("ATURAM_SKY_FORTRESS_2", new Point3D(819.41113, 212.29883, 605.6249),
			new Point3D(815.09174, 211.83253, 615.6375), new Point3D(810.67690, 212.15894, 605.6249), 90);
		spawnRing("ATURAM_SKY_FORTRESS_3", new Point3D(167.87286, 654.69824, 901.0089),
			new Point3D(172.81534, 652.12440, 913.8702), new Point3D(176.82208, 649.07650, 901.0089), 20);
	}

	@Override
	public boolean onPassFlyingRing(Player player, String flyingRing) {
		if (flyingRing.equals("ATURAM_SKY_FORTRESS_1") || flyingRing.equals("ATURAM_SKY_FORTRESS_2")) {
			removeEffects(player);
		} else if (flyingRing.equals("ATURAM_SKY_FORTRESS_3")) {
			setDoorState(177, true);
		}
		return false;
	}

	@Override
	public void onPlayerLogOut(Player player) {
		cleanup(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		cleanup(player);
	}

	private void spawnRing(String name, Point3D center, Point3D min, Point3D max, int radius) {
		new FlyRing(new FlyRingTemplate(name, mapId, center, min, max, radius), instanceId).spawn();
	}

	private void cleanup(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(164000163, storage.getItemCountByItemId(164000163));
		storage.decreaseByItemId(164000202, storage.getItemCountByItemId(164000202));
		removeEffects(player);
	}

	private void removeEffects(Player player) {
		PlayerEffectController effects = player.getEffectController();
		for (int skillId : new int[] { 19407, 19408, 19520, 21807, 21808 }) {
			effects.removeEffect(skillId);
		}
	}
}
