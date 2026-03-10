package com.example.newsdecode.api

import com.example.newsdecode.model.NewsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface Retrofit que define os endpoints da NewsData.io.
 * O Retrofit transforma cada função anotada em uma requisição HTTP real.
 * Cada @Query vira um parâmetro na URL: ?language=pt,en&category=politics...
 */
interface NewsApiService {

    /**
     * Busca as últimas notícias filtradas por idioma e categoria.
     * Usado para o feed principal do app.
     * @param page Token de paginação retornado pela resposta anterior (null = primeira página)
     */
    @GET("api/1/latest")
    suspend fun getTopHeadlines(
        @Query("language") language: String = "pt,en",
        @Query("category") category: String = "politics,business,technology",
        @Query("page")     page: String? = null,
        @Query("apikey")   apiKey: String
    ): Response<NewsResponse>

    /**
     * Busca notícias por palavra-chave no corpo do artigo.
     * Exige mínimo de 4 caracteres no parâmetro q (limitação da NewsData.io).
     */
    @GET("api/1/latest")
    suspend fun searchNews(
        @Query("q")      query: String,
        @Query("page")   page: String? = null,
        @Query("apikey") apiKey: String
    ): Response<NewsResponse>

    /**
     * Busca notícias por palavra-chave somente no título.
     * Usado como fallback para termos curtos (ex: "Irã", "Cão") que o
     * parâmetro q rejeita, pois qInTitle aceita menos caracteres.
     */
    @GET("api/1/latest")
    suspend fun searchNewsByTitle(
        @Query("qInTitle") query: String,
        @Query("page")     page: String? = null,
        @Query("apikey")   apiKey: String
    ): Response<NewsResponse>
}
