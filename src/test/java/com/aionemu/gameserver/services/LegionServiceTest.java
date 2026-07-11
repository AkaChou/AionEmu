package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionRank;
import com.aionemu.gameserver.world.WorldPosition;

class LegionServiceTest {

	@Test
	void legionCreationRequiresNearbyCreatorNpc() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		Npc creator = new ObjenesisStd().newInstance(Npc.class);
		creator.setObjectTemplate(new NpcTemplate());
		player.setPosition(position(0, 0));
		creator.setPosition(position(3, 0));

		assertTrue(LegionService.isNearLegionCreator(player, creator));

		creator.setPosition(position(5, 0));
		assertFalse(LegionService.isNearLegionCreator(player, creator));
		assertFalse(LegionService.isNearLegionCreator(player, null));
	}

	@Test
	void disbandIsBlockedWhileLegionWarehouseIsInUse() throws Exception {
		LegionService service = new ObjenesisStd().newInstance(LegionService.class);
		Legion legion = new Legion(1, "legion");
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setLegionMember(new LegionMember(1, legion, LegionRank.BRIGADE_GENERAL));

		assertTrue(canDisband(service, player, legion));

		legion.getLegionWarehouse().setWhUser(2);

		assertFalse(canDisband(service, player, legion));
	}

	@Test
	void newEmblemUploadReplacesPreviousUploadContext() throws Exception {
		LegionService service = new ObjenesisStd().newInstance(LegionService.class);
		initializeRestrictions(service);
		Legion legion = new Legion(1, "legion");
		legion.setLegionLevel(3);
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setLegionMember(new LegionMember(1, legion, LegionRank.BRIGADE_GENERAL));
		LegionEmblem emblem = legion.getLegionEmblem();
		emblem.setUploading(true);
		emblem.setUploadSize(10);
		emblem.addUploadedSize(5);

		service.uploadEmblemInfo(player, 20, 1, 2, 3, LegionEmblemType.CUSTOM);

		assertTrue(emblem.isUploading());
		assertEquals(20, emblem.getUploadSize());
		assertEquals(0, emblem.getUploadedSize());
	}

	private boolean canDisband(LegionService service, Player player, Legion legion) throws Exception {
		Object restrictions = newRestrictions(service);
		Method method = restrictions.getClass().getDeclaredMethod("canDisbandLegion", Player.class, Legion.class);
		method.setAccessible(true);
		return (boolean) method.invoke(restrictions, player, legion);
	}

	private void initializeRestrictions(LegionService service) throws Exception {
		Field field = LegionService.class.getDeclaredField("legionRestrictions");
		field.setAccessible(true);
		field.set(service, newRestrictions(service));
	}

	private Object newRestrictions(LegionService service) throws Exception {
		Class<?> restrictionsClass = Class.forName(LegionService.class.getName() + "$LegionRestrictions");
		Constructor<?> constructor = restrictionsClass.getDeclaredConstructor(LegionService.class);
		constructor.setAccessible(true);
		return constructor.newInstance(service);
	}

	private static WorldPosition position(float x, float y) {
		WorldPosition position = new WorldPosition(1) {
			@Override
			public int getInstanceId() {
				return 1;
			}
		};
		position.setXYZH(x, y, 0f, (byte) 0);
		return position;
	}
}
