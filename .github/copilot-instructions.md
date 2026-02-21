````instructions
# Skills 工作区 - Copilot 指令

本工作区已完成技能母库整合：所有技能统一收敛到 `ecc-migrated/skills/`。

## 目录规范（2026-02-21）

- **统一技能目录**：`ecc-migrated/skills/<skill-name>/SKILL.md`
- **统一命令目录**：`ecc-migrated/commands/*.md`
- **统一 Agent 目录**：`ecc-migrated/agents/*.md`
- **统一规则目录**：`ecc-migrated/rules/**`

维护原则：

1. 新增或修改技能，只在 `ecc-migrated/skills/` 下进行。
2. 路由 prompt 只引用 `ecc-migrated/*` 结构。
3. 旧根目录技能已移除，不再维护双路径。

## 路由命令

| 命令 | 用途 |
|------|------|
| `/ecc-auto <需求>` | ECC 范围内自动路由（偏工程化约束） |
| `/ecc-exec <需求>` | 严格模式：绑定 agent + 技能 + 规则 + 可选命令 |

## 技能入口（统一）

可直接引用：

```bash
@workspace 使用 ecc-migrated/skills/frontend-design 技能创建页面
@workspace 使用 ecc-migrated/skills/doc-coauthoring 技能写技术方案
```

常用技能示例（完整列表见目录）：

- `ecc-migrated/skills/doc-coauthoring`
- `ecc-migrated/skills/frontend-design`
- `ecc-migrated/skills/web-artifacts-builder`
- `ecc-migrated/skills/webapp-testing`
- `ecc-migrated/skills/mcp-builder`
- `ecc-migrated/skills/connect-apps`
- `ecc-migrated/skills/docx`
- `ecc-migrated/skills/pdf`
- `ecc-migrated/skills/pptx`
- `ecc-migrated/skills/xlsx`

## 使用建议

1. 需求不确定：优先 `/ecc-auto`。
2. 需要强约束（安全、测试、审计）：优先 `/ecc-exec`。
3. 需求明确：可直接引用具体技能目录。

---

**工作区路径**: C:\Users\25231\.copilot\skills
**更新时间**: 2026-02-21
````
