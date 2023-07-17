package dev.redicloud.utils.gson

import com.google.gson.GsonBuilder

val gsonInterfaceFactory = InterfaceTypeAdapterFactory()
val gson = GsonBuilder()
    .addInterfaceImpl(gsonInterfaceFactory)
    .fixKotlinAnnotations()
    .serializeNulls()
    .setPrettyPrinting()
    .create()