package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeType;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class SM_ABYSS_ARTIFACT_INFOTest {

    @Test
    void artifactInfoCountsArtifactAndFortressLocationsThroughUpperRange() throws Exception {
        SM_ABYSS_ARTIFACT_INFO packet = new SM_ABYSS_ARTIFACT_INFO(List.of(
            location(1011, SiegeType.ARTIFACT),
            location(2000, SiegeType.FORTRESS),
            location(10412, SiegeType.ARTIFACT),
            location(10413, SiegeType.ARTIFACT),
            location(1500, SiegeType.TOWER)
        ));

        ByteBuffer buffer = write(packet);

        assertEquals(3, Short.toUnsignedInt(buffer.getShort()));
    }

    private static ByteBuffer write(SM_ABYSS_ARTIFACT_INFO packet) {
        ByteBuffer buffer = ByteBuffer.allocate(128);
        packet.setBuf(buffer);
        packet.writeImpl(null);
        buffer.flip();
        return buffer;
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
