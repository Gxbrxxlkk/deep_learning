# Newsdecode 📰

App Android que busca notícias em tempo real e usa o **Google Gemini** para explicar cada notícia em português simples e acessível.

---

## Como rodar o projeto

### 1. Chaves de API necessárias

| Serviço | Onde obter |
|---|---|
| **NewsData.io** | [newsdata.io](https://newsdata.io) |
| **Google Gemini** | [aistudio.google.com](https://aistudio.google.com) |

### 2. Configurar `local.properties`

Na raiz do projeto, edite o arquivo `local.properties` e adicione:

```properties
NEWSDATA_API_KEY=sua_chave_aqui
GEMINI_API_KEY=sua_chave_aqui
```

### 3. Compilar

Abra o projeto no Android Studio e execute em um dispositivo ou emulador com Android 7.0+ (API 24).

---

## Como o Gemini é utilizado

Ao tocar em **"Descomplicar"** em qualquer notícia, o app envia o conteúdo do artigo para o Gemini e exibe uma explicação estruturada em português.

### Onde fica no código

Todo o código relacionado ao Gemini está em:

```
app/src/main/java/com/example/newsdecode/viewmodel/NewsViewModel.kt
```

As partes principais dentro desse arquivo:

**Inicialização do modelo** (linha 90)
```kotlin
private val generativeModel = GenerativeModel(
    modelName = "gemini-2.5-flash",
    apiKey = Config.GEMINI_API_KEY,
    generationConfig = generationConfig {
        temperature = 0.2f  // respostas mais factuais, menos criativas
    }
)
```

**Chamada ao modelo** — função `explainArticle()` (linha ~160)
```kotlin
val response = generativeModel.generateContent(prompt)
_explanation.value = response.text
```

**O prompt** é montado dentro de `explainArticle()` e instrui o Gemini a:
- Responder sempre em Português do Brasil
- Seguir uma estrutura fixa (O que aconteceu / Pontos principais / Por que isso importa)
- Não usar saudações ou introduções
- Incluir explicação de termos técnicos apenas quando o artigo for longo o suficiente
EOF
