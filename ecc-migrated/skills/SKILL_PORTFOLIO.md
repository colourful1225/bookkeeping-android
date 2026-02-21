# ECC Skills Portfolio (Canonical / Deprecated)

## 命名规范（统一）

1. 目录名统一使用 `kebab-case`：`ecc-migrated/skills/<skill-name>/`
2. 每个技能必须包含 `SKILL.md`
3. 能力重叠时保留 1 个 canonical，其他改为 deprecated alias
4. Prompt 与文档只引用 canonical 名称

## Canonical（保留）

### 工程核心
- `api-design`
- `backend-patterns`
- `coding-standards`
- `configure-ecc`
- `e2e-testing`
- `eval-harness`
- `frontend-patterns`
- `iterative-retrieval`
- `postgres-patterns`
- `security-review`
- `security-scan`
- `tdd-workflow`
- `verification-loop`
- `cost-aware-llm-pipeline`
- `continuous-learning-v2`

### 通用开发与交付
- `frontend-design`
- `web-artifacts-builder`
- `webapp-testing`
- `mcp-builder`
- `skill-creator`
- `skill-share`
- `changelog-generator`

### 文档与办公自动化
- `doc-coauthoring`
- `docx`
- `pdf`
- `pptx`
- `xlsx`
- `nutrient-document-processing`

### 业务与运营
- `content-research-writer`
- `lead-research-assistant`
- `competitive-ads-extractor`
- `developer-growth-analysis`
- `meeting-insights-analyzer`
- `tailored-resume-generator`

### 创意与品牌
- `algorithmic-art`
- `brand-guidelines`
- `canvas-design`
- `theme-factory`
- `image-enhancer`
- `slack-gif-creator`

### 效率工具
- `file-organizer`
- `invoice-organizer`
- `domain-name-brainstormer`
- `langsmith-fetch`
- `raffle-winner-picker`
- `twitter-algorithm-optimizer`
- `video-downloader`

### 连接与集成
- `connect-apps`

## Deprecated（已移除）

已执行硬删除，不再保留兼容入口，技能目录仅保留 canonical 集合。

## 合并策略

1. 新增技能前先检查 `SKILL_PORTFOLIO.md`，避免重复能力。
2. 若发现重复能力：
   - 保留使用量高、文档完整、脚本齐全的 canonical
   - 重复目录直接删除，不再保留 alias
3. 每次调整后，同步更新：
   - `.github/prompts/ecc-auto.prompt.md`
   - `.github/prompts/ecc-exec.prompt.md`
   - `TECHNICAL_DOCUMENTATION_SKILL_ROUTING.md`
