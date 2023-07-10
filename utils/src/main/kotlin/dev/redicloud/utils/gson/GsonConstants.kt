package dev.redicloud.utils.gson

import com.google.gson.GsonBuilder

val gson = GsonBuilder()
    .addInterfaceImpl()
    .fixKotlinAnnotations()
    .serializeNulls()
    .create()