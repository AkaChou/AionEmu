# R4 批次施工单：GameWorldServices 静态定位器退役

> 依据：`docs/architecture-optimization/2026-10-10-lifecycle-locator-retirement-plan.zh-CN.md`（R4 行：计划口径 1,287 引用）
> 前置：R1（CoreGameplay，`4ad05b991`）、R2（LocationBootstrap，`4562f39d7`）、R3（FeatureServices，`049223053`）已完成并提交。本批沿用已三次验收的流程、fail-fast getInstance 模板与测试注入套路。

---

## 一、范围与终态

**目标定位器**：`lifecycle/GameWorldServices.java`（142 行，@Component 纯绑定定位器）：
- 构造器持 3×`ObjectProvider`（GeoService、PathService、DropRegistrationService），逐一 `setInstanceProvider` 并写入静态字段；`destroy()` 反向清空 + `clearResolvedServices()`；
- 3 个静态访问器：`geoService()` 78 处、`pathService()` 39 处、`dropRegistrationService()` 1167 处（共 **1284 调用 / 152 消费文件**）；
- 三个访问器均已是「resolved 缓存 + fail-fast ISE」形态（无 Holder、无 `getIfAvailable(() ->` 兜底）——本批改造重点在消费方替换与绑定迁移，而非兜底拆除。

**终态**：
1. 3 个静态访问器全部退役；1284 处消费改走域服务自持 `XxxService.getInstance()`；
2. 3 个目标服务 `getInstance()` 统一为 R1 `DropService` 模板（`resolvedInstance` 对象字段缓存 + fail-fast + `setInstanceProvider` 重置缓存）——以对象字段缓存承接原定位器 `resolved*` 的热点语义；
3. 绑定职责迁入新建 `WorldServiceBindings` @Component（3×ObjectProvider 构造喂入 + destroy 清空）；定位器类删除（含 `clearResolvedServices()`，其唯一外部调用在兼容测试内，随批删除）；
4. `GameWorldServicesTest`（bridge 审计，语义反转）删除，新建 `WorldServiceBindingsTest`（R1/R2/R3 同款三断言）。

**A 类判定 = 0（本批无构造注入子步）**：全库类级字段初始化器扫描 `^\t(?!\t).*(?:private|protected|public|final|static).*=\s*GameWorldServices\.` **0 命中**；3 个目标服务与定位器的互调全部位于方法体内（见 §2.3）→ 统一 B 类直换。

---

## 二、现状核实（2026-10-13）

### 2.1 引用总量与分布

- `GameWorldServices.` 引用 **1432 行** = import **152 文件**（149 exact + `PlayerController` 通配 `lifecycle.*` + `WalkerGroup` 注释引用无 import + `GeoService` 全限定内联）+ 外部访问器调用 **1284 处** + 定位器类内 provider 自赋值。
- 访问器调用明细：

| 访问器 | 调用数 | 消费文件 | 热点 |
|---|---:|---:|---|
| `dropRegistrationService()` | 1167 | 102 | `instance/handlers/scripts` 集中 91 文件（KromedesTrial 76、BeshmundirTemple 63、EmpyreanCrucible 46…） |
| `geoService()` | 78 | 48 | 分布零散（skillengine/effect 11、spawnengine、controllers 等） |
| `pathService()` | 39 | 11 | world/geo、movement、controllers |
| 合计 | 1284 | 152 | geo∩path 7 文件，drop∩(geo|path) 2 文件 |

- 消费方目录 top：instance/handlers 91、skillengine/effect 11、spawnengine 6、controllers 5、ai2/handler 4。

### 2.2 3 个目标服务通道形态

| 服务 | 现状 | 本批动作 |
|---|---|---|
| `GeoService`（world/geo） | fail-fast ISE，**无 `resolvedInstance` 缓存**（每次调用穿 provider） | 加 `resolvedInstance` 缓存；`setInstanceProvider` 重置 |
| `PathService`（world/geo/path） | **Holder 兜底残留**（`provider == null ? Holder.INSTANCE : provider.getIfAvailable(() -> Holder.INSTANCE)` + 私有 `Holder` 内部类）；`instanceProvider` 为 **Lombok 字段级 `@Setter`**（63-64 行，全文件唯一用处） | 删 Holder 内部类改 fail-fast + 缓存；删 `@Setter` + `import lombok.Setter`（11 行）改手写 setter（R3 同款处理） |
| `DropRegistrationService`（services/drop） | fail-fast ISE，**无 `resolvedInstance` 缓存** | 加 `resolvedInstance` 缓存；`setInstanceProvider` 重置 |

- 三服务均已有 `getInstance`/`setInstanceProvider` 通道（无新增通道）。
- `PathService implements DisposableBean` 的 `destroy()`（关闭 pathfinders 线程池）为服务自身生命周期，**保留不动**。
- **Holder 拆除后全库 `getIfAvailable(() ->` 残留 7 → 6 处**（余：NetworkController、SkillEngine、LifeStatsRestoreService、ChatProcessor、RiftManager、ChatProcessRuntimeBridge）。

### 2.3 特例

- **PathService ⇄ GeoService 双向互调**（全部位于方法体内，B 类直换、无构造环）：
  - `GeoService` → `pathService()` 3 处（120、147、162 行，**全限定内联**写法 `com.aionemu.gameserver.lifecycle.GameWorldServices.pathService().obstacleChanged(...)`）；
  - `PathService` → `geoService()` 7 处（160、233、383、784、850、958、960 行，短名引用）→ 替换后需补 `GeoService` import、删 `GameWorldServices` import。
- **跨域引用（本批不动，记录）**：`PathService:805/858` → `GameWorldBootstrapServices.world()` 2 处（计划表外域，后续批次）；`DropRegistrationService:133` → `GameEventServices.eventService()` 1 处（R5 关联 Fallbacks 系列域）。
- **特殊消费形态**：
  - `PlayerController`（import 为通配 `lifecycle.*`，18 行；调用 701 行 `geoService().canSee(...)`）→ 替换时插 `com.aionemu.gameserver.world.geo.GeoService` 精确 import；
  - `WalkerGroup:398-399` 为**注释内引用**（javadoc 示例代码）→ 文本替换一并归零；
  - `GeoService` 自身 3 处全限定内联 → 改短名 + 补同域 `path.PathService` import。
- **自调**：三服务对本域访问器自调为 0（GeoService 的 3 处与 PathService 的 7 处均为互调，已归入上表）。

### 2.4 装配与测试现状

- 定位器为 @Component 由容器构造；lifecycle 包内无其它类引用 `GameWorldServices`；无对应 `*Lifecycle` 类。
- 测试引用 **7 文件**：

| 测试 | 引用点 | 处置 |
|---|---|---|
| `GameWorldServicesTest` | bridge 审计（断言生产代码不得出现 `GeoService.getInstance()`/`PathService.getInstance()`，语义反转） | **删除**，新建 `WorldServiceBindingsTest`（三断言） |
| `GameServiceProviderCompatibilityTest#singletonAccessorsUseSpringProvidersBeforeLegacyFallbacks` | 457-459 `new GameWorldServices(...)`；678/680 `assertSame(x, GameWorldServices.geoService()/pathService())`（与前一行 `getInstance()` 断言重复）；817 `clearResolvedServices()` | 构造改 `new WorldServiceBindings`；678/680 删行；817 删行 |
| `AionBootApplicationTest` | 41 import；195 `context.getType("gameWorldServices")` | 改 `worldServiceBindings` |
| `RetailConditionSpawnEngineTest` | 21 import；61 类字段类型；74-75 构造（后两参 null/真 provider） | import + 字段类型 + 构造改 `WorldServiceBindings` |
| `SM_LOOT_STATUSTest` | 13 import；22 类字段类型；33-34 构造 | 同上（46 行已用 `DropRegistrationService.getInstance()`） |
| `AbstractCollisionObserverTest` | 22 import；52-53 局部变量构造 | import + 构造改 `WorldServiceBindings`（局部变量名保留） |
| `NpcMoveControllerPathTest` | 26 import；860-862、871、885 **反射替换缓存字段** `GameWorldServices.resolvedGeoService` | **重锚**：`GeoService.class.getDeclaredField("resolvedInstance")`（R4 后 getInstance 读该缓存，语义等价） |

---

## 三、处置规则

### 3.1 C 类收口（3 服务统一模板）

统一为 R1 `DropService` 形态：

```java
private static volatile XxxService resolvedInstance;

public static XxxService getInstance() {
    XxxService resolved = resolvedInstance;
    if (resolved != null) {
        return resolved;
    }
    ObjectProvider<XxxService> provider = instanceProvider;
    resolved = provider == null ? null : provider.getIfAvailable();
    if (resolved == null) {
        throw new IllegalStateException("XxxService 未由 Spring 提供："
            + (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
            + "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
    }
    resolvedInstance = resolved;
    return resolved;
}
```

- GeoService / DropRegistrationService：加 `resolvedInstance` 字段 + getInstance 缓存版 + `setInstanceProvider` 内 `resolvedInstance = null;`；
- PathService：删 `Holder` 内部类 + 重写 getInstance + 删 `@Setter` 注解与 `import lombok.Setter` 改手写 `setInstanceProvider`（重置缓存）。

### 3.2 B 类（消费方 → 3 个映射）

| 访问器 | 目标 |
|---|---|
| `GameWorldServices.geoService()` | `GeoService.getInstance()` |
| `GameWorldServices.pathService()` | `PathService.getInstance()` |
| `GameWorldServices.dropRegistrationService()` | `DropRegistrationService.getInstance()` |

- 脚本批量 + 计数断言（1284）+ grep 归零 + build 兜底（同 R1/R2/R3）；import 就地改写；`PlayerController` 通配场景插精确 import；`GeoService` 全限定内联改短名 + 补 import。
- 替换映射同时覆盖服务互调 10 处（不排除目标服务自身文件）。

### 3.3 绑定迁移

- 新建 `lifecycle/WorldServiceBindings.java` @Component implements DisposableBean：构造器 3×ObjectProvider（参数名沿用旧定位器 `geoServiceProvider`/`pathServiceProvider`/`dropRegistrationServiceProvider`）逐一 `setInstanceProvider`；destroy() 全部置 null（重置缓存由 setInstanceProvider 承担）；
- 删除 `GameWorldServices.java`（含 `clearResolvedServices()`——唯一调用点在兼容测试 817 行，同步删）。

### 3.4 测试处置

见 §2.4 表。`WorldServiceBindingsTest` 三断言（R1/R2/R3 同款）：
1. `retiredLocatorClassDoesNotComeBack`；
2. `productionCodeNeverTouchesTheRetiredLocator`（src/main 全库不含 `GameWorldServices`）；
3. `bindingsComponentFeedsEveryDomainServiceChannel`（3 服务 `setInstanceProvider(`）。

---

## 四、动作序列（4 子步，逐步验证）

1. **C 类收口**（3 服务统一模板 + PathService 删 Holder/@Setter）→ `build_project` + 断言复核；
2. **B 类批量替换**（3 映射 / 1284 调用 / 152 文件）→ build + grep 残留归零 + 抽样；
3. **绑定迁移 + 删定位器**（WorldServiceBindings 新建 + 原类删除 + 测试编译级改写：构造 ×4、字段类型 ×2、AionBoot 断言、NpcMoveControllerPathTest 反射重锚、compat 678/680/817 删行）→ build；
4. **测试语义改写与分批复跑**（删 GameWorldServicesTest、建 WorldServiceBindingsTest）→ 全绿 → 提交授权。

（A 类 = 0，无构造注入子步。）

## 五、验收门禁

- **G1 反向 grep**：`grep -rn 'GameWorldServices' src/main src/test` 仅剩新审计断言文本与文档；
- **G2 构建**：build 全绿；
- **G3 测试**：lifecycle + services + boot + 功能测试相关分批全绿（截断日志 `testFailed=0` 终检）；
- **G4 缓存语义**：3 服务 getInstance 含 `resolvedInstance`；`setInstanceProvider` 重置缓存；dropRegistration（1167 处热点）与 `NpcMoveControllerPathTest` 反射路径核对；
- **G5 提交**：用户授权后提交（英文 message、显式路径暂存、单 commit 主体）。

## 六、风险与盘点

- **热点承接**：定位器 `resolved*` 静态缓存（3 字段）→ 各服务 `resolvedInstance`（单字段）；provider 皆是容器内单例，语义等价；`setInstanceProvider` 重置缓存保证重绑定安全；
- **反射耦合**：`NpcMoveControllerPathTest:860` 直接反射定位器私有缓存字段——重锚到 `GeoService.resolvedInstance` 后语义等价（getInstance 先读该字段）；
- **跨域引用**：PathService→WorldBootstrap 2 处、DropRegistration→GameEventServices 1 处，本批不动，记录待后续批次；
- **门禁**：PathService 删 Holder 后满足 `LegacySingletonFallbackAuditTest` RETIRED 断言；`getIfAvailable(() ->` 残留 7 → 6；
- **契约面**：`TestServiceProviders.install` 依赖 `setInstanceProvider` 签名（保留不动）；
- **计划表外域盘点结论（R3 遗留项收口）**：`GameEngineServices`（5 通道/821 调用/415 文件）与 `GameWorldBootstrapServices`（5 通道/1011 调用/375 文件）确为计划表遗漏的同类定位器、结构与 R2/R3 同构，可套用已验收流程；两域与 R4/R5 无交叉依赖（R4 目标服务对 WorldBootstrap 的 2 处引用即为仅有的接触面），建议 R5 后按替换面推进。

---

## 八、施工记录（2026-10-13）

四子步全部完成；G1/G2/G4 达成，G3 实跑 **48 个测试类全绿**（GameLegacyServiceBridgeConfigurationTest 56/56，截断日志 testFailed=0 终检）：

- **子步 1（C 类收口）**：3 服务统一 DropService 模板——GeoService、DropRegistrationService 加 `resolvedInstance` 缓存（fail-fast 形态保留，`setInstanceProvider` 重置缓存）；PathService 删 `Holder` 内部类 + 删 Lombok 字段级 `@Setter`（含 `import lombok.Setter`）改手写 setter + 缓存。首跑脚本 PathService 段漏写盘，修正重跑后终检达标（三服务 `resolvedInstance` 齐备、`Holder`=0、`@Setter`=0；全库 `getIfAvailable(() ->` 7 → 6）。
- **子步 2（B 类替换）**：**152 文件 / 1284 调用**（dropRegistrationService 1167、geoService 78、pathService 39）→ `XxxService.getInstance()`；import 149 删 156 增；特例：`PlayerController` 通配 `lifecycle.*` 场景插精确 `GeoService` import、`WalkerGroup:398-399` 注释引用文本替换、`GeoService` 自身 3 处全限定内联改短名 + 补 `PathService` import。首跑 `ensure_import` 对通配 import 行正则失配崩溃并部分写盘 → `git checkout -- src/main/java` 整体回滚、子步 1 脚本化重放、修正正则（`[\w.]+` → `[\w.*]+`）后重跑全对。
- **子步 3（绑定迁移）**：新建 `WorldServiceBindings`（@Component + DisposableBean，3×ObjectProvider 构造喂入 + destroy 清空）；删除 `GameWorldServices`（含 `clearResolvedServices()`，唯一外部调用点在兼容测试内随批删除）；测试编译级改写 6 文件（GameServiceProviderCompatibilityTest 构造改 `new WorldServiceBindings` + 冗余双断言删 3 行、AionBootApplicationTest 装配断言改 `worldServiceBindings`、RetailConditionSpawnEngineTest / SM_LOOT_STATUSTest / AbstractCollisionObserverTest 构造改、NpcMoveControllerPathTest 反射重锚 `GeoService.resolvedInstance` + 变量更名）。
- **子步 4（测试语义改写与复跑）**：`GameWorldServicesTest`（语义反转审计）删除，新建 `WorldServiceBindingsTest`（三断言：定位器不回归 / 生产代码零引用 / 3 通道喂入）。48 类矩阵分批复跑全绿；期间捕获一处语义级失效——`GameLegacyServiceBridgeConfigurationTest#createsSpringManagedGeoPathServicesInsteadOfLegacySingletons` 原 `assertNotSame(PathService.getInstance(), context.getBean(PathService.class))` 依赖 Holder 本地兜底（该类不引用 `GameWorldServices`，故不在 §2.4 编译级清单内），按同文件 OutpostService 用例的 R3 同构处置改 `assertThrows(IllegalStateException.class, PathService::getInstance)`。

**测试矩阵（48 类，全 0 失败）**：

| 域 | 类 / 用例数 |
|---|---|
| lifecycle | GameServiceProviderCompatibility 16、WorldServiceBindings 3、CoreGameplayServiceBindings 3、LocationBootstrapServiceBindings 3、FeatureServiceBindings 3、LegacySingletonFallbackAudit 1、GameServiceSelfSingletonCleanup 1、GameDisputeLandLifecycle 3、GameDredgionLifecycle 5、GameRewardServicesLifecycle 4、GameProtectorConquerorLifecycle 3、GameEventRuntimeLifecycle 4 |
| world/geo/path 及功能面 | NpcMoveControllerPathTest 68、RetailConditionSpawnEngineTest、DespawnableGeoHookTest、DropRegistrationServiceTest、SM_LOOT_STATUSTest、AbstractCollisionObserverTest、GeoServiceGroundSearchTest、GeoServiceSkillObstacleTest、PathDataTest、PathServiceCompressionTest、PathServiceConcurrencyTest、SpatialPathfinderTest |
| services | GameLegacyServiceBridgeConfiguration 56、ServiceConstructorInjection 48、ServiceInternalCollectionImplementation 6、ServiceMapImplementation 6、ServiceCollectionImplementation 3、TemporaryVisibleObjectMap 6、AStationService 3、BaseService 1、KiskService 1、ProtectorConquerorService 2、SiegeService 2、DropDistributionService 1、LadderService 3、SystemMailService 2、PlayerLimitService 2、WebshopService 1、ScheduleHotReload 1 |
| model / persistence / questEngine / dataholders | ModelCollectionImplementation 18、TransactionSafety 5、ExternalRewardAdvanceReentryContract 2、XmlDataLoader 25 |
| boot | AionBootApplication 13、LocalizedLogCalls 1、LocalizedLogArguments 2 |

**观察项（记录给后续批次）**：

- 全库 `getIfAvailable(() ->` 残留 7 → **6 处**（NetworkController、SkillEngine、ChatProcessor、LifeStatsRestoreService、RiftManager、ChatProcessRuntimeBridge），均属后续批次域。
- 跨域引用本批未动：`PathService:805/858` → `GameWorldBootstrapServices.world()` 2 处、`DropRegistrationService:133` → `GameEventServices.eventService()` 1 处。
- 计划表外域盘点结论维持：GameEngineServices（5 通道/821 调用/415 文件）与 GameWorldBootstrapServices（5 通道/1011 调用/375 文件）建议 R5 后按替换面推进。

---

## 附表 A：3 个访问器 → 目标服务映射

| # | 访问器 | 目标服务（getInstance） | 调用数 |
|---|---|---|---:|
| 1 | `dropRegistrationService()` | `com.aionemu.gameserver.services.drop.DropRegistrationService` | 1167 |
| 2 | `geoService()` | `com.aionemu.gameserver.world.geo.GeoService` | 78 |
| 3 | `pathService()` | `com.aionemu.gameserver.world.geo.path.PathService` | 39 |

## 附表 B：3 个域服务清单（C 类收口对象）

| 服务 | 形态 | 动作 |
|---|---|---|
| `GeoService` | fail-fast 无缓存 | 加 `resolvedInstance` |
| `PathService` | Holder + Lombok `@Setter` | 删 Holder、删 `@Setter`、改手写 setter + 缓存 |
| `DropRegistrationService` | fail-fast 无缓存 | 加 `resolvedInstance` |
