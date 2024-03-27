# 一、集成

## 1. 添加SDK

将 dark-sdk-xxx.jar 拷贝至工程libs目录下

## 2. 增加配置

在 build.gradle 中添加如下配置：

```
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    implementation 'androidx.appcompat:appcompat:1.3.1'
}
```

# 二、调用

在 Application 或 MainActivity 的 onCreate 中调用以下语句启动SDK:
```
DarkManager.start(this);
```
