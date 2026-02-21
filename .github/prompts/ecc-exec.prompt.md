---
agent: agent
---
严格使用 ECC 执行流程完成任务。开始前必须完成：技能选择、Agent 绑定、命令匹配、规则绑定。

## 强约束执行规则
1. 必须绑定 1 个 Agent（`ecc-migrated/agents/*.md`）。
2. 必须选择 1-2 个技能（`ecc-migrated/skills/*`）。
3. 必须引用规则（至少 3 条 common 规则；若是 TS 项目，补充 typescript 规则）。
4. 若存在匹配命令，必须绑定命令文件（`ecc-migrated/commands/*.md`）。
5. 只允许使用 canonical 技能名，不使用任何 alias 技能名。
6. 需求不明确时只问 1 个澄清问题。

## 固定输出头（必须）
- **已选技能**: `ecc-migrated/skills/<a>`, `ecc-migrated/skills/<b>`
- **绑定 Agent**: `ecc-migrated/agents/<name>.md`
- **引用命令**: `ecc-migrated/commands/<name>.md`（无则填“无”）
- **引用规则**: `ecc-migrated/rules/...`
- **选择原因**: <一句话>

## Agent 选择参考
| 场景 | Agent |
|---|---|
| 构建失败/编译错误 | `build-error-resolver.md` |
| E2E 测试自动化 | `e2e-runner.md` |
| 安全审查/漏洞分析 | `security-reviewer.md` |
| TDD 红绿重构 | `tdd-guide.md` |

## 技能完整映射

### 工程核心
`api-design` | `backend-patterns` | `coding-standards` | `configure-ecc` | `e2e-testing` | `eval-harness` | `frontend-patterns` | `iterative-retrieval` | `postgres-patterns` | `security-review` | `security-scan` | `tdd-workflow` | `verification-loop` | `cost-aware-llm-pipeline` | `continuous-learning-v2`

### 开发与交付
`frontend-design` | `web-artifacts-builder` | `webapp-testing` | `mcp-builder` | `skill-creator` | `skill-share` | `changelog-generator`

### 文档与办公
`doc-coauthoring` | `docx` | `pdf` | `pptx` | `xlsx` | `nutrient-document-processing`

### 业务运营
`content-research-writer` | `lead-research-assistant` | `competitive-ads-extractor` | `developer-growth-analysis` | `meeting-insights-analyzer` | `tailored-resume-generator`

### 创意品牌
`algorithmic-art` | `brand-guidelines` | `canvas-design` | `theme-factory` | `image-enhancer` | `slack-gif-creator`

### 效率工具与连接
`file-organizer` | `invoice-organizer` | `domain-name-brainstormer` | `langsmith-fetch` | `raffle-winner-picker` | `twitter-algorithm-optimizer` | `video-downloader` | `connect-apps`

## 命令映射参考
| 场景 | 命令 |
|---|---|
| E2E 执行 | `e2e.md` |
| 评测/打分 | `eval.md` |
| 方案迭代 | `evolve.md` |
| TDD 循环 | `tdd.md` |
| 覆盖率提升 | `test-coverage.md` |
| 结果核验 | `verify.md` |
| 新技能创建 | `skill-create.md` |
| 经验导入导出 | `instinct-export.md` / `instinct-import.md` / `instinct-status.md` |

## 规则绑定参考

### 公共规则（至少选 3）
`ecc-migrated/rules/common/coding-style.md`
`ecc-migrated/rules/common/security.md`
`ecc-migrated/rules/common/testing.md`
`ecc-migrated/rules/common/git-workflow.md`
`ecc-migrated/rules/common/performance.md`
`ecc-migrated/rules/common/patterns.md`
`ecc-migrated/rules/common/hooks.md`
`ecc-migrated/rules/common/agents.md`

### TypeScript 规则（TS 项目补充）
`ecc-migrated/rules/typescript/coding-style.md`
`ecc-migrated/rules/typescript/security.md`
`ecc-migrated/rules/typescript/testing.md`
`ecc-migrated/rules/typescript/patterns.md`
`ecc-migrated/rules/typescript/hooks.md`

## 执行流程
1. 解析需求并标准化技能名（处理 alias）。
2. 选择 1-2 个技能并绑定 1 个 Agent。
3. 匹配命令文件与规则集。
4. 输出固定头信息。
5. 分步骤执行并在关键节点做验证。

---

我的需求：
{{input}}
