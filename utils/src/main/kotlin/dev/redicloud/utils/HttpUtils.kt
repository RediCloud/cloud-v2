package dev.redicloud.utils

import io.ktor.client.*
import io.ktor.client.engine.cio.*


val httpClient = HttpClient(CIO)