package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeType;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class SM_ABYSS_ARTIFACT_INFO2Test {

    @Test
    void artifactInfo2CountsArtifactAndFortressLocationsBelowTwoThousand() throws Exception {
        SM_ABYSS_ARTIFACT_INFO2 packet = packetWithLocations(List.of(
            location(1011, SiegeType.ARTIFACT),
            location(1999, SiegeType.FORTRESS),
            location(2000, SiegeType.ARTIFACT),
            location(10412, SiegeType.ARTIFACT),
            location(1500, SiegeType.TOWER)
        ));

        ByteBuffer buffer = write(packet);

        assertEquals(2, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(1011, buffer.getInt());
        assertEquals(0, Byte.toUnsignedInt(buffer.get()));
        assertEquals(1999, buffer.getInt());
        assertEquals(0, Byte.toUnsignedInt(buffer.get()));
    }

    private static ByteBuffer write(SM_ABYSS_ARTIFACT_INFO2 packet) {
        ByteBuffer buffer = ByteBuffer.allocate(128);
        packet.setBuf(buffer);
        packet.writeImpl(null);
        buffer.flip();
        return buffer;
    }

    private static SM_ABYSS_ARTIFACT_INFO2 packetWithLocations(List<SiegeLocation> locations) throws Exception {
        SM_ABYSS_ARTIFACT_INFO2 packet = new ObjenesisStd().newInstance(SM_ABYSS_ARTIFACT_INFO2.class);
        setField(packet, "locations", locations);
        return packet;
    }

    private static SiegeLocation location(int locationId, SiegeType type) throws Exception {
        SiegeLocation location = new ObjenesisStd().newInstance(SiegeLocation.class);
        setField(location, "locationId", locationId);
        setField(location, "type", type);
        return location;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
