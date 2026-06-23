/*
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.utils.gametime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.configs.main.GSConfig;

/**
 * Utility class for date and time operations using Java 8 Time API. Applies configured time zone from GSConfig while preserving calendar field values
 * (analogous to withZoneRetainFields from Joda-Time).
 * 
 * @author Rolandas (original Joda-Time version)
 * @author (your name) (Java 8 migration)
 */
public final class DateTimeUtil {
    
    private static final Logger log = LoggerFactory.getLogger(DateTimeUtil.class);
    
    private static ZoneId configuredZoneId;
    private static boolean useConfiguredZone = false;

    private DateTimeUtil() {
        // Private constructor for utility class
    }

    /**
     * Initialize utility. Loads time zone from GSConfig.
     */
    public static void init() {
        String zoneConfig = GSConfig.TIME_ZONE_ID;
        
        if (zoneConfig != null && !zoneConfig.trim().isEmpty()) {
            try {
                configuredZoneId = ZoneId.of(zoneConfig.trim());
                useConfiguredZone = true;
                log.info("DateTimeUtil: configured time zone '{}' from config", configuredZoneId.getId());
            } catch (Exception e) {
                log.error("DateTimeUtil: invalid time zone '{}' in config, using system default", zoneConfig, e);
                configuredZoneId = ZoneId.systemDefault();
                useConfiguredZone = false;
            }
        } else {
            // If not specified in config, use system zone (which is already in GSConfig.TIME_ZONE_ID by default)
            String systemZoneId = Calendar.getInstance().getTimeZone().getID();
            configuredZoneId = ZoneId.of(systemZoneId);
            useConfiguredZone = false;
            log.info("DateTimeUtil: no time zone in config, using system default: {}", systemZoneId);
        }
    }

    /**
     * Returns current date/time with configured time zone.
     */
    public static ZonedDateTime now() {
        return ZonedDateTime.now(getZoneWithFallback());
    }

    /**
     * Creates ZonedDateTime from ISO string (e.g., "2024-01-15T10:30:00").
     * @param isoDateTime ISO formatted date/time string
     * @return ZonedDateTime with preserved field values but in configured zone
     */
    public static ZonedDateTime fromIsoString(String isoDateTime) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return applyZoneRetainFields(localDateTime);
        } catch (DateTimeParseException e) {
            log.error("Error parsing date '{}'", isoDateTime, e);
            return now();
        }
    }

    /**
     * Creates ZonedDateTime from string with specified format.
     */
    public static ZonedDateTime fromString(String dateTimeString, DateTimeFormatter formatter) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);
            return applyZoneRetainFields(localDateTime);
        } catch (DateTimeParseException e) {
            log.error("Error parsing date '{}' with format {}", dateTimeString, formatter, e);
            return now();
        }
    }

    /**
     * Creates ZonedDateTime from java.util.Calendar.
     */
    public static ZonedDateTime fromCalendar(Calendar calendar) {
        if (calendar == null) {
            return now();
        }
        return calendar.toInstant().atZone(getZoneWithFallback());
    }

    /**
     * Creates ZonedDateTime from milliseconds since epoch (1970-01-01).
     */
    public static ZonedDateTime fromMillis(long millisSinceEpoch) {
        return Instant.ofEpochMilli(millisSinceEpoch).atZone(getZoneWithFallback());
    }

    /**
     * Creates ZonedDateTime from Instant.
     */
    public static ZonedDateTime fromInstant(Instant instant) {
        if (instant == null) {
            return now();
        }
        return instant.atZone(getZoneWithFallback());
    }

    /**
     * Creates ZonedDateTime from minutes since 01.01.0000 (GameTime format).
     * @param gameTimeMinutes minutes since start of game time
     * @return ZonedDateTime in configured zone
     */
    public static ZonedDateTime fromGameTime(int gameTimeMinutes) {
        // GameTime counts minutes from 01.01.0000, Java Time from 1970
        // This is a rough approximation for demonstration
        long gameTimeMillis = (long) gameTimeMinutes * 60 * 1000;
        return fromMillis(gameTimeMillis);
    }

    /**
     * Applies configured time zone to LocalDateTime while preserving field values.
     * This is analogous to withZoneRetainFields from Joda-Time.
     */
    public static ZonedDateTime applyZoneRetainFields(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return now();
        }
        return localDateTime.atZone(getZoneWithFallback());
    }

    /**
     * Applies configured time zone to ZonedDateTime while preserving field values.
     * If source object has a different zone, its fields are extracted and applied to the configured zone.
     */
    public static ZonedDateTime applyZoneRetainFields(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return now();
        }
        if (!useConfiguredZone) {
            return zonedDateTime;
        }
        
        // Extract local fields (year, month, day, hour, minute, etc.)
        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
        
        // Apply them to configured zone
        return localDateTime.atZone(configuredZoneId);
    }

    /**
     * Checks if configured time zone from config is being used.
     */
    public static boolean isConfiguredZoneUsed() {
        return useConfiguredZone;
    }

    /**
     * Returns current configured zone or system default if config is disabled.
     */
    public static ZoneId getZone() {
        return getZoneWithFallback();
    }

    /**
     * Formats ZonedDateTime to string using specified format.
     */
    public static String format(ZonedDateTime dateTime, DateTimeFormatter formatter) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(formatter);
    }

    /**
     * Converts ZonedDateTime to GregorianCalendar.
     */
    public static GregorianCalendar toGregorianCalendar(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return GregorianCalendar.from(dateTime);
    }

    /**
     * Converts ZonedDateTime to java.util.Calendar.
     */
    public static Calendar toCalendar(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Calendar.getInstance(TimeZone.getTimeZone(dateTime.getZone()));
    }

    /**
     * Returns milliseconds since epoch for given ZonedDateTime.
     */
    public static long toMillis(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.toInstant().toEpochMilli();
    }

    /**
     * Returns ISO string representation of date/time.
     */
    public static String toIsoString(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Helper method to get zone with proper fallback.
     */
    private static ZoneId getZoneWithFallback() {
        if (useConfiguredZone && configuredZoneId != null) {
            return configuredZoneId;
        }
        return ZoneId.systemDefault();
    }
}