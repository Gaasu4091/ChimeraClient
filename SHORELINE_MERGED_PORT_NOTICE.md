# Shoreline merged → Chimera 1.21.11

Input: `shoreline-1.0-pistoncrystal-merged-2b2tmode.jar` (Minecraft 1.21.1).
Target: this conversation's latest `Chimeraclient-2bPiston-GeometryFix-1.21.11.jar`.

The supplied PistonCrystal replaces 2bPiston; the supplied AutoCrystal replaces CrystalAura.
The original, separate Chimera PistonCrystal, GUI, font and other modules remain unchanged.
Settings and combat algorithms are retained from the supplied implementations. Minecraft API,
event dispatch, configuration, rendering and framework adapters target Chimera/Fabric 1.21.11.

This is a source patch kit against the included binary base, not the original complete upstream
Chimera Gradle project. `src/` contains the two transplanted modules and their support sources.
Compilation and offline checks do not establish multiplayer server compatibility; no live server
or Minecraft client is launched by these checks. Existing saved settings are not overwritten.

## 日本語・利用上の注意

- `2bPiston` は今回の添付版 PistonCrystal に置き換えました。以前の Homovore 版は使用しません。
- `CrystalAura` は今回の添付版 AutoCrystal です。`PlaceLimit` と Sequential の制限・再開も含みます。
- 元から存在する別モジュール `PistonCrystal` は変更していません。
- 元の設定値を維持しています。2bPiston の `SelfGround` / `MovingPause` は初期値ONのため、空中移動中・移動中は停止します。`AirPlace` / `BasePlace` は初期値ON、`2b2t-Mode` は初期値OFFです。
- 既存の保存設定は優先されます。入力JARと同じ初期値で比較したい場合は、GUI内で値を確認してください。ユーザーの設定ファイルは変更していません。
- 同じmod IDを持つ旧Chimeraと同時導入しないでください。この作業ではmodsフォルダーへの自動配置はしていません。

## 互換処理

1. 設定をChimeraのSetting、PRE tick・受信・描画をChimeraのイベントに接続。
2. 元のRotationManagerを共有。移動パケットへの視点反映と即時Silent回転を1.21.11へ適合。
3. 通常のinteractBlock経由でもサーバー側の選択スロットを参照するMixinを移植。設置コンテキスト、packet sneak、quiet送信にも対応。
4. 1.21.11のPlayerInput、装備・ツールのタグ、描画API、パケット形式へ適合。
5. 元のAutoMineの「実際に持ち替え中」をChimera SpeedMineへ接続。採掘タスクが存在するだけでCrystalAuraを停止していた旧アダプターを修正。
6. CFRで崩れたローカル変数は元バイトコードを確認して復元。攻撃・配置パターンは独自変更していません。

Shoreline全体の別モジュールは追加していません。Chimeraに存在しない全体設定は元の初期値を使用
（Anticheat=VANILLA、全体AirPlace=OFF、ダメージ文字のNametags縮尺=0.003）。
2bPiston自身のAirPlaceは独立して動作します。

## 再ビルド

SRC ZIPを展開したルートで `zsh work/shoreline-merged-port/build-local.sh`。
Java 21以上、zsh、既存のGradle/Loomキャッシュが必要です。この端末で使用・検証した依存は
Minecraft 1.21.11 / Yarn 1.21.11+build.6 / Fabric Loader 0.18.4 / Fabric API 0.141.6+1.21.11 /
TinyRemapper 0.14.1です。`GRADLE_USER_HOME`指定に対応しています。
オリジナルChimeraの全ソースを復元したものではなく、変更ソース44ファイル・ベースJAR・再ビルド／検証ツールを含むパッチ形式です。

## 入力の識別（SHA-256）

- 新添付元: `5e2d91b67c587a39a304d46f1b5011b08803d647717cd8541ce48c5e97194d48`
- 最新Chimeraベース: `4951a111b1f60f2c6fcd0e17c2741cd508b13852fbf9ce2a7084f615fa82e293`
