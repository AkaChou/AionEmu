package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 验证影片播放凭据的权威、消费和会话清理合同。 / Verifies movie-playback authority, consumption, and session cleanup contracts. */
class MoviePlaybackAuthorityTest {

	/** 验证同一服务端播放仅接受一次匹配结束确认。 / Verifies one matching completion is accepted per server playback. */
	@Test
	void acceptsOneMatchingServerIssuedCompletion() {
		MoviePlaybackAuthority authority = new MoviePlaybackAuthority();
		MoviePlaybackAuthority.Playback playback = authority.begin(913, 1000);

		assertEquals(playback, authority.complete(913, 1001).orElseThrow());
		assertTrue(authority.active().isEmpty());
		assertTrue(authority.complete(913, 1002).isEmpty());
	}

	/** 验证伪造、错 ID、过早和已被替代的确认均被拒绝。 / Verifies forged, wrong-id, early, and superseded completions are rejected. */
	@Test
	void rejectsForgedWrongEarlyAndSupersededCompletions() {
		MoviePlaybackAuthority authority = new MoviePlaybackAuthority();
		MoviePlaybackAuthority.Playback first = authority.begin(913, 2000);

		assertTrue(authority.complete(914, 2001).isEmpty());
		assertTrue(authority.complete(913, 1999).isEmpty());
		MoviePlaybackAuthority.Playback second = authority.begin(914, 2002);
		assertEquals(first.playbackId() + 1, second.playbackId());
		assertTrue(authority.complete(913, 2003).isEmpty());
		assertEquals(second, authority.complete(914, 2003).orElseThrow());
	}

	/** 验证会话清理和协议 ID 边界。 / Verifies session cleanup and protocol id bounds. */
	@Test
	void clearInvalidatesSessionAuthorityAndProtocolBoundsAreEnforced() {
		MoviePlaybackAuthority authority = new MoviePlaybackAuthority();
		authority.begin(0xFFFF, 3000);
		authority.clear();

		assertTrue(authority.complete(0xFFFF, 3001).isEmpty());
		assertThrows(IllegalArgumentException.class, () -> authority.begin(0, 3002));
		assertThrows(IllegalArgumentException.class, () -> authority.begin(0x10000, 3002));
		assertThrows(IllegalArgumentException.class, () -> authority.begin(1, 0));
	}
}
