package com.aionemu.gameserver.taskmanager.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.lifecycle.GameEventServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster.BroadcastMode;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;

class PacketBroadcasterTest {

	@Test
	void updateQueuedDuringSendRemainsPending() {
		TestCreature creature = new TestCreature();
		AtomicBoolean queueAgain = new AtomicBoolean(true);
		creature.setEffectController(new EffectController(creature) {
			@Override
			public void broadCastEffectsImp() {
				if (queueAgain.getAndSet(false)) {
					creature.addPacketBroadcastMask(BroadcastMode.BROAD_CAST_EFFECTS);
				}
			}
		});
		creature.addPacketBroadcastMask(BroadcastMode.BROAD_CAST_EFFECTS);

		BroadcastMode.BROAD_CAST_EFFECTS.trySendPacket(creature, creature.getPacketBroadcastMask());

		assertEquals(BroadcastMode.BROAD_CAST_EFFECTS.mask(), creature.getPacketBroadcastMask());
		GameEventServices.packetBroadcaster().run();
		assertEquals(0, creature.getPacketBroadcastMask());
	}

	@Test
	void failedSendRemainsPending() {
		TestCreature creature = new TestCreature();
		AtomicBoolean fail = new AtomicBoolean(true);
		creature.setEffectController(new EffectController(creature) {
			@Override
			public void broadCastEffectsImp() {
				if (fail.getAndSet(false)) {
					throw new IllegalStateException("test");
				}
			}
		});
		creature.addPacketBroadcastMask(BroadcastMode.BROAD_CAST_EFFECTS);

		assertThrows(IllegalStateException.class,
			() -> BroadcastMode.BROAD_CAST_EFFECTS.trySendPacket(creature, creature.getPacketBroadcastMask()));

		assertEquals(BroadcastMode.BROAD_CAST_EFFECTS.mask(), creature.getPacketBroadcastMask());
		GameEventServices.packetBroadcaster().run();
		assertEquals(0, creature.getPacketBroadcastMask());
	}

	private static final class TestCreature extends Creature {

		private TestCreature() {
			super(1, new CreatureController<>() {}, null, new TestVisibleObjectTemplate(), new WorldPosition(1));
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}

	private static final class TestVisibleObjectTemplate extends VisibleObjectTemplate {

		@Override
		public int getTemplateId() {
			return 1;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public int getNameId() {
			return 1;
		}
	}
}
