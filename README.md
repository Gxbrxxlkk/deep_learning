# Newsdecode

App Android que busca notícias em tempo real e usa IA (Google Gemini) para gerar explicações detalhadas em português, tornando qualquer notícia acessível ao público geral.

---

## Funcionalidades

- **Feed de notícias** com categorias navegáveis (Política, Economia, Tecnologia, Ciência, Esportes, Entretenimento)
- **Busca por palavra-chave** com suporte a termos curtos (menos de 4 caracteres)
- **Scroll infinito** — novas notícias carregam automaticamente ao rolar o feed
- **Descomplicar** — envia o artigo ao Gemini e retorna uma explicação estruturada em PT-BR

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| Arquitetura | MVVM (Model-View-ViewModel) |
| UI | Fragments + ViewBinding + Material Design |
| Navegação | Navigation Component |
| Rede | Retrofit + Gson |
| Imagens | Coil |
| LLM | Google Gemini 2.5 Flash (SDK generativeai) |
| API de notícias | NewsData.io |

---

## Configuração

### 1. Pré-requisitos
- Android Studio Hedgehog ou superior
- JDK 8+
- Conta nas APIs abaixo

### 2. Chaves de API necessárias

| Serviço | Onde obter | Uso |
|---|---|---|
| **NewsData.io** | [newsdata.io](https://newsdata.io) | Busca de notícias |
| **Google Gemini** | [aistudio.google.com](https://aistudio.google.com) | Explicações via LLM |

### 3. Configurar `local.properties`

Na raiz do projeto, crie o arquivo `local.properties` e substitua os placeholders pelas suas chaves:

```properties
NEWSDATA_API_KEY=sua_chave_newsdata_aqui
GEMINI_API_KEY=sua_chave_gemini_aqui
```

> **Nunca commite este arquivo.** Ele já está no `.gitignore` por padrão.

### 4. Compilar e executar

Abra o projeto no Android Studio e execute em um dispositivo ou emulador com Android 7.0+ (API 24).

---

## Arquitetura

```
com.example.newsdecode
├── api/
│   └── NewsApiService.kt       # Interface Retrofit com os endpoints da NewsData.io
├── model/
│   └── NewsModels.kt           # Data classes: NewsResponse, Article, Source
├── viewmodel/
│   └── NewsViewModel.kt        # Lógica de negócio: fetch, busca, paginação, Gemini
├── Config.kt                   # Acesso centralizado às chaves via BuildConfig
├── FirstFragment.kt            # Tela do feed (chips, busca, scroll infinito)
├── SecondFragment.kt           # Tela de explicação do Gemini
├── NewsAdapter.kt              # Adapter da RecyclerView
└── MainActivity.kt             # Activity host com Navigation Component
```

---

## Como funciona a integração com o Gemini

O app usa o SDK oficial `com.google.ai.client.generativeai`. Ao tocar em **Descomplicar**, o `NewsViewModel` monta um prompt com título, descrição e conteúdo do artigo e envia ao modelo:

```kotlin
val generativeModel = GenerativeModel(
    modelName = "gemini-2.5-flash",
    apiKey = Config.GEMINI_API_KEY,
    generationConfig = generationConfig {
        temperature = 0.2f  // respostas mais factuais, menos criativas
    }
)
```

**Técnicas de prompt engineering usadas:**
- **Role prompting** — "Você é um jornalista experiente..." direciona estilo e vocabulário
- **Instrução negativa** — proíbe saudações que o modelo geraria por padrão
- **Output estruturado** — seções fixas com emojis garantem consistência na UI
- **Prompt condicional** — seção de termos técnicos só aparece para artigos com conteúdo substancial (> 300 chars)
