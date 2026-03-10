package com.example.newsdecode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.newsdecode.databinding.ItemNewsBinding
import com.example.newsdecode.model.Article
import coil.load
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Adapter da RecyclerView responsável por exibir a lista de notícias.
 *
 * Implementa o padrão ViewHolder: em vez de criar uma nova View para cada
 * artigo (custoso), a RecyclerView reutiliza os ViewHolders que saem da tela,
 * apenas atualizando seu conteúdo via bind().
 *
 * @param onArticleClick Callback chamado quando o usuário toca em "Descomplicar"
 */
class NewsAdapter(private val onArticleClick: (Article) -> Unit) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private var articles = listOf<Article>()

    /** Substitui toda a lista e notifica a RecyclerView para redesenhar */
    fun setArticles(newArticles: List<Article>) {
        articles = newArticles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(articles[position])
    }

    override fun getItemCount() = articles.size

    inner class NewsViewHolder(private val binding: ItemNewsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(article: Article) {
            binding.textViewTitle.text = article.title
            binding.textViewDescription.text = article.description ?: "Sem descrição disponível"
            binding.textViewSource.text = article.source.name
            binding.textViewDate.text = formatRelativeDate(article.publishedAt ?: "")

            // Coil: biblioteca de carregamento de imagens com cache automático
            binding.imageViewArticle.load(article.urlToImage) {
                placeholder(android.R.drawable.ic_menu_report_image)
                error(android.R.drawable.ic_menu_report_image)
            }

            binding.buttonExplain.setOnClickListener {
                onArticleClick(article)
            }
        }

        /**
         * Converte a data ISO 8601 da API em formato relativo legível.
         * Ex: "2024-01-15T10:30:00Z" → "há 2h"
         */
        private fun formatRelativeDate(publishedAt: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date: Date = sdf.parse(publishedAt) ?: return publishedAt
                val diffMs = System.currentTimeMillis() - date.time
                val diffMin = diffMs / 60_000
                when {
                    diffMin < 60   -> "há ${diffMin}min"
                    diffMin < 1440 -> "há ${diffMin / 60}h"
                    else           -> "há ${diffMin / 1440}d"
                }
            } catch (e: Exception) {
                publishedAt.take(10) // fallback: mostra só a data (YYYY-MM-DD)
            }
        }
    }
}
