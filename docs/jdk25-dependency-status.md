# JDK 25 dependency status

Last checked: 2026-06-27

This project targets Java 25 bytecode with `maven.compiler.release=25`. Dependency versions below were checked against Maven Central release metadata and Maven Versions Plugin output. Pre-release versions such as alpha, milestone, release candidate, beta, preview, and dev builds are intentionally ignored for this baseline.

## Managed by Spring Boot

| Dependency | Project usage | Spring Boot 4.1.0 BOM | Decision |
| --- | --- | --- | --- |
| `com.zaxxer:HikariCP` | Direct dependency without an explicit version | `7.0.2` in the effective POM | Keep managed by Spring Boot. Maven Central latest is `7.1.0`, but this project should not override Boot's connection-pool baseline unless a concrete bug requires it. |
| `io.netty:netty-all` | Direct dependency without an explicit version | `4.2.15.Final` in the effective POM | Keep managed by Spring Boot. |
| `org.apache.commons:commons-lang3` | Direct dependency without an explicit version | `3.20.0` in the effective POM | Keep managed by Spring Boot. |

## Explicit baseline dependencies

| Dependency | Current version | Maven Central release metadata | JDK 25 status |
| --- | ---: | --- | --- |
| `commons-io:commons-io` | `2.22.0` | latest release `2.22.0` | Current. |
| `org.apache.commons:commons-pool2` | `2.13.1` | latest release `2.13.1` | Current. |
| `com.google.guava:guava` | `33.6.0-jre` | latest release `33.6.0-jre` | Current. |
| `org.javassist:javassist` | `3.32.0-GA` | latest release `3.32.0-GA`, updated `2026-06-21` | Current; required by callback bytecode weaving on newer class-file versions. |
| `jakarta.xml.bind:jakarta.xml.bind-api` | `4.0.5` | newest stable release `4.0.5`; `4.1.0-M1` exists but is a milestone | Keep stable release. |
| `org.glassfish.jaxb:jaxb-runtime` | `4.0.9` | latest release `4.0.9` | Current. |
| `ch.qos.logback:logback-classic` / `logback-core` | `1.5.37` | latest release `1.5.37` | Current. |
| `com.mysql:mysql-connector-j` | `9.7.0` | latest release `9.7.0` | Current. |
| `org.quartz-scheduler:quartz` | `2.5.2` | latest release `2.5.2`, updated `2025-12-01` | Current for this Maven coordinate. No `5.x` release is present under `org.quartz-scheduler:quartz` in Maven Central metadata. |
| `org.slf4j:slf4j-api` | `2.0.18` | newest stable release `2.0.18`; `2.1.0-alpha1` exists but is alpha | Keep stable release. |
| `org.objenesis:objenesis` | `3.5` | latest release `3.5`, updated `2026-01-26` | Current. |
| `org.junit.jupiter:junit-jupiter` | `6.1.0` | latest release `6.1.0` | Current. |
| `org.projectlombok:lombok` | `1.18.46` | latest release `1.18.46` | Current. |

## Stopped or risky dependencies

| Dependency | Status | Reason | Recommendation |
| --- | --- | --- | --- |
| `javolution:javolution` | Removed | Maven Central latest is `5.5.1`, last updated `2011-08-15`. No current JDK 25 support statement was found in release metadata. | Do not reintroduce. The repo-local `FastList` / `FastMap` / `FastSet` compatibility classes have also been removed; use JDK collections directly. |
| `net.sf.trove4j:trove4j` | Removed | Maven Central latest is `3.0.3`, last updated `2014-12-04`. No current JDK 25 support statement was found in release metadata. | Do not reintroduce. Primitive collection needs should use maintained libraries or JDK collections. |

## Vendored JDK compatibility risk

| Code | Status | Evidence | Recommendation |
| --- | --- | --- | --- |
| `com.aionemu.commons.utils.internal.chmv8` | Removed | The vendored Java 8 backport package was deleted and direct users were moved to `java.util.concurrent`. | Do not reintroduce vendored JDK concurrency backports. Use JDK concurrency primitives directly. |

## Build plugins

| Plugin | Current version | Maven Versions result | Decision |
| --- | ---: | --- | --- |
| `maven-enforcer-plugin` | `3.6.3` | latest Maven 3-compatible release | Keep. |
| `maven-compiler-plugin` | `3.15.0` | Maven 4 beta line exists | Keep Maven 3-compatible release. |
| `exec-maven-plugin` | `3.6.3` | latest release | Keep. |
| `maven-surefire-plugin` | `3.5.6` | `3.6.0-M1` exists | Keep stable release. |
| `maven-jar-plugin` | `3.5.0` | Maven 4 beta line exists | Keep Maven 3-compatible release. |
| `spring-boot-maven-plugin` | `4.1.0` | latest release via Spring Boot version | Keep. |

## Sources checked

- Maven Central metadata: `https://repo.maven.apache.org/maven2/org/quartz-scheduler/quartz/maven-metadata.xml`
- Maven Central metadata: `https://repo.maven.apache.org/maven2/com/zaxxer/HikariCP/maven-metadata.xml`
- Maven Central metadata: `https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/maven-metadata.xml`
- Maven Central metadata: `https://repo.maven.apache.org/maven2/javolution/javolution/maven-metadata.xml`
- Maven Central metadata: `https://repo.maven.apache.org/maven2/net/sf/trove4j/trove4j/maven-metadata.xml`
- Maven Central metadata: `https://repo.maven.apache.org/maven2/org/javassist/javassist/maven-metadata.xml`
- Maven Central metadata: `https://repo.maven.apache.org/maven2/org/objenesis/objenesis/maven-metadata.xml`
- Local checks: `mvn versions:display-property-updates`, `mvn versions:display-dependency-updates`, `mvn versions:display-plugin-updates`, `mvn dependency:tree`, `mvn -DskipTests package`, `mvn test`
