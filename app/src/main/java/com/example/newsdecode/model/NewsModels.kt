package com.example.newsdecode.model

import com.google.gson.annotations.SerializedName

/**
 * Resposta raiz da NewsData.io.
 * O Gson usa @SerializedName para mapear campos da API cujos nomes
 * diferem das propriedades Kotlin.
 */
data class NewsResponse(
    val status: String,
    @SerializedName("totalResults") val totalResults: Int?,

    // A API chama de "results", mas usamos "articles" internamente
    @SerializedName("results") val articles: List<Article>?,

    // Token opaco retornado pela API para buscar a próxima página (paginação)
    @SerializedName("nextPage") val nextPage: String?
)

/**
 * Representa um artigo de notícia retornado pela NewsData.io.
 */
data class Article(
    @SerializedName("source_id")   val sourceId: String?,
    @SerializedName("source_name") val sourceName: String?,

    val title: String,
    val description: String?,

    // Conteúdo completo do artigo — a NewsData.io não trunca no plano gratuito,
    // o que melhora significativamente a qualidade das explicações do Gemini
    val content: String?,

    @SerializedName("link")      val url: String?,
    @SerializedName("image_url") val urlToImage: String?,
    @SerializedName("pubDate")   val publishedAt: String?,

    val creator: List<String>?,
    val category: List<String>?
) {
    /**
     * Propriedade computada que monta um objeto Source compatível com o Adapter.
     * Não armazena nada — é calculada toda vez que acessada.
     */
    val source: Source get() = Source(sourceId, sourceName ?: sourceId ?: "Desconhecido")
}

data class Source(val id: String?, val name: String)
