# Emerald RNG Complete

ポケモンエメラルド乱数調整アプリ（全機能実装版）

## 機能一覧

- Method1 / Method2 / Method4
- 高速PID逆算（Lattice風）・IV逆算
- 個体値・性格・色違い・めざめるパワー検索
- 御三家（ポチエナ差・キャリブレーション）
- 孵化・絵画・バトルビデオ・ロアマー・ACE支援
- 固定シンボル・TID/SID・ヒンバス・まぼろしの島 など
- 高精度タイマー

## GitHubでのビルド方法

### 方法1: GitHub Actions（推奨）

1. このリポジトリをGitHubにPush
2. **Actions** タブを開く
3. **Build APK** ワークフローが自動実行される
4. 完了後、Artifacts から `EmeraldRNG-debug` をダウンロード

### 方法2: ローカル（Android Studio）

1. Android Studio でこのフォルダを Open
2. JDK 17 を指定
3. **File → Sync Project with Gradle Files**
4. 初回は Gradle Wrapper が自動生成される場合があります
5. **Build → Build Bundle(s) / APK(s) → Build APK(s)**

### 方法3: コマンドライン

```bash
# Wrapperが無い場合は生成
gradle wrapper --gradle-version 8.7

# ビルド
./gradlew assembleDebug

# APK出力先
# app/build/outputs/apk/debug/app-debug.apk
```

## 必要環境

- Android Studio Hedgehog 以降
- JDK 17
- minSdk 26 / targetSdk 34

## 注意

乱数調整は自己責任で行ってください。
