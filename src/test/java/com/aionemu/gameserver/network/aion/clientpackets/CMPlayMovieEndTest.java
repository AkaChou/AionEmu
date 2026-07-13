package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.ai2.AITemplate;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.knownlist.KnownList;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CMPlayMovieEndTest {

	@Test
	void notifiesOnlyTargetNpcFromPlayerKnownList() {
		ObjenesisStd objenesis = new ObjenesisStd();
		Player player = objenesis.newInstance(Player.class);
		Npc npc = objenesis.newInstance(Npc.class);
		RecordingAI ai = new RecordingAI();
		player.setKnownlist(new KnownList(player));
		player.getKnownList().getKnownObjects().put(42, npc);
		npc.setAi2(ai);

		CM_PLAY_MOVIE_END.notifyRetailAi(player, 41, 913);
		CM_PLAY_MOVIE_END.notifyRetailAi(player, 42, 914);

		assertEquals(1, ai.calls);
		assertSame(player, ai.player);
		assertEquals(914, ai.cutsceneId);
	}

	private static final class RecordingAI extends AITemplate {

		private int calls;
		private Player player;
		private int cutsceneId;

		@Override
		public void onQuitCutscene(Player player, int cutsceneId) {
			calls++;
			this.player = player;
			this.cutsceneId = cutsceneId;
		}
	}
}
