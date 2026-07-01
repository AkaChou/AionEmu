package com.aionemu.scripts;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MavenPackageRuntimeResourcesTest {

    @Test
    void packagePhaseCopiesRuntimeResourcesUnderAionHome() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(pom.contains("<id>copy-runtime-aion-resources</id>"));
        assertTrue(pom.contains("<phase>package</phase>"));
        assertTrue(pom.contains("<goal>copy-resources</goal>"));
        assertTrue(pom.contains("<directory>${project.basedir}/src/main/resources/aion</directory>"));
        assertTrue(pom.contains("<outputDirectory>${project.basedir}/aion</outputDirectory>"));
        assertTrue(pom.contains("<id>copy-runtime-logback</id>"));
        assertTrue(pom.contains("<outputDirectory>${project.basedir}/aion/log</outputDirectory>"));
    }
}
