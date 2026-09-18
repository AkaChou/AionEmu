package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.testutil.ConfigSnapshot;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.network.aion.AionConnection;

/**
 * 固化 {@link PlayerTags} 的账号、管理员与聊天命令标签语义。
 * Pins down {@link PlayerTags} account, admin and chat-command tag semantics.
 */
class PlayerTagsTest {

	private final ConfigSnapshot adminTagSnapshot = ConfigSnapshot.of(AdminConfig.class, "ADMIN_TAG_3");
	private final ConfigSnapshot membershipTagSnapshot =
		ConfigSnapshot.of(MembershipConfig.class, "PLAYER_TAG_30");

	@BeforeEach
	void setUp() {
		AdminConfig.ADMIN_TAG_3 = "[ADMIN]%s";
		MembershipConfig.PLAYER_TAG_30 = "[AION]%s";
	}

	@AfterEach
	void tearDown() {
		adminTagSnapshot.restore();
		membershipTagSnapshot.restore();
	}

	@Test
	void defaultsToPlaceholderWhenNoTagApplies() {
		Player player = player("unknown", (byte) 0);

		assertEquals("%s", player.getCustomTag(false));
		assertEquals("", player.getCustomTag(true));
	}

	@Test
	void usesAdminTagForAccessLevel() {
		Player player = player("unknown", (byte) 3);

		assertEquals("[ADMIN]%s", player.getCustomTag(false));
		assertEquals("[ADMIN]", player.getCustomTag(true));
	}

	@Test
	void usesMembershipTagForSpecialAccountName() {
		Player player = player("Aion", (byte) 0);

		assertEquals("[AION]%s", player.getCustomTag(false));
		assertEquals("[AION]", player.getCustomTag(true));
	}

	private static Player player(String accountName, byte accessLevel) {
		Account account = new Account(1);
		account.setName(accountName);
		account.setAccessLevel(accessLevel);
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		connection.setAccount(account);
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setClientConnection(connection);
		return player;
	}
}
