package com.aionemu.gameserver.world.container;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;

class LegionMemberContainerTest {

	@Test
	void addMemberExRejectsDuplicateWithoutChangingExistingMember() {
		LegionMemberContainer members = new LegionMemberContainer();
		LegionMemberEx existing = member(1, "existing");
		members.addMemberEx(existing);

		assertThrows(DuplicateAionObjectException.class, () -> members.addMemberEx(member(1, "other")));
		assertThrows(DuplicateAionObjectException.class, () -> members.addMemberEx(member(2, "existing")));

		assertSame(existing, members.getMemberEx(1));
		assertSame(existing, members.getMemberEx("existing"));
	}

	@Test
	void publicAccessorsUseSingleObjectMonitor() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/container/LegionMemberContainer.java"));

		assertTrue(source.contains("public synchronized void addMember("));
		assertTrue(source.contains("public synchronized LegionMember getMember("));
		assertTrue(source.contains("public synchronized void addMemberEx("));
		assertTrue(source.contains("public synchronized LegionMemberEx getMemberEx("));
		assertTrue(source.contains("public synchronized void remove("));
		assertTrue(source.contains("public synchronized boolean contains("));
		assertTrue(source.contains("public synchronized boolean containsEx("));
		assertTrue(source.contains("public synchronized void clear()"));
	}

	private LegionMemberEx member(int objectId, String name) {
		LegionMemberEx member = new LegionMemberEx(objectId);
		member.setName(name);
		return member;
	}
}
