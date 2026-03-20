# AI Commit Generator

IntelliJ IDEA 插件，使用 AI 自动生成 Conventional Commits 格式的提交信息。

## 功能

- 在 Commit 面板中一键生成 AI 提交信息
- 支持多种 AI 提供商：Claude、OpenAI、自定义端点（兼容 OpenAI 格式）
- API Key 安全存储（使用 IntelliJ PasswordSafe）
- 可配置 AI 模型和 Prompt 模板
- 自动收集 Git Diff，智能截断超大文件

## 兼容性

- IntelliJ IDEA 2023.3 及更新版本
- 需要 Git 插件

## 安装

### 从 JetBrains Marketplace 安装

1. 打开 IntelliJ IDEA → **Settings → Plugins → Marketplace**
2. 搜索 **"AI Commit Generator"**
3. 点击 **Install**

### 从本地安装

1. 下载 [Releases](https://github.com/q520asdf0123/aicommit/releases) 中的 `.zip` 文件
2. 打开 IntelliJ IDEA → **Settings → Plugins → ⚙️ → Install Plugin from Disk**
3. 选择下载的 `.zip` 文件

## 配置

1. 打开 **Settings → Tools → AI Commit**
2. 选择 AI 提供商（Claude / OpenAI / Custom）
3. 填入 API Key 和模型名称
4. 可选：自定义 Prompt 模板

## 使用

1. 在 Commit 面板中暂存你的变更
2. 点击 **"AI 生成 Commit Message"** 按钮
3. AI 将根据 diff 内容自动生成提交信息

## 构建

```bash
# 需要 JDK 17
./gradlew buildPlugin
```

构建产物位于 `build/distributions/` 目录。

## License

MIT License
