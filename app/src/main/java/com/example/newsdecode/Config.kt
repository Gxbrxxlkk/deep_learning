package com.example.newsdecode

/**
 * Ponto central de acesso às chaves de API do app.
 *
 * As chaves são lidas do BuildConfig, que as injeta em tempo de compilação
 * a partir do arquivo local.properties (ignorado pelo Git).
 * Nunca coloque chaves diretamente aqui.
 */
object Config {
    val NEWSDATA_API_KEY: String get() = BuildConfig.NEWSDATA_API_KEY
    val GEMINI_API_KEY: String get() = BuildConfig.GEMINI_API_KEY
}
