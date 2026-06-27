package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.model.siege.SiegeLocation;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class SM_SIEGE_LOCATION_INFOTest {

    @Test
    void singleLocationConstructorCreatesOneLocationUpdate() throws Exception {
        SiegeLocation location = location(1011);

        SM_SIEGE_LOCATION_INFO packet = new SM_SIEGE_LOCATION_INFO(location);

        Integer infoType = field(packet, "infoType");
        assertEquals(1, infoType);
        Map<?, ?> locations = field(packet, "locations");
        assertEquals(1, locations.size());
        assertSame(location, locations.get(1011));
    }

    private static SiegeLocation location(int locationId) throws Exception {
        SiegeLocation location = new ObjenesisStd().newInstance(SiegeLocation.class);
        Field locationIdField = SiegeLocation.class.getDeclaredField("locationId");
        locationIdField.setAccessible(true);
        locationIdField.set(location, locationId);
        return location;
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
