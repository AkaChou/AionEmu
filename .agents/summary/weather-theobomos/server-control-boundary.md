# 服务端能否控制 level 生效 —— 边界排查（2026-09-20 14:3x）

用户问题：既然替换客户端 `Levels/lf2a/Level.pak` 能修好天空，那么能否由服务端控制这份 level 生效？

## 结论（先说结果）

**不能。** Aion 5.8 游戏协议（7777 端口）里没有任何"关卡资源 / 客户端 CVar / 文件覆盖"通道：

- 服务端只能决定玩家进哪个 `worldId`（传送、进入世界），**不能决定客户端用哪份 level 文件、也不能传输 level 内容**；
- 客户端 `Level.pak`（含 `mission_mission0.xml`）由客户端 CryPak 在**进程启动时挂载**，替换后必须重启客户端进程；
- 已实测反证：服务端移除 `weather_table.xml` 的 210060000 条目、`gameserver.beritra.enable=false` 之后，客户端进图仍会自行变红 —— 触发源在客户端本地数据/逻辑，服务端已无手段。

## 证据 1：服务端 -> 客户端的环境类包清单（子代理全文检索）

`src/main/java/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`：

- `SM_TIME_CHECK` 0x27（时间校验，不影响环境）
- `SM_GAME_TIME` 0x26（游戏时间分钟数；仅时间相关逻辑）
- `SM_FLY_TIME` 0xf6（飞行时间）
- `SM_WEATHER` 0x43（**唯一能驱动客户端天气层的包**）
- `SM_PLAY_MOVIE` 0x69（过场电影，quest/AI/实例触发，非持续环境控制）

Beritra/WorldRaid 全部走 `SM_SYSTEM_MESSAGE` + 特效 NPC 生成/删除
（`BeritraService`、`ai/beritraInvasion/WorldRaid_*AI2`），没有天空/TimeEnv/黑空状态包。

`SM_WEATHER` 包体（`SM_WEATHER.java`）：`unk(0x00) + zoneCount + code[zone]`；
`WeatherService.changeRegionWeather()` 只能改这些 code，**改变不了客户端本地 level 里的
WeatherOption / TimeEnvOption / TimeofDayGroup(cutscene=...) 数据**。

## 证据 2：客户端 level 加载路径（Cry3DEngine.dll 反编译）

- `Cry3DEngine.dll` 字符串 `\level.pak` → `FUN_101214b0`，把目录名（如 `Levels/lf2a`）拼成
  `<dir>\level.pak` 后交给 CryPak 打开。客户端所有关卡资源都在本地 pak 内。
- Ghidra 工程：`/Users/mc/IdeaProjects/58Server/server58/ghidra-projects/CryEngineDll`（本轮新建，
  导入 `bin64/Cry3DEngine.dll`）；分析脚本见本目录 `FindStringRefs.java` / `DecompileFunc.java` / `ReadMemory.java`（运行日志为一次性产物）。

## 证据 3：客户端天气/时间环境 CVar（Cry3DEngine.dll 注册函数 FUN_101ab430）

| CVar | 默认值 | 描述（原字面量） |
|---|---|---|
| `e_weather_enable` | 1.0 | `Aion - 0/1 0-Disable 1-Enable Weather System 적용여부 결정` |
| `e_weather_cutscene` | **-1.0** | `Aion - [-1/0/기상만큼] -1/컷신OFF 0~기상번호만큼 Cutscene Weather를 발효시킨다.` |
| `e_weather_change_immediate` | 0 | `0 - e_weather_interp_speed 로 보간한다 1 - 즉시변환한다` |
| `e_weather_fx_enable` | 1.0 | `Aion - 0/1 0-Disable 1-Enable Weather-FX System Enable` |
| `e_gametime_control_manual` | -1.0 | `-1 - Disable 0~23 - Enable Game Time` |

要点：`e_weather_cutscene` 默认 **-1 = cutscene OFF**，所以"进图变红"在默认 CVar 下**不是**
Cutscene Weather 机制触发的；此外 `e_timeofday` / `e_timeofday_force_apply` 等 CVar 属于
开发者调试开关。

（Game.dll 里 `e_commands` 调试 UI 有 `button_weather_cutscene_on/off`、`button_weather_on/off`
（执行 `weather server 1 1 1` / `0 0 0`），这是客户端本地调试入口，服务端协议不包含这些命令。）

## 证据 4：散文件优先线索（CrySystem.dll）

`sys_PakPriority` 描述：`If set to 1, tells CryPak to try to open the file in pak first, then go to
file system` —— 即**默认（0）时先查文件系统（散文件），后查 pak**。
因此补丁可以优先尝试散文件：`Levels/lf2a/mission_mission0.xml`，无需回封 Level.pak。
（此前诊断中已列为待实测项，用户最终直接用了回封 pak。）

## 可行路径（都不是"服务端控制"）

1. **客户端补丁分发（唯一可靠）**：Level.pak（已验证）或先试散文件 XML；
2. **分发渠道**：手动打包 / `LauncherConfigs/NewVersionCenter.ini` 指向的 HTTP 自动更新服务器；
3. **可选实验**：客户端启动参数 `+e_weather_enable 0`（或 `+e_weather_cutscene -1`）——CryEngine
   命令行 CVar 语法，属客户端侧开关，需实机验证；会全局影响所有地图天气。

## 本轮结论对既有服务端提交的意义

`32b7dba63 fix(weather): stabilize Theobomos clear sky`（移除 210060000 天气表）仍然是必要的：
它阻止服务端把"沙尘天气码"推给客户端（天气层粒子/雾/环境色）。但天空本身（skydome/TimeEnv）
由客户端 level 数据决定，服务端无法替代。
