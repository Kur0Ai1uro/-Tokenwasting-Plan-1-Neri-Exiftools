![Neri Exiftools](./pic/未标题-1.png)

# 音理 ExifTools


风又音理主题的 Android 图片元数据查看与编辑器。Kotlin + Jetpack Compose，最低 Android 8.0。

支持 JPEG、PNG、WebP。可以改拍摄时间、机型、描述、版权、方向和 GPS（十进制经纬度），也可以从可写标签里自选字段并赋值。保存前会先备份原图，再尝试覆盖；覆盖不了时可以另存。

## 安装

本地安装包在 `dist/音理ExifTools-1.0.apk`。`dist/` 不会提交到 Git。

## 编译

需要 Android SDK，并在 `local.properties` 里写上 `sdk.dir`。

```bat
gradlew.bat :app:assembleDebug
```

打好的 debug 包会自动放到 `dist/`，文件名跟着版本号走。