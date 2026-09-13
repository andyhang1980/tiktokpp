# TikTokPP

TikTok 功能增强模块 - 基于 LSPosed/libxposed

## 功能特性

- **地区伪装** - 支持 100+ 国家/地区
- **语言/时区伪装** - 自定义系统语言和时区
- **无损下载** - 移除下载限制，自定义保存路径
- **信息流净化** - 隐藏广告、直播、图文、AI 内容等
- **页面净化** - 隐藏 31 种页面元素
- **播放控制** - 默认播放速度、循环控制、进度条
- **评论翻译** - 自动翻译评论
- **GPS 伪装** - 自定义 GPS 坐标
- **数据过滤** - 按播放量/点赞数过滤内容

## 环境要求

- Android 8.0 (API 26) 及以上
- LSPosed 框架
- TikTok 官方版本 (已测试: 46.7.16)

## 安装

1. 从 Release 页面下载 APK
2. 在 LSPosed 中启用模块
3. 勾选 TikTok 作用域
4. 重启 TikTok

## 构建

```bash
# 设置签名环境变量
export TOKI_KEYSTORE_FILE=path/to/keystore
export TOKI_STORE_PASSWORD=your_password
export TOKI_KEY_ALIAS=your_alias
export TOKI_KEY_PASSWORD=your_key_password

# 构建
./gradlew assembleRelease
```

## 许可证

[MIT License](LICENSE)
