# Theobomos (210060000) 红色天空 = SandRain 天气排查记录

日期：2026-09-20　分支：quest（工作区脏；本次只改 WeatherService / Weather 两个文件，未构建、未提交）

## 现象

- 泰奥勃莫斯整张图天空偏红/土黄，像沙尘暴；传送进入瞬间正常，随后变红。
- `//weather` → `No weather.`
- `//weather THEOBOMOS 0`、`//weather reset` 无可见变化。

## 已确认事实

1. 服务端天气表只有沙尘暴档位（没有放晴/after 档）：
   - `src/main/resources/aion/data/static_data/weather_table.xml:49-53`
     - `zone_id=1 code=1 name=Sand_Rain att_ranking=2`
     - `zone_id=1 code=2 name=Sand_Rain_Before att_ranking=2 before=true`
   - `WeatherTable.getWeatherAfter()`（`model/templates/world/WeatherTable.java`）对 `Sand_Rain`
     返回 null → `WeatherService.setNextWeather()` 只能重新随机。
2. 客户端地图数据同源（`/Users/mc/IdeaProjects/58Server/Map/Worlds/lf2a/world.xml`，UTF-16LE，
   `<weather>` 段 528745-529003 行）：
   - `SandRain`：`weather_zone_name=wz_Weatherzone_SandDersert`、`occur_prob=20`、
     `duration=110`、`weather_state=wind`、`sign_weather_name=SandRain_Before`(prob 100, 10)。
   - `SandRain_Before`：`occur_prob=0`、`duration=10`（纯前兆态）。
   - `weather_zone` 多边形点集与本仓 `zones/zones_weather.xml:438` 基本一致。
   - 结论：红色天空就是官方 SandRain 沙尘暴的客户端渲染，不是"红色滤镜 bug"。
3. `//weather` 无参分支只在 GM 站在 `WEATHER` 类型 zone 内才报告
   （`commands/admin/Weather.java:41-56`）；本图唯一天气区
   `WZ_WEATHERZONE_SANDDERSERT_210060000`（`zones_weather.xml:438`）多边形范围
   x 1759–3081 / y 355–3054，故下列点都在区外：
   `//goto Theobomos`(1398,1557)、`Meniherk`(1396,1560)、`obsvillage`(2234,2284)、
   `Josnack`(901,2774)、`Jamanok`(458,1257)、Arari 马戏团点(2232,2282)。
   区内的坐标示例：`//goto Anangke`(2681,847,138)、`2434/2287/70`、`2521/1616/63`、
   `2806/2765/60`（取自本图 NPC 刷点，已用多边形点内判定核对）。
4. "传送进来先正常后变红"= `CM_LEVEL_READY` 里 `WeatherService.loadWeather()`
   （`network/aion/clientpackets/CM_LEVEL_READY.java:105`）下发 `SM_WEATHER`(0x43)，
   客户端先重置天空再应用当前 code。
5. 与噩梦马戏团无关：`NightmareCircusService`/`Nightmare` 只做 OPEN/CLOSED 的 NPC 生成/移除；
   全仓 `new SM_WEATHER(...)` 只出现在 `WeatherService`；`SiegeService.onWeatherChanged()` 为空实现；
   `gameserver.nightmare.circus.enable=false`。Theobomos 另有 Beritra 入侵（已开启）与
   Live Party Concert Hall 事件，同样没有任何天气/天空代码。
6. 按代码复算重掷概率（脚本模拟 20 万轮 DayTime 切换）：
   稳态约 25.5% 的时间处于沙尘暴；单次沙尘暴中位数 ≈30 分钟现实时间，p90 ≈70 分钟，尾部可达数小时。
   （游戏时间 1 天 = 现实 2 小时，DayTime 切换在 4/9/17/22 点，每 25–40 分钟一次。）
   另外 `getRandomWeather()` 里 `else if (chance > 50)` 是死代码，`Sand_Rain_Before`
   会被当成主天气随机选中（本应只是 10 单位的前兆）。

## 待用户实机确认

- 区内 `//weather` 打印的 code（1/2 = 服务端就是沙尘暴；0 = 服务端晴天，红色另有来源）。
- `//weather THEOBOMOS 1` 是否即时变红、再 `0` 是否放晴（判定客户端是否认 code 0）。
- 第二个客户端同点对照 / 关闭客户端天气效果（`SystemOptionGraphics.cfg: WEATHER_FX`）是否消失
  （区分服务端天气 vs 客户端 env 层，Game.dll 里有 `EnvWeatherShow/EnvWeatherHide`、
  `mrt_cb_weatherlayer`、`weather_fog` 等字符串）。

## 未走完的路

- `.agents/summary/weather-theobomos/*.java` 的 Ghidra 脚本目前都因脚本编译
  （`ClassNotFoundException`）失败，未取得客户端 `SM_WEATHER` 处理函数行为证据。

## 追加证据（用户实机）

- 站在天气区内 `//weather` 返回 **0**（`Weather code for region ... is 0`），但天空仍然是红色沙尘暴。
  说明要么服务端当前确实是"无天气"（那红色就不是服务端天气驱动），要么这个读数不可信。
- 读数不可信的路径：`Weather.getWeatherCode(mapId, weatherZoneId)` 找不到匹配条目时
  返回 0（`WeatherService.getWeatherCode()` 末尾 `return 0`），而
  `ZoneData.getWeatherZoneId(zoneTemplate)` 未登记时也返回 0 —— 两种情况在
  `//weather` 输出里都是 "is 0"，无法区分。可用
  `//weather THEOBOMOS 1` → 再 `//weather` 复读来判定探针是否可信。
- 仍待判定：客户端是否认 "0 = 放晴"（强制 1 变红、强制 0 是否转晴）；关闭客户端
  **天气效果**（`SystemOptionGraphics.cfg: WEATHER_FX`）是否让红色消失；换第二个客户端对照。
- 官方参考已找到：`/Users/mc/IdeaProjects/58Server/Map/XML/weather.xml`（全局天气目录：
  rain/wind/fog/darkcloud，含 occur_check_interval/occur_prob/occur_delay/duration/
  sign_weather_name/remain_weather_name/sub_weather_name），以及
  `/Users/mc/IdeaProjects/58Server/server58-source/**/World/WorldWeatherManager.cpp`、
  `WeatherZone.cpp`、`WeatherZoneArea.cpp`（恢复出的官方服务端天气实现）。

## 追加证据 2（用户实机 2）

- 客户端资源侧：`/Users/mc/IdeaProjects/5.8客户端/Data/skybox/lf2a/` 有 5 套天空盒：
  `lf2a_dawn / lf2a_daylight / lf2a_dusk / lf2a_night / lf2a_rain`。
  ⇒ "黄云滚滚"= 客户端切到了**天气版天空盒**（rain 变体），即客户端处于"有天气"状态。
- 用户实测：站在 Theobomos 天气区内（`//goto Anangke`，z=138 在 0.1–500.1 内）
  `//weather THEOBOMOS 1` → **客户端立刻开始沙尘暴** ⇒ SM_WEATHER 有效、客户端认 code。
  随后 `//weather`（无参）仍读 0 ⇒ **探针不可信**（`WeatherEntry()` 晴天条目 zoneId=0，
  `changeRegionWeather()` 又沿用该 0，`getWeatherCode(mapId, 1)` 永远匹配不到 → 返回 0）。
- Eltnen 那次"没下雨"不代表链路坏：`//goto Eltnen` 落点 z=264，而
  `WZ_WEATHERZONE1_210020000` 的 z 范围是 381.975–1181.975（`zones_weather.xml:51`），
  人其实在天气区**下方**，所以 `//weather` 显示 No weather、客户端也不渲染天气。
- 待办与修复方案见下节。

## 修复记录（2026-09-20，源码已改，未构建 / 未重启 / 未提交）

改动文件（分支 `quest`，工作区仍有其它并行修改，未 `git add`）：

1. `src/main/java/com/aionemu/gameserver/services/WeatherService.java`
   - `getRandomWeather()`：收集候选时跳过 `before` / `after` 条目（前兆、残留只作过渡态）；
     删除"同名 + isBefore/isAfter"改写当前天气的死逻辑（真实表名是 `X` / `X_Before` / `X_After`，
     原名永远匹配不到，`WeatherTable.getWeatherAfter()` 因此对所有地图恒返回 null）；
     未选中任何条目时改返回 `clearWeather(zoneId)`。
   - 新增 `clearWeather(int zoneId)`：`new WeatherEntry(zoneId, 0)`，放晴条目不再丢天气区序号。
   - `changeRegionWeather()`：天气区序号固定 `i + 1`（旧实现沿用旧条目 zoneId，会把 0 传播到全表，
     使 `//weather` 与 `CollisionMaterialActor` 读到的代码恒为 0）。
   - `resetWeather()`：空数组跳过 + `clearWeather(i + 1)`；`getWeatherCode()`：无天气表返回 0，不再 NPE。
   - 新增 `getWeatherSnapshot(int mapId)`（返回副本，无表时空数组），供管理命令排查。
2. `src/main/java/com/aionemu/gameserver/commands/admin/Weather.java`
   - 无参分支打印 `(weather zone <id>)`，并额外输出整图快照
     `Server weather of map 210060000: zone 1=1(Sand_Rain)`（站在天气区外也能读到服务端真值）。
   - 新增 `describeWeather(int mapId)`；删除未使用的 `WeatherService` import。

### 修复不能解决的部分（重要，勿误判为已修好）

- **沙尘暴频率不变**：本图每张表只有一个可抽条目 `Sand_Rain`(att 2)，修复前后都是
  **约 25.4% 的时间**处于沙尘暴（DayTime 分档：非午后 33% / 午后 11%，按各时段时长加权），
  单次中位 ≈30 现实分钟、p90 ≈65 分钟（脚本模拟，脚本口径：每个 DayTime 边界掷骰一次）。
  原逻辑的另一半概率命中的 `Sand_Rain_Before` 只是官方 `sign` 前兆态（`sign_duration=10`），
  并不是"更少的沙尘暴"。
- **`code 0` 是否能让客户端放晴仍未证实**：用户已报 `//weather THEOBOMOS 0` / `//weather reset`
  无可见变化。若客户端确实不认 0，则本图只能靠"不再产生 Sand_Rain"来彻底解决。
- `getRandomWeather()` 里 `else if (chance > 50)` 仍是死代码（前一个 `chance > 33` 已覆盖）；
  改它会整体改变全部地图的天气分布，本次不动。

### 官方频率对照（说明模拟器偏离点）

- 客户端 `world.xml`(lf2a) `SandRain`：`occur_prob=20`、`duration=110`、
  `sign_weather_name=SandRain_Before`(`sign_occur_prob=100`, `sign_duration=10`)，无 `remain_weather_name`。
- 全局 `weather.xml`：`wind` 为 `occur_check_interval=30`、`occur_prob=10`、`duration=360`（游戏分钟）。
  游戏时间 1 天 = 现实 2 小时，官方语义是"每若干游戏分钟按概率起一次、持续百余游戏分钟"，
  与本模拟器"每 30 现实分钟整体重掷一次"的粗粒度模型不同。

### 验证步骤（未执行，需用户授权构建/重启）

- 构建：`mvn -q -DskipTests compile`（按仓库规则未执行）。
- 重启后实机：
  1. `//goto Anangke`（区内）→ `//weather` 应显示 `(weather zone 1)` 与整图快照，读数应与实际天空一致；
  2. `//weather THEOBOMOS 1` → 立刻沙尘暴，随后 `//weather` 读数应为 `1`（修复前恒为 `0`）；
  3. `//weather THEOBOMOS 0` → 观察客户端是否恢复晴空；若不恢复，则确认客户端不认 `code 0`，
     需改 `weather_table.xml`（本图不再生成 `Sand_Rain`）或按官方 `occur_prob/duration/interval` 重写调度。

## 移动命令（泰奥勃莫斯天气区内，z 需落在 0.1–500.1）

- 命名点只有 `//goto Anangke`（2681, 847, 138）落在区内；GoTo.java 的其它 Theobomos 命名点全部在区外
  （Theobomos 1398/1557、Meniherk 1396/1560、obsvillage 2234/2284、Josnack 901/2774、Jamanok 458/1257）。
- 任意坐标用 `//moveto <worldId> <x> <y> <z>`（`commands/admin/MoveTo.java`）：
  `//moveto 210060000 2434 2287 70`、`//moveto 210060000 2521 1616 63`、`//moveto 210060000 2806 2765 60`、
  `//moveto 210060000 3000 2000 100`（均已用 `zones_weather.xml:438` 多边形顶点判定为区内）。
- 区外的常见落点（不会渲染天气，用于对照）：`//goto Theobomos`(1398,1557)、`Meniherk`(1396,1560)、
  `obsvillage`(2234,2284)、`Josnack`(901,2774)、`Jamanok`(458,1257)、Arari 马戏团点(2232,2282)。

## 追加证据 3（客户端排除法）

服务端当时已是修复后的构建（IDEA 10:21:48 编译、10:22:05 启动）。用户实机结果：

| 试验 | 结果 | 结论 |
| --- | --- | --- |
| `//weather THEOBOMOS 0`（快照确认 `zone 1=0`） | 天空仍黄 | 天空与 SM_WEATHER 无关 |
| 出天气区 `//goto Theobomos`(1398,1557,31) | 天空仍黄 | 不是天气区/粒子层 |
| `//time day` / `dusk` / `night` | 天空完全不变 | 不是 TOD 天空盒切换 |
| 改名 `data\skybox\lf2a\lf2a_rain.pak` | 无变化 | 不是 lf2a_rain 天空盒 |
| 改名整个 `data\skybox\lf2a` | 无变化 | 本图天空不来自 `skybox` 目录 |
| 改名 `data\skydome` | 客户端崩溃 | 天空来自 **skydome（天空穹顶）** 层 |
| Poeta `//weather POETA 1` → `0` | 天空恢复 | 客户端天气天空本身可恢复 |

客户端数据对照（`58Server/Map/Worlds/<level>/world.xml`，UTF-16LE）：

- LF1(Poeta)：`Rain` + `RainBefore` + **`RainAfter`**（有结束档）。
- LF4(Inggison)：`Rain` 带 `remain_weather_name=RainAfter`（有结束档）。
- LF2A(Theobomos)：`SandRain` **没有** `remain_weather_name`，只有 `sign_weather_name=SandRain_Before`。
  ⇒ 与"Poeta/Inggison 能恢复、Theobomos 锁死"一致。假设：客户端切到天气天空穹顶后，
  该 weather 没有 remain/after 档就无法切回，直到客户端重启/重进地图。

## 服务端处置（本次改动）

`src/main/resources/aion/data/static_data/weather_table.xml`：210060000 由
`zone_count=1 / weather_count=2`（`Sand_Rain` code 1、`Sand_Rain_Before` code 2）改为
`zone_count=1 / weather_count=0`（空天气表），并写明双语原因。

- 影响：`WeatherService.getRandomWeather()` 候选为空 → 恒定 `clearWeather(1)`（code 0），
  本图不再产生沙尘粒子，也不会触发客户端天气天空穹顶。
- GM 仍可 `//weather THEOBOMOS 1` 手动强推（`Weather` 命令的 `getZoneCount() == 0` 守卫不触发）。
- 校验：`xmllint --noout --schema weather_table.xsd weather_table.xml` 通过（XSD 允许 `table` minOccurs=0）。
- 需**重启服务端**才生效（静态数据在启动时加载）；本次未构建、未重启（按仓库规则由用户执行）。

验证（待用户）：

1. 重启服务端后，区内 `//weather` 应恒显示 `zone 1=0`；
2. **完全重启客户端**后进泰奥博莫斯：天空应为晴；若仍黄，则说明该图默认 skydome 本身就是黄云，
   属客户端资源问题，服务端无法再改。

## 追加证据 4（空天气表的 JAXB 契约，2026-09-20）

把 210060000 改成 `weather_count="0"` 后必须先确认反序列化不会留下 null 列表，否则
`WeatherService.setNextWeather()` → `getRandomWeather()` → `getWeathersForZone()` 会在启动时 NPE。

- 探针：`bash .agents/summary/weather-theobomos/probe/run-probe.sh`
  （只把 `WeatherTable.java` 重编译到临时目录，再用 `probe/TheobomosWeatherProbe.jsh` 跑真实 JAXB 反序列化 +
  真实 `WeatherService` 私有方法反射调用；修复前的对照脚本是 `probe/EmptyTableNullProbe.before-fix.jsh`）。
- 未修 `WeatherTable` 前实测：`zoneData = null` → `getWeathersForZone(1)` 抛
  `NullPointerException: Cannot invoke "java.util.List.iterator()" ...`。**即原空表写法会让服务端启动失败**。
- 修复：`WeatherTable.zoneData` 初始化为空 `ArrayList`（JAXB 对缺省集合元素不会写入字段，
  这是 JAXB 集合属性的已知契约），并写明双语原因。
- 修复后同一探针输出：
  - `[table] 210060000 zoneCount=1 weatherCount=0 zoneData=[]`
  - `[lookup] getWeathersForZone(1)=[] getWeatherAfter(clearEntry)=null`
  - `[service] getRandomWeather(null, table, 1) -> zoneId=1 code=0 name=null`（真实私有方法反射调用）
  - `[gm-guard] zoneCount=1` ⇒ `//weather THEOBOMOS 1` 守卫不触发，仍可强推
  - `[regression] poeta zoneData size=7 zone1 entries=7`（初始化列表后 JAXB 正常填充，无回归）

结论：本图服务端状态确定性为 `zone 1 = 0`（不再掷骰），GM 强推通道保留。

## 待用户实机判定的分叉（重要）

重启服务端（静态数据重载）+ **完全重启客户端**后进泰奥博莫斯：

- 天空放晴 ⇒ 之前的"红云滚滚"就是 `Sand_Rain` 天气穹顶被触发且客户端无法回退；本次空表已根治。
- 天空依旧黄/红（此时 `//weather` 应显示 `zone 1=0`）⇒ 说明该图**默认 skydome 本身就是黄云**
  （客户端 `data/skydome/xml/xml.pak` 内含 `lf2a.xml`），与 SM_WEATHER 无关，服务端无解，
  下一步要拆 `xml.pak` 里的 `lf2a.xml` 对比 Poeta 的默认穹顶，或接受为客户端资源设定。

## 追加证据 5（2026-09-20 中午）：天空另有第二条路径 = 贝里特拉入侵的世界入侵穹顶

用户反馈：改完天气后"入图瞬间晴朗、随后整个天空乌云滚滚"，并怀疑"同名地图/地图不对"。

- **地图没有问题**：服务端 `world_maps.xml:22` 只有一条 `name="Theobomos"`（id `210060000`）；
  `WorldMapType.THEOBOMOS(210060000)`；`310110000`=THEOBOMOS_LAB、`301610000`=THEOBOMOS_TEST_CHAMBER
  是同名实例（副本），不是同名大地图。用户 `//gps` 亦为 `210060000`。
- **客户端 skydome 库只有一个 dome**：解包 `5.8客户端/data/skydome/xml/xml.pak` 得到的 `lf2a.xml`（265 字节）为：
  `<SkydomeLibrary Name="lf2a"><Skydome Name="invade_direct_portal">
  <Params path="objects\system\skydome\SkyDome_NewSigong.cgf"/>` —— 即泰奥博莫斯的专属天空就是**入侵传送门穹顶**。
- **服务端在该图刷入侵传送门/黑空**：`spawns/Beritra/210060000_Theobomos.xml` 定义了入侵地点 id 13/14/15；
  `beritra_schedule.xml` 中三者都在 11:00 / 17:00 / 21:00 触发；`custom.properties` `gameserver.beritra.duration = 1`（小时）。
  `BeritraStartRunnable` 时间线：t+0 `adventPortalSP` → **702550 `WorldRaid_Advent_Portal`**（mesh
  `Portal_Vritra_worldraid`，`SpawnEngine` 刷在 210060000 的 2429/2619、555/2231、1771/1356）；
  t+180s `adventDirectingSP`（激光）；**t+300s `adventControlSP` → 702529 `WorldRaid_CTRL_01`（ai `advent_control`，注释即"黑空"）**；
  随后 `startBeritraInvasion(id)`，1 小时后 `stopBeritraInvasion` → `clearAdventObjects`。
- 现象吻合："入图瞬间晴朗"= 客户端先建地图；随后按图内入侵传送门/黑空状态把天空切成
  `SkyDome_NewSigong` 穹顶；**换过去之后服务端重启（11:30:57）或入侵结束都不会切回**（11:33 用户进图仍黄）。
  也吻合：`//weather*`/`//weather reset` 无效、改名 `data/skybox/lf2a` 无效（用的是 dome 不是 skybox）、
  改名 `data/skydome` 崩溃（dome 资源）。
- 待用户确认（唯一判定实验）：
  1. 完全退出客户端重进 + 非 11-12/17-18/21-22 时段进图 → 应保持晴空；
  2. 区内 `//beritra start 13` → 0~1 分钟出现传送门、+5 分钟"黑空" → 天空转黄/乌云；`//beritra stop 13` 观察是否恢复。
- 处置选项：保留入侵（默认，每天 3 次各 1 小时）；或从 `beritra_schedule.xml` 删除 Theobomos 的 id 13/14/15；
  或全局 `gameserver.beritra.enable = false`。想保留入侵但不让天空变化只能改客户端 dome（不建议）。

## 追加证据 6（2026-09-20 12:00）：客户端关卡数据里的"天气天空"与"入侵天空"是两套独立覆盖

解包 `5.8客户端/Levels/lf2a/Level.pak → mission_mission0.xml`（用户侧 unpak 工具，
`/Users/mc/PycharmProjects/unpak/aion_pak.py`）后确认泰奥博莫斯的天空由三层决定：

1. **时段天空（正常）**：`<TimeEnvOption ... sky_box_name="LF2A.TimeEnv.LF2A_Dawn/Daylight/Dusk/Night">`
   → `data/skybox/lf2a/lf2a_daylight` 等；`lf2adaylight_5.dds` 中心像素 (29,106,247) 是蓝天，
   这正是"入图瞬间晴朗"看到的天空。
2. **天气天空（乌云）**：`<WeatherSystem>` 只有两个 `WeatherOption`，**两者的 Sky 都指向
   `LF2A.Weather.LF2A_Rain`**（`data/skybox/lf2a/lf2a_rain`，中心像素 (129,131,145)，深灰云）：
   - `SandRain`：带粒子 `weather_sandrain.sandrain.GraySand`、`EnvColor=087,083,064`（暗黄）。
   - `SandRain_Before`：**同一个乌云天空盒，但 ParticleName 为空**（所以"有乌云、没沙尘"）。
   ⇒ 用户看到的"乌云滚滚"就是 `LF2A.Weather.LF2A_Rain` 天空盒，属于天气态渲染。
3. **特殊覆盖态**：`TimeEnvOption` 另有两条无 zonename 的条目——`invade_direct_portal`
   （skydome `lf2a.invade_direct_portal`，即 `data/skydome/xml/xml.pak` 里那张唯一的穹顶）
   与 `WorldRaid`（skydome `skydome_worldraid.skydome_worldraid`），由客户端在世界入侵/传送门状态时切换。
   `.err` 崩溃日志显示实际客户端根目录是 `F:\永恒之塔5.8\`（与 macOS 侧 `5.8客户端` 需确认是同一份数据）。

**当前的干扰项**：`log/adminaudit.log` 显示 12:00:37/12:00:44 用户执行了 `beritra start 13`
（我建议的复现测试）→ 泰奥博莫斯入侵被手动开启且未停止，客户端又处于本进程内的入侵/天气覆盖态。
因此"关掉 beritra.enable 还是乌云"很可能仍是**客户端进程内的天空覆盖态未清除**，必须完全重启客户端进程才能验证。

下一步判定（唯一顺序）：
1. `//beritra stop 13`（必要时 14/15）确认入侵已停；
2. 区内 `//weather` 记录快照行（应为 `zone 1=0`）；
3. **完全退出客户端进程**（不是换角色/重登）→ 重开 → 进图观察 1 分钟。
   - 放晴 ⇒ 之前的乌云是客户端天气/入侵覆盖态残留，服务端两处改动（天气空表、入侵关闭）成立；
   - 仍乌云 ⇒ 客户端 env 覆盖态自立（与服务器无关），下一步改客户端数据：
     `Levels/lf2a/Level.pak` 的 `mission_mission0.xml` 里把 `WeatherOption` 的
     `SkySkyBox/SkyCloud2D` 与 `WorldRaid`/`invade_direct_portal` 的 `skydome_name` 指向
     `LF2A.TimeEnv.LF2A_Daylight`（可用 `aion_pak.py pack --template` 回封）。

## 追加证据 7（2026-09-20 12:04）：贝里特拉已彻底关闭 → 入侵被排除为当前原因

用户在游戏内执行 `//beritra stop 13` 得到 **"Id 13 is invalid"**（配合 12:00 的 `start 13/1/113/10` 也报错）。
原因：`gameserver.beritra.enable = false` → `BeritraService.initBeritraLocations()` 把 `beritra`
置为 `Collections.emptyMap()` → `isValidBeritraLocationId()` 全部失败 → **当前服务端根本没有入侵地点，
泰奥博莫斯不可能有入侵传送门/黑空对象**。

⇒ 结论：在"入侵已关闭 + 本图天气表为空（服务端恒 code 0）"的前提下天空仍为乌云，
说明把天空切成乌云的是**客户端进程内的天空覆盖态**（`LF2A.Weather.LF2A_Rain` 天气天空盒，
或 `invade_direct_portal`/`WorldRaid` 穹顶），服务端已经无法再影响它；
唯一能在游戏内清掉该状态的动作是**完全退出客户端进程再启动**（换角色/重登/传送都不算）。

待用户回传：区内 `//weather` 的完整输出（新版会打印 `Server weather of map 210060000: zone 1=0`），
以及完全重启客户端进程后进图的观测结果。若完全重启后仍乌云，则需改客户端
`Levels/lf2a/Level.pak → mission_mission0.xml`（把 WeatherOption 的 SkySkyBox/SkyCloud2D 与
WorldRaid/invade_direct_portal 的 skydome_name 指回 `LF2A.TimeEnv.LF2A_Daylight`，用
`aion_pak.py pack --template` 回封）。

## 追加证据 8（2026-09-20 12:10）：客户端 §Level.pak 定位与可修补性

- 位置：**客户端根目录下 `Levels/lf2a/Level.pak`**（同目录还有 `lf2a.pak`、`levellm.pak`、`terrainlm.pak`、
  `PathFind.pak`、`leveletc.pak`）。macOS 侧副本
  `/Users/mc/IdeaProjects/5.8客户端/Levels/lf2a/Level.pak` = 9,766,479 字节，
  MD5 `fb49f0c3f1fce43d798b453e1def6dcf`；用户运行端按 `bin64/AIONClnt.err` 记录的根目录推断为
  `F:\永恒之塔5.8\Levels\lf2a\Level.pak`（两侧需先比 MD5 确认为同一份数据）。
- `Level.pak` 内含 17 个条目，其中 `mission_mission0.xml`（3,755,553 字节）就是
  `<WeatherSystem>` / `<TimeEnvOption ... skydome_name=...>` 的宿主；该 XML 在 pak 内是**明文**（raw 解包首字节 `<Mission ...`），
  可直接编辑。
- 但 `Level.pak` 本身是**混淆过的 zip**（`zipfile.is_zipfile()` = False，签名经 0xFF 异或）；
  `aion_pak.py pack` 写出的是标准 zip，回封后客户端是否接受需实测。
  ⇒ 优先方案：把改好的 XML 作为**散文件**放到 `Levels/lf2a/mission_mission0.xml`（CryPak 通常散文件优先，
  先放一份未改动的同名文件验证优先级，再改内容），避免动 pak。
- 可先在客户端轻量试：`SystemOptionGraphics.cfg` 的 `WEATHER_FX = "0"` + 完全重启客户端，
  用于判定乌云是否来自客户端天气层。

## 追加证据 9（2026-09-20 12:23）：客户端 WeatherSystem 的 after 档缺失与服务端空天气表

用户反馈：替换客户端 `Levels/lf2a/Level.pak` 后天空仍不是晴朗。复核 macOS 侧副本：
`/Users/mc/IdeaProjects/5.8客户端/Levels/lf2a/Level.pak` 仍是 2020-02-21 原文件，
MD5 `fb49f0c3f1fce43d798b453e1def6dcf`，即这次替换没有落到该副本；用户实机应按
`F:\永恒之塔5.8\Levels\lf2a\Level.pak` 核 MD5，确认替换是否生效。

对 `mission_mission0.xml` 做客户端 WeatherSystem 对照后确认：
- Poeta（`lf1`）的 `WeatherOption` 顺序为 Rain / RainBefore / RainAfter / Rain2 / Rain2_Before /
  Rain2_After / Snow，服务端 code 1..7 一一对应，且有 `after` 档；所以 `//weather POETA 1` 后再
  `//weather POETA 0` 能恢复 TimeEnv 晴天。
- Theobomos（`lf2a`）只有 SandRain / SandRain_Before 两个 `WeatherOption`，二者
  `SkySkyBox/SkyCloud2D` 都指向 `LF2A.Weather.LF2A_Rain`（深灰乌云），没有 `after` 档。
  服务端 code 0 只能停掉 `weather_sandrain.sandrain.GraySand` 粒子，客户端无法把天空从
  `LF2A.Weather.LF2A_Rain` 退回 `TimeEnv/Daylight`；这正是“入图瞬间晴、随后红黄云滚滚，
  `//weather THEOBOMOS 0` 后周围沙尘消失但天空不变”的客户端状态机原因。

因此已把 `src/main/resources/aion/data/static_data/weather_table.xml` 中
`<map id="210060000" .../>` **整条移除**（不是只把 weather_count 改成 0）：
`WeatherService` 构造时不再为该图建立天气键，`CM_LEVEL_READY → loadWeather()` 查到 `null` 后
直接返回，不下发任何 `SM_WEATHER`，客户端保持 TimeEnv/Daylight 晴天。
代价：`//weather THEOBOMOS 1` 会提示 `Region has no weather defined`，该图不再有沙尘暴；
若以后要保留零售沙尘暴，需要同时在客户端 `mission_mission0.xml` 增加
`SandRain_After`（Sky 指向 TimeEnv/Daylight）并给服务端补 `after="true"` 条目。

待用户实机验证分叉：
- 重启服务端 + 完全重启客户端后放晴，且 `//weather` 显示 `Server weather of map 210060000: no weather table`
  ⇒ 天气包路径确认为唯一触发源，服务端修复成立。
- 仍红黄 ⇒ 客户端在无天气包时仍自行进入 `LF2A.Weather.LF2A_Rain` 或 `invade_direct_portal` /
  `WorldRaid` 穹顶，下一步只改客户端 `Levels/lf2a/Level.pak`（优先散文件
  `Levels/lf2a/mission_mission0.xml`，回封标准 zip 需单独验证客户端接受度）。

## 追加证据 10（2026-09-20 12:47）：服务端已确认无天气包，天空仍不晴 → 触发点在客户端

`log/console.log` 显示服务端在 12:45:59-12:46:03 重启，`target/classes/.../weather_table.xml`
也在 12:45 更新为无 `210060000` 条目；玩家 12:47:42 进入世界后仍反馈天空不晴朗。
因此可以确定：
- 服务端已经不会再为 210060000 构造天气键，也不会下发 `SM_WEATHER`；
- 剩余红/黄天空来自客户端自身：默认 WeatherOption（`LF2A.Weather.LF2A_Rain`）或
  `invade_direct_portal` / `WorldRaid` 的 cutscene skydome（`NewSigong` / `skydome_worldraid`）。

本轮已生成客户端补丁（未覆盖任何现有客户端文件）：
- 补丁目录：`.agents/summary/weather-theobomos/client-patch-20260920-1249/`
- 回封 pak：`Level.pak`，9,809,314 bytes，MD5 `3d57c104788aae3415b05f44f22e85ca`
- 散文件 XML：`unpacked/mission_mission0.xml`，3,755,199 bytes，
  MD5 `19c0a439c37096943359e5a86f25f40c`
- 改动内容：
  1. `WeatherOption SandRain` / `SandRain_Before` 的 `SkyCloud2D` 与 `SkySkyBox`
     全部从 `LF2A.Weather.LF2A_Rain` 改为 `LF2A.TimeEnv.LF2A_Daylight`；
  2. 两个 WeatherOption 的 `FogApply` 改为 0、`EnvColor` 改为 230,230,230、
     `SandRain` 的 `ParticleName/ParticleType` 清空（防止默认天气态再给天空染色/扬沙）；
  3. `TimeEnvOption name="invade_direct_portal"` 的 `skydome_name` 改为 `default`；
  4. `TimeEnvOption name="WorldRaid"` 的 `sky_box_name` 改为
     `LF2A.TimeEnv.LF2A_Daylight`、`skydome_name` 改为 `default`；
  5. `TimeofDayGroup cutscene="invade_direct_portal"` 与 `cutscene="WorldRaid"` 内的
     `sky_box_name` 改为 `LF2A.TimeEnv.LF2A_Daylight`，`skydome_name` 改为 `default`。

测试顺序（Windows 实机，客户端根目录按 `F:\永恒之塔5.8\`）：
1. 先放散文件：把 `unpacked/mission_mission0.xml` 复制为
   `F:\永恒之塔5.8\Levels\lf2a\mission_mission0.xml`，完全退出客户端进程后重开进图。
2. 若散文件不生效，再备份原 `F:\永恒之塔5.8\Levels\lf2a\Level.pak`，然后用补丁 `Level.pak` 覆盖。
3. 补丁 pak 是标准 zip（unpak 工具回封），客户端若拒绝或崩溃，立即还原备份并把结果回报。
4. 若如此仍红黄，说明红天来自另一条客户端资源/全屏效果，需要进一步用客户端控制台或
   Game.dll 侧定位；当前已排除服务端天气包。

## 追加证据 11（2026-09-20 13:58）：天空已恢复但环境仍是黄昏 → cutscene TimeEnv 竞争

用户实机回报：替换第一版补丁 `Level.pak`（MD5 `3d57c104788aae3415b05f44f22e85ca`）后天空正常，
但周围环境仍像黄昏。复核 `mission_mission0.xml` 的 TimeEnv 结构后确认：
- 用户位置 `(2434,2287)` 落在 `TZ_timezone_B-1`（Salt Desert1）；该区正常 Daylight 的
  `env_color=181,185,202`、`fog_color=118,132,167`。
- 但 `TimeEnvOption name="invade_direct_portal"` 仍带 `time_name="Daylight"`, `time_hour="9"`,
  `zonename=""`；`name="WorldRaid"` 仍带 `time_name="Night"`, `time_hour="6"`, `zonename=""`。
  这两个 cutscene 选项与正常 Daylight/Night 竞争；其 `TimeofDayGroup` 的环境仍是黄昏色
  （invade：`env_color=163,121,107` / `fog_color=126,86,69`；WorldRaid：
  `env_color=114,121,158` / `fog_color=40,49,77`），所以天空虽然已指回 Daylight，
  地面/雾仍呈黄昏。

最终补丁（未覆盖现有客户端文件，仍放在同一目录）：
- 除证据 10 的天空/雾/粒子修正外，再把两个 cutscene TimeEnv 选项改名为
  `invade_direct_portal_disabled` / `WorldRaid_disabled`，`time_name` 改为同名，
  `zonename` 改为 `__disabled__`；对应 `TimeofDayGroup cutscene="..."` 也改为 `*_disabled`。
  这样正常 `TZ_timezone_B-1` 的 `Salt Desert1_Daylight` 环境重新生效。
- 最新 `Level.pak`：9,809,347 bytes，MD5 `f72b44b54f0989b9235519a4f3d21571`；
  最新 `unpacked/mission_mission0.xml`：3,755,293 bytes，MD5 `53839e5962aea7df4e9f6357267dfcd5`。
- 验证重点：进图后执行 `//time day`（9:00），观察天空和地面是否都为晴天；
  若地面仍偏暗，下一步检查客户端是否还被别的 cutscene zone 或全屏 FX 覆盖。

## 追加证据 12（2026-09-20 14:30）：服务端控制边界 —— level 只能由客户端生效

用户追问"既然替换客户端 Level.pak 能修好天空，能否由服务端控制这份 level 生效"。排查结论：**不能**。

- 游戏服务端协议里与客户端环境相关的包只有 `SM_WEATHER`（天气 code）、`SM_GAME_TIME`（时间）、
  `SM_PLAY_MOVIE`（过场），没有关卡资源 / CVar / 天空穹顶通道；Beritra/WorldRaid 只发系统消息并生成特效 NPC；
- 客户端 `Levels/lf2a/Level.pak` 由 CryPak 本地加载（Cry3DEngine `FUN_101214b0` 拼接 `<dir>\level.pak`），
  替换后必须重启客户端进程；
- 实测反证：服务端移除 210060000 天气表 + `gameserver.beritra.enable=false` 后，客户端进图仍自行变红，
  触发源在客户端本地 level 数据。

完整证据链（Cry3DEngine 天气 CVar 清单、`sys_PakPriority` 散文件优先线索、客户端补丁包结构）
见同目录 `server-control-boundary.md`；补丁本体为仓库根 `patch/Levels/lf2a/Level.pak`（说明见 `patch/patch_documentation.md`）。
