# External data roots / 外部数据根

部分工具与审计脚本需要读取**仓库之外**的数据（真端服务端表、客户端安装目录、客户端解包产物）。
这些位置因机器而异，因此仓库内一律用**名称**指代，不写本机绝对路径，也不需要预先定义环境变量。

| 名称 | 指向 |
|---|---|
| `<仓库根>` | 本仓库根目录 |
| `<真端根>` | 真端 5.8 服务端根：`Map/XML/Quest_*.xml`、`Map/Worlds/`、`MainServer/ScriptDLL64.dll`、`server58/` 反编译源码 |
| `<客户端目录>` | Aion 5.8 客户端安装目录：`L10N/CHS/Data/data.pak`、`data/Quest/Quest.pak`、`bin32|bin64/*.dll` |
| `<客户端解包根>` | 客户端解包产物根：`Quest_unpacked/quest.xml`、`quest_monster.csv`、`data_unpacked/Dialogs/**`、`Strings/*`、`npcs_unpacked/*`、`aion_pak.py` |

## 用法 / Usage

`.agents/**` 下的一次性脚本按**同宿主目录约定**解析外部根：由 `__file__` 推导 `<仓库根>`，
再取同级目录，例如 `<仓库根>/../58Server`、`<仓库根>/../5.8客户端`、
`<仓库根>/../PycharmProjects/unpak`。因此在标准布局下无需任何环境变量即可运行：

```text
<workspace>/AionEmu-test      <- <仓库根>
<workspace>/58Server          <- <真端根>
<workspace>/5.8客户端          <- <客户端目录>
<workspace>/PycharmProjects/unpak  <- <客户端解包根>
```

约定 / Conventions:

- 仓库内路径写成相对路径（如 `src/main/resources/...`、`.agents/summary/foo.py`），不要写绝对路径。
- 文档中引用外部数据根时写名称（`<真端根>/Map/XML/Quest_SimpleHunt.xml`），不要写本机目录，也不要写 `${AION_*}` 形式。
- Python 脚本从 `__file__` 向上查找含 `pom.xml` 的目录推导 `<仓库根>`（`REPO`），再按同宿主目录约定取外部根，不要引入项目专属环境变量。
- Java 工具链路径统一使用 `$JAVA_HOME`（例如 `$JAVA_HOME/bin/jfr`），不要写具体 JDK 发行版安装路径。
- 同一开发者机器的实际路径映射由各开发者在本地自行维护（本地笔记 / AI 助手记忆），**不入仓**。

## 相关 / See also

- 客户端对话映射与生成脚本的输入约定：`docs/quest/client-dialog-mapping/README.zh-CN.md`
- 任务修复 Playbook：`docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md`
