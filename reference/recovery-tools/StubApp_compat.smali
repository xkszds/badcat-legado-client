.class public final Lcom/stub/StubApp;
.super Ljava/lang/Object;
.source "StubApp.java"

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

# Compatibility shim for the recovered, authorized app build.
# It keeps the method signatures still invoked by the recovered business DEXes,
# without running the original 360 shell initialization or loading libjiagu.

.method public static getOrigApplicationContext(Landroid/content/Context;)Landroid/content/Context;
    .locals 0
    return-object p0
.end method

.method public static interface11(I)V
    .locals 0
    return-void
.end method

.method public static interface22(I[Ljava/lang/String;[I)V
    .locals 0
    return-void
.end method

.method public static mark()V
    .locals 0
    return-void
.end method