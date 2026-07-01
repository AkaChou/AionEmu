package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IPRangeTest {

	@Test
	void rejectsNonIpv4ByteArrays() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
			() -> new IPRange(new byte[0], new byte[] { 127, 0, 0, 1 }, new byte[] { 127, 0, 0, 1 }));

		assertTrue(error.getMessage().contains("min"));
		assertTrue(error.getMessage().contains("4 bytes"));
	}

	@Test
	void keepsIpv4AddressBytes() {
		byte[] address = new byte[] { 127, 0, 0, 1 };

		IPRange range = new IPRange(new byte[] { 127, 0, 0, 0 }, new byte[] { 127, 0, 0, 1 }, address);

		assertArrayEquals(address, range.getAddress());
	}
}
