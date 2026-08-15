# Aion 5.8 中文任务词典补丁

任务对话和任务摘要由客户端本地 `L10N/CHS/Data/data.pak` 渲染，服务端只下发任务 ID、状态和进度变量。任务 `1153` 的服务端交付 NPC 是西亚诺（`203150`），但 5.8 中文客户端的 `STR_DIC_N_Hianu` 丢失了词典显示名和分隔符，导致客户端直接显示异常词典正文。

按文件名大小写无关的方式扫描权威中文包后，共找到 9,116 个非 `unused` 任务 HTML、50,260 次词典引用。原包中有两类缺陷：

- 3 个被任务引用的词典正文缺少 ASCII 分号，客户端无法分离显示名和说明。
- 54 个任务 HTML 词典键不存在，共出现 138 次。其中 33 个是大小写错误，16 个能由同任务步骤和现有词典唯一还原，另有 5 个缺少词典条目但能从 NPC、物品字符串及 ENU 任务文本确定中文显示名。

词典正文缺陷如下：

| 词典条目 | 影响任务 | 修复后的显示名 |
| --- | --- | --- |
| `STR_DIC_N_Hianu` | `1153` | 西亚诺 |
| `STR_DIC_I_quest_23020a` | `23020` | 污染的伊德凝胶箱子碎块 |
| `STR_DIC_I_noblemetal_d_q5303_65a` | `25303`、`25313` | 佩尔农露水 |

任务 HTML 引用修复包括：

| 任务 | 原引用 | 修复依据 |
| --- | --- | --- |
| `1171` | `str_dic_la30` | 同文件其余步骤及唯一词典键 `STR_DIC_LA30`，显示为罗塞诺 |
| `20529` | `STR_DIC_N_DF6_Deser_E` | 服务端目标 NPC `806300` 与同文件摘要均指向 `STR_DIC_N_DF6_Wuste_E` |
| `22558` | `STR_DIC_I_quest_212558a` | 任务号、同组物品及唯一词典键均指向 `STR_DIC_I_quest_22558a` |
| `80189` | `STR_DIC_NPC_event_event_Dirull` | 相邻任务和现有词典均使用 `STR_DIC_N_event_Dirull` |
| `10086`-`10088` | 韩文语法后缀键 | ENU 已使用明文，中文 NPC/词典表可确定对应人名 |
| `9714`、`9715` | 不存在的任务物品词典键 | `client_strings_quest.xml` 中物品显示名均为“美味年糕汤” |

仓库内已经生成可直接覆盖的两个完整字符串表和 55 个受影响任务 HTML：

```text
patch/L10N/CHS/Data/Strings/client_strings_dic_people.xml
patch/L10N/CHS/Data/Strings/client_strings_dic_item.xml
patch/L10N/CHS/Data/Dialogs/**/quest_q*.html
```

将 `patch/L10N/CHS/Data` 中的文件按原目录结构复制到客户端的 `L10N/CHS/Data`。若客户端配置禁用了 loose-file 覆盖，则用补丁文件覆盖完整解包目录中的同名文件，再重新打包 `L10N/CHS/Data/data.pak`。

补丁必须从 SHA-256 为 `ac54a2ee01aef730500513c05755e32d8c2d4ee2eb9443ecb323a47717f99553` 的权威中文 `data.pak` 解包内容生成：

```bash
rtk python3 scripts/generate_client_quest_localization_patch.py \
  --strings-dir /path/to/unpacked/L10N/CHS/Data/Strings
```

生成器会从 `Strings` 的同级 `Dialogs` 目录读取任务 HTML，核对 3 个错误词典正文和 54 个错误引用的原始出现次数。来源内容漂移时会直接失败。补丁叠加后的 9,116 个任务 HTML 已验证不存在缺失词典键，也不存在被引用但缺少显示名分隔符的词典正文。
