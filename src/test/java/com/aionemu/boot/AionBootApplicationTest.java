package com.aionemu.boot;

import com.aionemu.gameserver.lifecycle.CoreGameplayServiceBindings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.AionBootApplication;
import com.aionemu.boot.config.AionGameProperties;
import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyChatConfigOverrides;
import com.aionemu.boot.config.LegacyChatProperties;
import com.aionemu.boot.config.LegacyConfigOverrides;
import com.aionemu.boot.config.LegacyGameProperties;
import com.aionemu.boot.config.LegacyLoginConfigOverrides;
import com.aionemu.boot.config.LegacyLoginProperties;
import com.aionemu.boot.lifecycle.AionProcessRuntimeBridge;
import com.aionemu.boot.lifecycle.AionServiceLauncher;
import com.aionemu.boot.lifecycle.ChatServerLifecycleGateway;
import com.aionemu.boot.lifecycle.ChatServerRuntimeBridge;
import com.aionemu.boot.lifecycle.ChatServiceLifecycle;
import com.aionemu.boot.lifecycle.GameServiceLifecycle;
import com.aionemu.loginserver.lifecycle.LoginStartupSequence;
import com.aionemu.boot.lifecycle.LoginServiceLifecycle;
import com.aionemu.boot.transport.AionTransportBoundary;
import com.aionemu.boot.transport.NettyTransportLifecycle;
import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ChatServer;
import com.aionemu.chatserver.ChatServerRuntime;
import com.aionemu.chatserver.ChatServerStartupBridge;
import com.aionemu.chatserver.ShutdownHook;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.lifecycle.FeatureServiceBindings;
import com.aionemu.gameserver.lifecycle.WorldServiceBindings;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameAdminPanelLifecycle;
import com.aionemu.gameserver.lifecycle.GameBattlefieldLifecycle;
import com.aionemu.gameserver.lifecycle.GameChatServerOverrideLifecycle;
import com.aionemu.gameserver.lifecycle.GameCleaningLifecycle;
import com.aionemu.gameserver.lifecycle.GameCustomEventsLifecycle;
import com.aionemu.gameserver.lifecycle.GameDisputeLandLifecycle;
import com.aionemu.gameserver.lifecycle.GameDredgionLifecycle;
import com.aionemu.gameserver.lifecycle.GameEnginesLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeLifecycle;
import com.aionemu.gameserver.lifecycle.GameGeoPathLifecycle;
import com.aionemu.gameserver.lifecycle.GameHtmlLifecycle;
import com.aionemu.gameserver.lifecycle.GameHousingLifecycle;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameLoggingLifecycle;
import com.aionemu.gameserver.lifecycle.GameNetworkStartupLifecycle;
import com.aionemu.gameserver.lifecycle.GameOptionalServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameProtectorConquerorLifecycle;
import com.aionemu.gameserver.lifecycle.GameRatioLimitLifecycle;
import com.aionemu.gameserver.lifecycle.GameRewardServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameRuntimeServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSeasonRankingLifecycle;
import com.aionemu.gameserver.lifecycle.GameServerNetworkLifecycle;
import com.aionemu.gameserver.lifecycle.GameScheduledServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSiegeScheduleLifecycle;
import com.aionemu.gameserver.lifecycle.GameSpawnLifecycle;
import com.aionemu.gameserver.lifecycle.GameStaticDataLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupCompletionLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupHooksLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupLogLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupSequenceLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemPropertiesLifecycle;
import com.aionemu.gameserver.lifecycle.GameThreadPoolLifecycle;
import com.aionemu.gameserver.lifecycle.GameUtilityServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldActivationLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapLifecycle;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.commons.utils.AionRuntimeMode;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class AionBootApplicationTest {

	/**
	 * 装配审计上下文的 JDBC 自动配置排除清单；{@code JdbcClientAutoConfiguration} 也必须排除，
	 * 否则它会经由 {@code dataSourceScriptDatabaseInitializer} 拉起数据源解析。{@code FlywayAutoConfiguration}
	 * 同理：夹具声明的 {@code JdbcClient} 被 Boot 视为依赖库初始化，会拉起 {@code flywayInitializer} 真连库，
	 * 使这些用例依赖本机 MySQL 是否放行无凭据连接。
	 * JDBC auto-configurations excluded from assembly-audit contexts; {@code JdbcClientAutoConfiguration}
	 * must be listed too, or it drags in DataSource resolution through
	 * {@code dataSourceScriptDatabaseInitializer}. {@code FlywayAutoConfiguration} likewise: the fixture's
	 * {@code JdbcClient} counts as depending on database initialization, which starts {@code flywayInitializer}
	 * against the real database and makes these cases depend on whether the local MySQL accepts a
	 * credential-less connection.
	 */
	private static final String JDBC_AUTO_CONFIGURATION_EXCLUSIONS = String.join(",",
		"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration",
		"org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
		"org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
		"org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration",
		"org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration");

	@AfterEach
	void clearBootRuntimeFlags() {
		System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
	}

	@Test
	void bootApplicationIsTheOnlyServerEntrypoint() {
		assertTrue(hasPublicStaticMain(AionBootApplication.class));
		assertFalse(hasPublicStaticMain(ChatServer.class));
		assertFalse(hasPublicStaticMain(GameServer.class));
	}

	@Test
	void productionSourcesExposeOnlyBootMain() throws IOException {
		Path mainSource = Path.of("src/main/java");
		List<Path> productionMainFiles;
		try (var paths = Files.walk(mainSource)) {
			productionMainFiles = paths
				.filter(path -> path.toString().endsWith(".java"))
				.filter(AionBootApplicationTest::declaresPublicStaticMain)
				.sorted()
				.toList();
		}

		assertEquals(List.of(mainSource.resolve("com/aionemu/AionBootApplication.java")), productionMainFiles);
	}

	@Test
	void javassistCallbackWeavingIsRemovedFromBuildAndRuntime() throws IOException {
		String pom = Files.readString(Path.of("pom.xml"));
		assertFalse(pom.contains("javassist"));
		assertFalse(pom.contains("CallbackBuildTimeWeaver"));
		assertFalse(pom.contains("exec-maven-plugin"));
		assertFalse(Files.exists(Path.of("src/main/java/com/aionemu/commons/callbacks/weaver")));
	}

	@Test
	void legacyGameServerStartMethodsAreCompletelyRemoved() {
		List<Method> startMethods = Arrays.stream(GameServer.class.getDeclaredMethods())
			.filter(method -> method.getName().equals("start"))
			.filter(method -> Modifier.isPublic(method.getModifiers()))
			.filter(method -> Modifier.isStatic(method.getModifiers()))
			.toList();

		assertTrue(startMethods.isEmpty());
	}

	@Test
	void bootApplicationScansGameLifecycleBeans() {
		try (AnnotationConfigApplicationContext context = bootContextWithoutDataSource()) {
			assertTrue(context.containsBean("aionServiceLauncher"));
			assertTrue(context.containsBean("gameStartupSequenceLifecycle"));
		}
	}

	@Test
	void bootApplicationScansGameLifecyclesAsEagerSpringBeans() {
		try (AnnotationConfigApplicationContext context = bootContextWithoutDataSource()) {
			assertEquals(AdminService.class, context.getType("adminService"));
			assertEquals(GameRuntimeServicesLifecycle.class, context.getType("gameRuntimeServicesLifecycle"));
			assertEquals(CoreGameplayServiceBindings.class, context.getType("coreGameplayServiceBindings"));
			assertEquals(FeatureServiceBindings.class, context.getType("featureServiceBindings"));
			assertEquals(WorldServiceBindings.class, context.getType("worldServiceBindings"));
			assertEquals(GameEngineServices.class, context.getType("gameEngineServices"));
			assertEquals(GameBattlefieldLifecycle.class, context.getType("gameBattlefieldLifecycle"));
			assertEquals(GameUtilityServicesLifecycle.class, context.getType("gameUtilityServicesLifecycle"));
			assertLazy(context.getBeanFactory(), "adminService");
		}
	}

	@Test
	void bootApplicationScansServerRuntimeBridgeBeansLazily() {
		try (AnnotationConfigApplicationContext context = bootContextWithoutDataSource()) {
			assertEquals(ChatServerRuntimeBridge.class, context.getType("chatServerRuntimeBridge"));
			assertEquals(AionProcessRuntimeBridge.class, context.getType("aionProcessRuntimeBridge"));
			assertLazy(context.getBeanFactory(), "chatServerRuntimeBridge");
			assertLazy(context.getBeanFactory(), "aionProcessRuntimeBridge");
		}
	}

	@Test
	void springApplicationStartsWithServicesDisabled() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AionBootApplication.class)
			.web(WebApplicationType.NONE)
			.bannerMode(org.springframework.boot.Banner.Mode.OFF)
			.run(
				"--boot-smoke=true",
				"--spring.autoconfigure.exclude=" + JDBC_AUTO_CONFIGURATION_EXCLUSIONS,
				"--spring.flyway.enabled=false",
				"--aion.services.login.enabled=false",
				"--aion.services.chat.enabled=false",
				"--aion.services.game.enabled=false"
			)) {
			assertTrue(context.isActive());
			assertTrue(AionRuntimeMode.isBootEmbedded());
		}
	}

	@Test
	void chatRuntimeBeansStayOptionalByDefault() {
		try (AnnotationConfigApplicationContext context = bootContextWithoutDataSource()) {
			assertHasBean(context, ChatServiceLifecycle.class);
			assertHasBean(context, ChatServerLifecycleGateway.class);
			assertDoesNotHaveBean(context, ChatProcessRuntimeBridge.class);
			assertDoesNotHaveBean(context, ChatServerRuntime.class);
			assertDoesNotHaveBean(context, ChatServerStartupBridge.class);
			assertDoesNotHaveBean(context, IdFactory.class);
			assertDoesNotHaveBean(context, ClientPacketHandler.class);
			assertDoesNotHaveBean(context, NettyServer.class);
			assertDoesNotHaveBean(context, GameServerService.class);
			assertDoesNotHaveBean(context, BroadcastService.class);
			assertDoesNotHaveBean(context, ChatService.class);
			assertDoesNotHaveBean(context, RestartService.class);
			assertDoesNotHaveBean(context, ShutdownHook.class);
		}
	}

	@Test
	void chatServerGuiceBindingsAreSpringBeansWhenChatIsEnabled() {
		try (AnnotationConfigApplicationContext context = chatEnabledBootContext()) {
			assertHasBean(context, ChatProcessRuntimeBridge.class);
			assertHasBean(context, ChatServerRuntime.class);
			assertHasBean(context, ChatServerStartupBridge.class);
			assertHasBean(context, IdFactory.class);
			assertHasBean(context, ClientPacketHandler.class);
			assertHasBean(context, NettyServer.class);
			assertHasBean(context, GameServerService.class);
			assertHasBean(context, BroadcastService.class);
			assertHasBean(context, ChatService.class);
			assertHasBean(context, RestartService.class);
			assertHasBean(context, ShutdownHook.class);

			assertTrue(context.getBeanFactory().getBeanDefinition("chatServerRuntime").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("idFactory").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("clientPacketHandler").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("nettyServer").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("gameServerService").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("broadcastService").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("chatService").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("restartService").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("chatShutdownHook").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("chatProcessRuntimeBridge").isLazyInit());
			assertTrue(context.getBeanFactory().getBeanDefinition("chatServerStartupBridge").isLazyInit());
		}
	}

	@Test
	void productionSourcesDoNotUseGuice() throws IOException {
		String pom = Files.readString(Path.of("pom.xml"));
		assertFalse(pom.contains("<guice.version>"), "pom.xml must not declare guice.version");
		assertFalse(pom.contains("com.google.inject"), "pom.xml must not depend on com.google.inject");
		assertFalse(pom.contains("<artifactId>guice</artifactId>"), "pom.xml must not depend on guice");

		Path mainSource = Path.of("src/main/java");
		try (var paths = Files.walk(mainSource)) {
			List<Path> guicePaths = paths
				.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> path.toString().toLowerCase().contains("guice"))
				.sorted()
				.toList();
			assertEquals(List.of(), guicePaths);
		}

		List<Path> guiceImports;
		try (var paths = Files.walk(mainSource)) {
			guiceImports = paths
				.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> sourceContains(path, "com.google.inject"))
				.sorted()
				.toList();
		}
		assertEquals(List.of(), guiceImports);
	}

	@Test
	void serviceLauncherCanBeCreatedAsSpringBean() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.registerBean(AionServicesProperties.class);
			context.registerBean(AionGameProperties.class);
			context.registerBean(NettyTransportLifecycle.class);
			context.registerBean(AionTransportBoundary.class);
			context.registerBean(LegacyGameProperties.class);
			context.registerBean(LegacyConfigOverrides.class);
			context.registerBean(LegacyLoginProperties.class);
			context.registerBean(LegacyLoginConfigOverrides.class);
			context.registerBean(LegacyChatProperties.class);
			context.registerBean(LegacyChatConfigOverrides.class);
			context.registerBean(AionProcessRuntimeBridge.class);
			context.registerBean(GameLoggingLifecycle.class);
			context.registerBean(GameStaticDataLifecycle.class);
			context.registerBean(GameWorldBootstrapLifecycle.class);
			context.registerBean(GameEventBootstrapLifecycle.class);
			context.registerBean(GameGeoPathLifecycle.class);
			context.registerBean(GameWorldActivationLifecycle.class);
			context.registerBean(GameEnginesLifecycle.class);
			context.registerBean(GameLocationBootstrapLifecycle.class);
			context.registerBean(GameSpawnLifecycle.class);
			context.registerBean(GameEventRuntimeLifecycle.class);
			context.registerBean(GameCleaningLifecycle.class);
			context.registerBean(GameScheduledServicesLifecycle.class);
			context.registerBean(GameCustomEventsLifecycle.class);
			context.registerBean(GameSiegeScheduleLifecycle.class);
			context.registerBean(GameDredgionLifecycle.class);
			context.registerBean(GameBattlefieldLifecycle.class);
			context.registerBean(GameProtectorConquerorLifecycle.class);
			context.registerBean(GameDisputeLandLifecycle.class);
			context.registerBean(GameHtmlLifecycle.class);
			context.registerBean(GameRewardServicesLifecycle.class);
			context.registerBean(GameRuntimeServicesLifecycle.class);
			context.registerBean(GameOptionalServicesLifecycle.class);
			context.registerBean(GameSeasonRankingLifecycle.class);
			context.registerBean(GameServerNetworkLifecycle.class);
			context.registerBean(GameHousingLifecycle.class);
			context.registerBean(GameSystemLifecycle.class);
			context.registerBean(GameNetworkStartupLifecycle.class);
			context.registerBean(GameRatioLimitLifecycle.class);
			context.registerBean(GameStartupHooksLifecycle.class);
			context.registerBean(GameStartupCompletionLifecycle.class);
			context.registerBean(GameStartupSequenceLifecycle.class);
			context.registerBean(GameUtilityServicesLifecycle.class);
			context.registerBean(GameAdminPanelLifecycle.class);
			context.registerBean(GameSystemPropertiesLifecycle.class);
			context.registerBean(GameStartupLogLifecycle.class);
			context.registerBean(GameChatServerOverrideLifecycle.class);
			context.registerBean(GameThreadPoolLifecycle.class);
			context.registerBean(LoginStartupSequence.class,
				() -> new LoginStartupSequence(null, null, null, null, null, null, null));
			context.registerBean(LoginServiceLifecycle.class);
			context.registerBean(ChatServerLifecycleGateway.class);
			context.registerBean(ChatServiceLifecycle.class);
			context.registerBean(GameServiceLifecycle.class);
			context.registerBean(AionServiceLauncher.class);

			context.refresh();

			assertTrue(context.containsBean("aionServiceLauncher"));
		}
	}

	private static boolean declaresPublicStaticMain(Path path) {
		try {
			String source = Files.readString(path);
			return source.matches("(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*(?:final\\s+)?String\\s*\\[\\]\\s+\\w+\\s*\\).*");
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read " + path, e);
		}
	}

	private static boolean hasPublicStaticMain(Class<?> type) {
		for (Method method : type.getDeclaredMethods()) {
			int modifiers = method.getModifiers();
			if (method.getName().equals("main")
				&& Modifier.isPublic(modifiers)
				&& Modifier.isStatic(modifiers)
				&& method.getReturnType() == Void.TYPE
				&& method.getParameterCount() == 1
				&& method.getParameterTypes()[0] == String[].class) {
				return true;
			}
		}
		return false;
	}

	private static void assertHasBean(AnnotationConfigApplicationContext context, Class<?> type) {
		Assertions.assertTrue(
			context.getBeanNamesForType(type, true, false).length > 0,
			() -> "Missing Spring bean for " + type.getName()
		);
	}

	private static void assertDoesNotHaveBean(AnnotationConfigApplicationContext context, Class<?> type) {
		Assertions.assertEquals(
			0,
			context.getBeanNamesForType(type, true, false).length,
			() -> "Unexpected Spring bean for " + type.getName()
		);
	}

	private static void assertLazy(ConfigurableListableBeanFactory beanFactory, String beanName) {
		Assertions.assertTrue(beanFactory.getBeanDefinition(beanName).isLazyInit(), beanName + " should be lazy");
	}

	private static void assertEager(ConfigurableListableBeanFactory beanFactory, String beanName) {
		Assertions.assertFalse(beanFactory.getBeanDefinition(beanName).isLazyInit(), beanName + " should be eager");
	}

	/**
	 * 装配审计上下文的属性：排除全部 JDBC 自动配置，避免无库环境下解析数据源。
	 * Properties for assembly-audit contexts: every JDBC auto-configuration is excluded so no
	 * DataSource is resolved.
	 */
	private static final Map<String, Object> ASSEMBLY_TEST_PROPERTIES = Map.of(
		"spring.autoconfigure.exclude", JDBC_AUTO_CONFIGURATION_EXCLUSIONS);

	private static AnnotationConfigApplicationContext bootContextWithoutDataSource() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
			"bootAssemblyTestProperties",
			ASSEMBLY_TEST_PROPERTIES
		));
		context.register(AionBootApplication.class, NoAccessJdbcBeans.class);
		context.refresh();
		return context;
	}

	/**
	 * 装配审计用的离线 JDBC Bean：游戏服 DAO 尚未迁移到 Flex，仍需注入 {@link JdbcClient} 等；
	 * 未池化数据源只暴露 URL 供 MyBatis-Flex 探测方言，不建立连接，任何真实 SQL 调用都会立即失败。
	 * Offline JDBC beans for assembly audits: the game-server DAOs are not yet migrated to Flex and
	 * still require {@link JdbcClient} and friends. The unpooled data source only exposes its URL so
	 * MyBatis-Flex can detect the dialect; no connection is opened, so any real SQL call fails at once.
	 */
	@Configuration(proxyBeanMethods = false)
	static class NoAccessJdbcBeans {

		@Bean
		DataSource noAccessDataSource() {
			UnpooledDataSource dataSource = new UnpooledDataSource();
			dataSource.setDriver("com.mysql.cj.jdbc.Driver");
			dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/aion_emu");
			return dataSource;
		}

		@Bean
		JdbcClient jdbcClient(DataSource noAccessDataSource) {
			return JdbcClient.create(noAccessDataSource);
		}

		@Bean
		JdbcTemplate jdbcTemplate(DataSource noAccessDataSource) {
			return new JdbcTemplate(noAccessDataSource);
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource noAccessDataSource) {
			return new JdbcTransactionManager(noAccessDataSource);
		}

		@Bean
		TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}
	}

	private static AnnotationConfigApplicationContext chatEnabledBootContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
			"chatEnabledTestProperties",
			Map.of(
				"aion.services.chat.enabled", "true",
				"spring.autoconfigure.exclude", JDBC_AUTO_CONFIGURATION_EXCLUSIONS
			)
		));
		context.register(AionBootApplication.class, NoAccessJdbcBeans.class);
		context.refresh();
		return context;
	}

	private static boolean sourceContains(Path path, String needle) {
		try {
			return Files.readString(path).contains(needle);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read " + path, e);
		}
	}

}
