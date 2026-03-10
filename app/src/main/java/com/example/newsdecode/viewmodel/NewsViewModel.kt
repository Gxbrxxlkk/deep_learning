package com.example.newsdecode.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsdecode.Config
import com.example.newsdecode.api.NewsApiService
import com.example.newsdecode.model.Article
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * ViewModel compartilhado entre FirstFragment e SecondFragment.
 *
 * Responsabilidades:
 * - Buscar notícias via NewsData.io (Retrofit)
 * - Gerenciar paginação e estado do feed
 * - Gerar explicações via Google Gemini (LLM)
 *
 * O ViewModel sobrevive a mudanças de configuração (ex: rotação de tela),
 * garantindo que dados e estados não sejam perdidos.
 */
class NewsViewModel : ViewModel() {

    /**
     * Categorias disponíveis para filtrar o feed.
     * apiValue é o valor aceito pela NewsData.io; null = usa as categorias padrão do app.
     */
    enum class Category(val label: String, val apiValue: String?) {
        ALL("Tudo",                   null),
        POLITICS("Política",          "politics"),
        BUSINESS("Economia",          "business"),
        TECHNOLOGY("Tecnologia",      "technology"),
        SCIENCE("Ciência",            "science"),
        SPORTS("Esportes",            "sports"),
        ENTERTAINMENT("Entretenimento", "entertainment")
    }

    // --- LiveData exposta aos Fragments ---
    // MutableLiveData é privado (só o ViewModel modifica); LiveData é público (só leitura)

    private val _articles = MutableLiveData<List<Article>>()
    val articles: LiveData<List<Article>> = _articles

    private val _explanation = MutableLiveData<String>()
    val explanation: LiveData<String> = _explanation

    // Loading da primeira página / explicação do Gemini
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Loading exclusivo do scroll infinito (rodapé), sem cobrir a tela toda
    private val _loadingMore = MutableLiveData<Boolean>(false)
    val loadingMore: LiveData<Boolean> = _loadingMore

    // Artigo selecionado, passado para o SecondFragment exibir título e fonte
    private val _currentArticle = MutableLiveData<Article>()
    val currentArticle: LiveData<Article> = _currentArticle

    // --- Estado interno de paginação ---
    private var nextPageToken: String? = null   // Token da próxima página retornado pela API
    private var hasMorePages: Boolean = true    // false quando a API não retorna nextPage
    private var currentCategory: Category = Category.ALL
    private var currentSearchQuery: String? = null
    private var isSearchMode: Boolean = false   // true = está mostrando resultado de busca
    private var isShortQuery: Boolean = false   // true = busca por qInTitle (termo < 4 chars)

    // --- Clientes de API ---

    // Retrofit: transforma a interface NewsApiService em chamadas HTTP reais
    private val newsApi: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://newsdata.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }

    /**
     * Modelo Gemini 2.5 Flash configurado para sumarização de notícias.
     *
     * temperature = 0.2f → respostas mais factuais e consistentes.
     * Escala: 0.0 (totalmente determinístico) a 2.0 (muito criativo).
     * Para jornalismo, valores baixos são preferíveis para evitar "alucinações"
     * (o modelo inventar informações com confiança).
     */
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = Config.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.2f
        }
    )

    /**
     * Carrega o feed principal por categoria (sempre da primeira página).
     * Reinicia o estado de paginação a cada chamada.
     */
    fun fetchNews(category: Category = currentCategory) {
        currentCategory = category
        currentSearchQuery = null
        isSearchMode = false
        nextPageToken = null
        hasMorePages = true

        viewModelScope.launch {
            _loading.value = true
            try {
                val categoryParam = if (category == Category.ALL) {
                    "politics,business,technology" // padrão para "Tudo"
                } else {
                    category.apiValue!!
                }

                val response = newsApi.getTopHeadlines(
                    category = categoryParam,
                    apiKey = Config.NEWSDATA_API_KEY
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    nextPageToken = body?.nextPage
                    hasMorePages = nextPageToken != null
                    _articles.value = body?.articles
                        ?.filter { !it.title.isNullOrBlank() && it.title != "[Removed]" }
                        ?.distinctBy { it.url ?: it.title } // remove duplicatas por URL
                        ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Busca notícias por palavra-chave.
     * @param shortQuery true para termos < 4 chars: usa qInTitle em vez de q
     */
    fun searchNews(query: String, shortQuery: Boolean = false) {
        currentSearchQuery = query
        isSearchMode = true
        isShortQuery = shortQuery
        nextPageToken = null
        hasMorePages = true

        viewModelScope.launch {
            _loading.value = true
            try {
                val response = if (shortQuery) {
                    newsApi.searchNewsByTitle(query = query, apiKey = Config.NEWSDATA_API_KEY)
                } else {
                    newsApi.searchNews(query = query, apiKey = Config.NEWSDATA_API_KEY)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    nextPageToken = body?.nextPage
                    hasMorePages = nextPageToken != null
                    _articles.value = body?.articles
                        ?.filter { !it.title.isNullOrBlank() && it.title != "[Removed]" }
                        ?.distinctBy { it.url ?: it.title }
                        ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Carrega a próxima página e ANEXA os artigos ao feed existente (scroll infinito).
     * Ignora a chamada se já estiver carregando ou não houver mais páginas.
     */
    fun loadNextPage() {
        if (!hasMorePages || _loadingMore.value == true || _loading.value == true) return

        viewModelScope.launch {
            _loadingMore.value = true
            try {
                val response = if (isSearchMode) {
                    if (isShortQuery) {
                        newsApi.searchNewsByTitle(
                            query = currentSearchQuery ?: return@launch,
                            page = nextPageToken,
                            apiKey = Config.NEWSDATA_API_KEY
                        )
                    } else {
                        newsApi.searchNews(
                            query = currentSearchQuery ?: return@launch,
                            page = nextPageToken,
                            apiKey = Config.NEWSDATA_API_KEY
                        )
                    }
                } else {
                    val categoryParam = if (currentCategory == Category.ALL) {
                        "politics,business,technology"
                    } else {
                        currentCategory.apiValue!!
                    }
                    newsApi.getTopHeadlines(
                        category = categoryParam,
                        page = nextPageToken,
                        apiKey = Config.NEWSDATA_API_KEY
                    )
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    nextPageToken = body?.nextPage
                    hasMorePages = nextPageToken != null

                    val newArticles = body?.articles
                        ?.filter { !it.title.isNullOrBlank() && it.title != "[Removed]" }
                        ?: emptyList()

                    // Filtra artigos que já existem no feed antes de anexar
                    val current = _articles.value ?: emptyList()
                    val existingUrls = current.map { it.url ?: it.title }.toSet()
                    val unique = newArticles.filter { (it.url ?: it.title) !in existingUrls }
                    _articles.value = current + unique
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loadingMore.value = false
            }
        }
    }

    /**
     * Envia o artigo para o Gemini e armazena a explicação em [explanation].
     *
     * O prompt usa técnicas de engenharia de prompt:
     * - Role prompting: "você é um jornalista..." define estilo e vocabulário
     * - Instrução negativa: proíbe saudações que o modelo geraria por padrão
     * - Output estruturado: seções fixas com emojis garantem consistência na UI
     * - Prompt condicional: seção de termos técnicos só aparece se o conteúdo
     *   for longo o suficiente para justificá-la
     */
    fun explainArticle(article: Article) {
        _currentArticle.value = article
        viewModelScope.launch {
            _loading.value = true
            _explanation.value = ""
            try {
                val content = article.content?.trim()?.takeIf { it.isNotBlank() }
                    ?: article.description?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Não disponível"

                // Inclui a seção de termos técnicos apenas para artigos com conteúdo substancial
                val termosSection = if (content.length > 300) """

                    📚 ENTENDA OS TERMOS
                    Explique apenas termos técnicos, siglas ou conceitos que realmente apareçam
                    no texto e que um leitor leigo possa não conhecer.
                    Se não houver nenhum, omita esta seção completamente.
                """.trimIndent() else ""

                val prompt = """
                    Você é um jornalista experiente que explica notícias de forma clara e acessível
                    para o público brasileiro.

                    IMPORTANTE: Vá direto ao conteúdo. NÃO use saudações, apresentações, frases como
                    "Olá!", "Claro!", "Vamos entender...", "Com prazer!" ou qualquer introdução.
                    Comece imediatamente com o emoji e o título da primeira seção.

                    Gere uma explicação completa seguindo esta estrutura:

                    📰 O QUE ACONTECEU
                    Explique em 2 a 3 parágrafos: o quê, quem, onde, quando e como.
                    Seja específico com nomes, datas e números.

                    🔑 PONTOS PRINCIPAIS
                    • [ponto objetivo e direto]
                    • [ponto]
                    • [ponto]
                    (adicione mais pontos se necessário)
                    $termosSection

                    🌍 POR QUE ISSO IMPORTA
                    Explique em 1 a 2 parágrafos o impacto prático para as pessoas e o contexto
                    mais amplo: o que levou a isso e o que pode acontecer a seguir.

                    ---
                    NOTÍCIA:
                    Título: ${article.title}
                    Fonte: ${article.source.name}
                    Descrição: ${article.description ?: "Não disponível"}
                    Conteúdo: $content

                    Responda em Português do Brasil. Seja direto e informativo.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                _explanation.value = response.text ?: "Não foi possível gerar uma explicação."
            } catch (e: Exception) {
                _explanation.value = "Erro ao obter explicação: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
