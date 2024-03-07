-ignorewarnings

-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature

#混淆后保留类名和行号
-keepattributes SourceFile,LineNumberTable

# 实现了Serializable接口的实体类不混淆
-keep class * implements java.io.Serializable {*;}

# 保留包含native方法的类
-keepclasseswithmembernames class * {
    native <methods>;
}

# for android support & androidx
-keep class android.support.** {*;}
-keep class * extends android.support.** {*;}
-keep class androidx.** {*;}
-keep class * extends androidx.** {*;}
