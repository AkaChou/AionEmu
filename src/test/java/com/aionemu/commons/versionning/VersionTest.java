package com.aionemu.commons.versionning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class VersionTest {

    @Test
    void locatesSpringBootExecutableJar() throws Exception {
        Path jar = Path.of(System.getProperty("java.io.tmpdir"), "Aion Emu.jar").toAbsolutePath();
        URL resource = URL.of(URI.create(
            "jar:nested:" + jar.toUri().getRawPath() + "/!BOOT-INF/classes/!/com/aionemu/gameserver/GameServer.class"),
            new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    throw new IOException("Not needed by this test");
                }
            });
        ClassLoader loader = new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                return resource;
            }
        };

        assertEquals(jar.toFile(), Locator.getResourceSource(loader, "com/aionemu/gameserver/GameServer.class"));
    }

    @Test
    void explodedClassDirectoryUsesUnknownVersionWithoutWarning() {
        Logger logger = (Logger) LoggerFactory.getLogger(Version.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        boolean additive = logger.isAdditive();
        logger.setAdditive(false);
        logger.addAppender(appender);
        Version version = new Version();

        try {
            version.loadInformation(Version.class);
        } finally {
            logger.detachAppender(appender);
            logger.setAdditive(additive);
            appender.stop();
        }

        assertEquals("Unknown Revision", version.getRevision());
        assertFalse(appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .anyMatch(message -> message.contains("Unable to get Soft information")));
    }
}
