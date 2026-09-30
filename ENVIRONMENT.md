# External data roots / 外部数据根目录

部分工具与审计脚本需要读取**仓库之外**的数据（真端服务端表、客户端解包产物、客户端安装目录）。
这些位置因机器而异，因此统一通过环境变量注入，**禁止把开发者本机绝对路径写入代码或文档**。

| 环境变量 | 指向 | 典型 HOME 相对位置 | 用途 |
|---|---|---|---|
| `AION_REPO_ROOT` | 本仓库根目录 | `~/IdeaProjects/AionEmu-test` | 需要绝对仓库路径的脚本、文档命令 |
| `AION_RETAIL_ROOT` | 真端 5.8 服务端根 | `~/IdeaProjects/58Server` | `Map/XML/Quest_*.xml`、`Map/Worlds/`、`MainServer/ScriptDLL64.dll`、反编译源码 |
| `AION_CLIENT_ROOT` | Aion 5.8 客户端安装目录 | `~/IdeaProjects/5.8客户端` | `L10N/CHS/Data/data.pak`、`data/Dialogs/Dialogs.pak`、`bin64/Game.dll` |
| `AION_UNPACK_ROOT` | 客户端解包产物根 | `~/PycharmProjects/unpak` | `Quest_unpacked/quest.xml`、`quest_monster.csv`、`data_unpacked/Dialogs/**`、`Strings/*`、`npcs_unpacked/*` |

## 用法 / Usage

脚本默认回退到上表的 HOME 相对位置，因此同一台机器上通常无需显式设置：

```bash
export AION_RETAIL_ROOT="$HOME/IdeaProjects/58Server"
export AION_CLIENT_ROOT="$HOME/IdeaProjects/5.8客户端"
export AION_UNPACK_ROOT="$HOME/PycharmProjects/unpak"
```

约定 / Conventions:

- 仓库内路径写成相对路径（如 `.agents/summary/foo.py`、`src/main/resources/...`），不要写成绝对路径。
- 文档中的命令若必须引用外部根，使用 `${AION_RETAIL_ROOT}` 这类变量写法，不要写死本机目录。
- Python 脚本使用 `os.environ.get("<VAR>", os.path.expanduser("~/..."))`；shell 脚本使用 `"${VAR:-$HOME/...}"`。
- Java 工具链路径统一使用 `$JAVA_HOME`（例如 `$JAVA_HOME/bin/jfr`），不要写具体 JDK 发行版安装路径。
- 详见 [AGENTS.md](AGENTS.md) 仓库约定与 Git 历史中的同类清理提交。

## 相关 / See also

- 客户端对话映射与生成脚本的输入约定：`docs/quest/client-dialog-mapping/README.zh-CN.md`
- 任务修复 Playbook：`docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md`
