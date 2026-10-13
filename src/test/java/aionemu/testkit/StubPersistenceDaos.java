package aionemu.testkit;

import static java.lang.reflect.Proxy.newProxyInstance;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aionemu.gameserver.persistence.dao.AbyssRankDao;
import com.aionemu.gameserver.persistence.dao.BlockDao;
import com.aionemu.gameserver.persistence.dao.CraftCooldownDao;
import com.aionemu.gameserver.persistence.dao.EventItemDao;
import com.aionemu.gameserver.persistence.dao.F2paccountDao;
import com.aionemu.gameserver.persistence.dao.FriendDao;
import com.aionemu.gameserver.persistence.dao.HouseDao;
import com.aionemu.gameserver.persistence.dao.HouseObjectCooldownDao;
import com.aionemu.gameserver.persistence.dao.HouseScriptDao;
import com.aionemu.gameserver.persistence.dao.InventoryDao;
import com.aionemu.gameserver.persistence.dao.ItemCooldownDao;
import com.aionemu.gameserver.persistence.dao.ItemStoneDao;
import com.aionemu.gameserver.persistence.dao.LegionDao;
import com.aionemu.gameserver.persistence.dao.LegionMemberDao;
import com.aionemu.gameserver.persistence.dao.MailDao;
import com.aionemu.gameserver.persistence.dao.OldNameDao;
import com.aionemu.gameserver.persistence.dao.PlayerAppearanceDao;
import com.aionemu.gameserver.persistence.dao.PlayerAtreianBestiaryDao;
import com.aionemu.gameserver.persistence.dao.PlayerBindPointDao;
import com.aionemu.gameserver.persistence.dao.PlayerCooldownDao;
import com.aionemu.gameserver.persistence.dao.PlayerCpDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.dao.PlayerEffectDao;
import com.aionemu.gameserver.persistence.dao.PlayerEmotionDao;
import com.aionemu.gameserver.persistence.dao.PlayerEquipmentSettingDao;
import com.aionemu.gameserver.persistence.dao.PlayerEventsWindowDao;
import com.aionemu.gameserver.persistence.dao.PlayerLifeStatDao;
import com.aionemu.gameserver.persistence.dao.PlayerLunaShopDao;
import com.aionemu.gameserver.persistence.dao.PlayerMacrosseDao;
import com.aionemu.gameserver.persistence.dao.PlayerMinionDao;
import com.aionemu.gameserver.persistence.dao.PlayerMotionDao;
import com.aionemu.gameserver.persistence.dao.PlayerNpcFactionDao;
import com.aionemu.gameserver.persistence.dao.PlayerPasskeyDao;
import com.aionemu.gameserver.persistence.dao.PlayerPetDao;
import com.aionemu.gameserver.persistence.dao.PlayerPunishmentDao;
import com.aionemu.gameserver.persistence.dao.PlayerQuestDao;
import com.aionemu.gameserver.persistence.dao.PlayerRecipeDao;
import com.aionemu.gameserver.persistence.dao.PlayerRegisteredItemDao;
import com.aionemu.gameserver.persistence.dao.PlayerSettingDao;
import com.aionemu.gameserver.persistence.dao.PlayerShugoSweepDao;
import com.aionemu.gameserver.persistence.dao.PlayerSkillDao;
import com.aionemu.gameserver.persistence.dao.PlayerSkillSkinDao;
import com.aionemu.gameserver.persistence.dao.PlayerStigmasEquippedDao;
import com.aionemu.gameserver.persistence.dao.PlayerTitleDao;
import com.aionemu.gameserver.persistence.dao.PlayerTransformDao;
import com.aionemu.gameserver.persistence.dao.PlayerVarDao;
import com.aionemu.gameserver.persistence.dao.PlayerWardrobeDao;
import com.aionemu.gameserver.persistence.dao.PortalCooldownDao;

/**
 * 测试用持久层桩：为非 Boot 注解上下文（仅导入 {@link GameLegacyServiceBridgeConfiguration}）提供占位 DAO Bean，
 * 使 playerServiceDaos / mailService / abyssRankingCache 等急切 Bean 得以构造。生产环境由
 * {@code @SpringBootApplication} 组件扫描 {@code @Service} 的 {@code XxxDaoImpl} 提供这些 Bean；本类只在测试装配期替代。
 * Test-scope persistence stubs: supplies placeholder DAO beans to non-Boot annotation contexts that only
 * import {@link GameLegacyServiceBridgeConfiguration}, so eager beans such as playerServiceDaos / mailService /
 * abyssRankingCache can be constructed. In production these beans come from {@code @Service} DaoImpl component
 * scanning; this class only stands in during tests.
 * <p>桩方法不会被调用（相关构造仅做非空校验），故返回默认值即可。
 * Stub methods are never invoked (constructors only null-check), so default values suffice.</p>
 */
@Configuration(proxyBeanMethods = false)
public class StubPersistenceDaos {

	@Bean
	PlayerDao playerDao() {
		return stub(PlayerDao.class);
	}

	@Bean
	OldNameDao oldNameDao() {
		return stub(OldNameDao.class);
	}

	@Bean
	PlayerAppearanceDao playerAppearanceDao() {
		return stub(PlayerAppearanceDao.class);
	}

	@Bean
	PlayerSkillDao playerSkillDao() {
		return stub(PlayerSkillDao.class);
	}

	@Bean
	InventoryDao inventoryDao() {
		return stub(InventoryDao.class);
	}

	@Bean
	PlayerStigmasEquippedDao playerStigmasEquippedDao() {
		return stub(PlayerStigmasEquippedDao.class);
	}

	@Bean
	PlayerEquipmentSettingDao playerEquipmentSettingDao() {
		return stub(PlayerEquipmentSettingDao.class);
	}

	@Bean
	PlayerSettingDao playerSettingDao() {
		return stub(PlayerSettingDao.class);
	}

	@Bean
	PlayerQuestDao playerQuestDao() {
		return stub(PlayerQuestDao.class);
	}

	@Bean
	AbyssRankDao abyssRankDao() {
		return stub(AbyssRankDao.class);
	}

	@Bean
	PlayerPunishmentDao playerPunishmentDao() {
		return stub(PlayerPunishmentDao.class);
	}

	@Bean
	HouseDao houseDao() {
		return stub(HouseDao.class);
	}

	@Bean
	PlayerRegisteredItemDao playerRegisteredItemDao() {
		return stub(PlayerRegisteredItemDao.class);
	}

	@Bean
	ItemStoneDao itemStoneDao() {
		return stub(ItemStoneDao.class);
	}

	@Bean
	MailDao mailDao() {
		return stub(MailDao.class);
	}

	@Bean
	PortalCooldownDao portalCooldownDao() {
		return stub(PortalCooldownDao.class);
	}

	@Bean
	CraftCooldownDao craftCooldownDao() {
		return stub(CraftCooldownDao.class);
	}

	@Bean
	PlayerNpcFactionDao playerNpcFactionDao() {
		return stub(PlayerNpcFactionDao.class);
	}

	@Bean
	PlayerLunaShopDao playerLunaShopDao() {
		return stub(PlayerLunaShopDao.class);
	}

	@Bean
	EventItemDao eventItemDao() {
		return stub(EventItemDao.class);
	}

	@Bean
	PlayerMacrosseDao playerMacrosseDao() {
		return stub(PlayerMacrosseDao.class);
	}

	@Bean
	FriendDao friendDao() {
		return stub(FriendDao.class);
	}

	@Bean
	BlockDao blockDao() {
		return stub(BlockDao.class);
	}

	@Bean
	PlayerTitleDao playerTitleDao() {
		return stub(PlayerTitleDao.class);
	}

	@Bean
	PlayerCpDao playerCpDao() {
		return stub(PlayerCpDao.class);
	}

	@Bean
	PlayerEventsWindowDao playerEventsWindowDao() {
		return stub(PlayerEventsWindowDao.class);
	}

	@Bean
	PlayerAtreianBestiaryDao playerAtreianBestiaryDao() {
		return stub(PlayerAtreianBestiaryDao.class);
	}

	@Bean
	PlayerWardrobeDao playerWardrobeDao() {
		return stub(PlayerWardrobeDao.class);
	}

	@Bean
	F2paccountDao f2paccountDao() {
		return stub(F2paccountDao.class);
	}

	@Bean
	PlayerMotionDao playerMotionDao() {
		return stub(PlayerMotionDao.class);
	}

	@Bean
	PlayerVarDao playerVarDao() {
		return stub(PlayerVarDao.class);
	}

	@Bean
	PlayerRecipeDao playerRecipeDao() {
		return stub(PlayerRecipeDao.class);
	}

	@Bean
	PlayerSkillSkinDao playerSkillSkinDao() {
		return stub(PlayerSkillSkinDao.class);
	}

	@Bean
	PlayerEffectDao playerEffectDao() {
		return stub(PlayerEffectDao.class);
	}

	@Bean
	PlayerCooldownDao playerCooldownDao() {
		return stub(PlayerCooldownDao.class);
	}

	@Bean
	ItemCooldownDao itemCooldownDao() {
		return stub(ItemCooldownDao.class);
	}

	@Bean
	HouseObjectCooldownDao houseObjectCooldownDao() {
		return stub(HouseObjectCooldownDao.class);
	}

	@Bean
	PlayerBindPointDao playerBindPointDao() {
		return stub(PlayerBindPointDao.class);
	}

	@Bean
	PlayerLifeStatDao playerLifeStatDao() {
		return stub(PlayerLifeStatDao.class);
	}

	@Bean
	PlayerEmotionDao playerEmotionDao() {
		return stub(PlayerEmotionDao.class);
	}

	@Bean
	PlayerPetDao playerPetDao() {
		return stub(PlayerPetDao.class);
	}

	@Bean
	PlayerMinionDao playerMinionDao() {
		return stub(PlayerMinionDao.class);
	}

	@Bean
	PlayerShugoSweepDao playerShugoSweepDao() {
		return stub(PlayerShugoSweepDao.class);
	}

	@Bean
	HouseScriptDao houseScriptDao() {
		return stub(HouseScriptDao.class);
	}

	@Bean
	PlayerPasskeyDao playerPasskeyDao() {
		return stub(PlayerPasskeyDao.class);
	}

	@Bean
	PlayerTransformDao playerTransformDao() {
		return stub(PlayerTransformDao.class);
	}

	@Bean
	LegionDao legionDao() {
		return stub(LegionDao.class);
	}

	@Bean
	LegionMemberDao legionMemberDao() {
		return stub(LegionMemberDao.class);
	}

	/** 生成返回默认值的动态代理桩。 / Builds a dynamic-proxy stub returning default values. */
	@SuppressWarnings("unchecked")
	private static <T> T stub(Class<T> type) {
		return (T) newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
			(proxy, method, args) -> defaultValue(method.getReturnType()));
	}

	private static Object defaultValue(Class<?> returnType) {
		if (!returnType.isPrimitive()) {
			return null;
		}
		if (returnType == boolean.class) {
			return false;
		}
		if (returnType == char.class) {
			return '\0';
		}
		if (returnType == long.class) {
			return 0L;
		}
		if (returnType == double.class) {
			return 0d;
		}
		if (returnType == float.class) {
			return 0f;
		}
		if (returnType == void.class) {
			return null;
		}
		return 0;
	}
}
