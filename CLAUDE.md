# CLAUDE.md

## プロジェクト概要

ブレハン — Android端末内の写真のシャープネスを判定するアプリ。OpenCVのLaplacianフィルタで各写真にスコア（0〜100）を付与し、ぼけている写真を見つけやすくする。

## ビルド・実行

```bash
# デバッグビルド
./gradlew assembleDebug

# テスト
./gradlew test              # ユニットテスト
./gradlew connectedCheck    # インストルメンテーションテスト

# Lint
./gradlew lint
```

## アーキテクチャ

- パッケージ: `com.warabi1062.burehan`
- 言語: Kotlin
- UI: Jetpack Compose + Material3
- minSdk: 34, targetSdk: 36
- ビルドシステム: Gradle (Version Catalog `gradle/libs.versions.toml`)

### 主要ライブラリ

- OpenCV Android SDK — シャープネス判定（Laplacianフィルタ）
- MediaStore API — 端末写真の取得
- Kotlin Coroutines — バックグラウンドスキャン

### シャープネス判定フロー

1. 画像を長辺800pxにリサイズ
2. グレースケール変換
3. Laplacianフィルタ適用
4. 分散値（variance）算出
5. シグモイド関数で0〜100にマッピング

## コーディング規約

- Kotlin公式スタイルに準拠
- UIはJetpack Composeで構築（XMLレイアウト不使用）
- 非同期処理はCoroutinesを使用（RxJava不使用）
- 削除機能は実装しない（閲覧・スコア確認のみ）

## パーミッション

- `READ_MEDIA_IMAGES`（API 33+）
- `READ_EXTERNAL_STORAGE`（API 32以下、ただしminSdk=34のため実質不要）
