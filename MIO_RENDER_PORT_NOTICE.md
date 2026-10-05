# ChimeraClient Mio Render / HUD port — Minecraft 1.21.11

## 入力・範囲

移植先は、添付 `Chimeraclient-ShorelineMerged-1.21.11.jar` です。
移植元は、添付 `OpenMio-main.zip` の Java ソース、`nick` の設定定義、画像、音声、GLSL です。
Mio の認証・ネイティブ Loader を実行する実装にはしていません。
Minecraft 1.21.1 の呼び出しを 1.21.11 の描画コマンドキュー、RenderPipeline、環境属性、入力 API に接続しています。

追加した Render 35項目:

Ambience, Animations, Blur, Borders, BreakHighlight, Chams, Crosshair, ESP, FreeLook,
Glint, Highlight, Hitmarker, HoleESP, LogoutSpots, Markers, NameTags, NoBob, NoRender,
Particles, PhaseESP, Search, Shader, Skeleton, SkyColor, Tooltips, Tracers, Trails,
Trajectories, Tunnels, ViewClip, ViewModel, VoidESP, Waypoints, Xray, Zoom.

追加した HUD 18項目:

Armor, Chat, Crypto, Direction, Effects, EntityList, Graph, Inventory, Lag'O'Meter,
Map, Metrics, ModuleList, Music, PlayerModel, Position, TextRadar, Totems, Welcomer.

除外したのは **Mio の Watermark だけ** です。Welcomer は含めています。
元の Chimera の Watermark / Coordinates、BlockHighlight / KillEffect などは削除していません。
既存の戦闘モジュール、ClickGUI、Font、同梱ライブラリのクラスは変更していません。
元の ModuleManager には最後の登録後に `MioRenderModules.register(this)` を1回追加しただけです。
`fabric.mod.json` の変更は新規 Mixin 設定と Mio の GPL ライセンスの追加だけです。

## 使用方法

Fabric / Java 21 / Minecraft 1.21.11 向けです。元の Chimera JAR と同時に入れないでください。
この成果物は元の JAR を含む置き換え用です。ゲームの mods、設定、ワールドへの自動インストールは行っていません。

ClickGUI の Render タブに35項目を追加しています。既存の右クリック設定とカラーピッカーを使います。
Client タブの `HUD` に共通の色・余白・アイコン位置・SmoothWidth と HudEditor の入口があります。
個々の HUD の ON/OFF、位置、設定は既存の HudEditor で操作します。
HUD は必要なものを有効にし、ドラッグしてください。Anchor を NONE にすると手動配置です。
Mio の折りたたみ用 Boolean は設定表示を開閉するものであり、配下の機能全体のマスタースイッチではありません。

レジストリ一覧は Chimera の String 設定に合わせています。
例: `minecraft:player,minecraft:zombie` / `minecraft:diamond_ore,minecraft:deepslate_diamond_ore`。
既定の値・範囲・列挙値465件は `assets/opensrc/mio/settings.json` に保存しています。
同じ表示名の設定は保存時の衝突を避けるため一部に元フィールド名を併記しています。
Markers の Radius=45 / OnlyOffscreen=false は、添付の設定定義に該当2件が欠けていたため追加した互換設定です。

Waypoints は Chimera のコマンドプレフィックスを使う `wp` / `waypoints` コマンドを追加しています。
例: `wp add base` / `wp add base 100 64 200 overworld` / `wp list`。
保存先は通常のゲームディレクトリ内 `opensrc/waypoints.json` です。
Tooltips のコンテナ閲覧は読み取り専用です。Alt または中クリックでプレビューを開きます。

## 互換処理・既知の制限

- Chams / LogoutSpots はバウンディングボックスだけの代用ではなく、1.21.11 の実モデルで塗り・線・ゴーストを描きます。
  Shader は元の Solid / Rainbow / Gradient / Bloom を新 API のマスクとポスト処理に接続しています。
  レンダラーの構造変更のため、1.21.1 とピクセル単位で同一の表示を保証するものではありません。
- Mio の Friend 判定は Chimera の friendManager に接続しました。
  Chimera に存在しない Mio の Enemy リストやアカウント照合 (MioCheck) は取得できません。
  対応設定があっても特別な Enemy 色 / Mio ユーザー認証表示は再現しません。
- Welcomer の NAME / CUSTOM は動作する実装です。UID は添付ソースの既定値 `-1` の表示です。
  Mio の認証アカウント番号を取得する実装は添付にありません。
- Music は項目を含めていますが、添付 `nick.Loader.setup()` は状態5と空文字を代入するスタブです。
  現在再生中の曲を取得する提供元実装がなく、実曲表示は未実装です。元と同様 Windows 限定の扱いで、
  macOS では有効化されません。HudEditor のサンプル表示は実曲取得ではありません。
- HUD Map は受信したチャンクを表示します。新旧チャンクの識別は Mio の Render/HUD 外の NewChunks モジュールに依存するため、
  この移植では読み込み済みチャンクの表示であり、新旧分類色は再現していません。
- Highlight の Unreachable は Mio の別カテゴリの Reach に依存するため、その追加リーチ判定は移植対象外です。
- Crypto は有効化した場合のみ元と同じ外部価格 API へ非同期アクセスします。
  ネットワーク障害時は UNKNOWN です。価格 API の実接続は今回の検査に含めていません。
- 元の独自 Font は移植せず、表示は既存 Chimera の FontDraw / Vanilla font に接続しています。
  個別 GUI 設定の公開・一覧編集は既存 Chimera UI に合わせたもので、Mio の一覧選択ダイアログそのものではありません。

## 検査と未検証の範囲

- Java 21 向けコンパイルと named→intermediary の Mixin 対応リマップ。
- Minecraft メンバー参照、Mixin 対象、注入箇所、Accessor / Invoker の静的検査。
- 本物の Fabric Loader / Mixin で対象 Minecraft クラスへ変換を適用し、移植クラスを JVM にリンクするオフライン検査。
  Minecraft のゲーム起動やワールドロードとは別の検査です。
- Render 35 / HUD 18 の生成、設定所有者・一意性・保存用 JSON・465件の既定値・ブラックリスト動作。
- Apple M4 の実 OpenGL で4種類の GLSL をコンパイル・リンク・描画。
  通常の中心アルファ127～128、背景0、NoCluster の中心アルファ32を確認。
  モデル用・Glint 用シェーダーも Vanilla 頂点シェーダーとのリンク検査を実施。
- 元 JAR の全エントリ比較。ModuleManager の登録追加と Mixin / license メタデータ以外の既存ファイルの改変を禁止。

**Minecraft を起動しての目視・サーバー上でのプレイ検証は未実施です。**
他の描画 Mod との競合、GUI スケールごとの位置、全項目の組み合わせ、負荷は実機プレイで追加確認が必要です。
この説明の制限項目を含むため、完全な Mio クライアントの複製という意味ではありません。

## SRC と再ビルド

SRC は新規移植コードの全 Java ソース、リソース、設定目録、再ビルドツール、入力 Chimera JAR を含むオーバーレイ開発キットです。
添付の Chimera は JAR だけだったため、Chimera 全体のオリジナルソース一式ではありません。
既存バイナリを保存して追加コードのみを重ねる方式です。Mio 原文は別途添付 `OpenMio-main.zip` を参照してください。

必要環境: JDK 21以上の javac/java、zsh、Yarn 1.21.11+build.6、TinyRemapper 0.14.1 (MixinExtension 付き)、ASM、Gson、
Fabric / Mixin / Minecraft のコンパイル依存。ローカル Gradle / Loom キャッシュから既定で解決します。
キャッシュがない環境では次の変数に正確なパスを指定してください (自動ダウンロードはしません):

```
export NAMED_MC=/path/to/minecraft-merged-named-1.21.11.jar
export INTER_MC=/path/to/minecraft-merged-intermediary-1.21.11.jar
export MAPPINGS=/path/to/yarn-1.21.11-build.6-mappings.tiny
export TR=/path/to/tiny-remapper-0.14.1.jar
export DEPS='/path/to/gson.jar:/path/to/asm.jar:...'
zsh build.sh
```

`BASE_JAR` / `OUT_JAR` も上書き指定できます。開発キットでは `base/` の入力を使います。
出力は `Chimeraclient-MioRender-candidate-1.21.11.jar`。
ビルド時に参照検査と元 JAR の保存検査を必ず実行します。
`SmokeRender.java` / `VerifyShaders.java` / `AuditMixins.java` は追加の検査ツールです。

## ライセンス

Mio の添付 LICENSE は GNU GPL v3 です。本文を `LICENSE_Mio_GPL-3.0` に同梱しました。
元 Chimera JAR 内の MIT / AGPL 表示と既存のライセンス本文・告知はそのまま残しています。
Mio からの移植部分は GPL v3 としてソースを添付しています。
