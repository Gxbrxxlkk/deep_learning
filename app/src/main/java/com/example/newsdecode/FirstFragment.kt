package com.example.newsdecode

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsdecode.databinding.FragmentFirstBinding
import com.example.newsdecode.viewmodel.NewsViewModel

/**
 * Tela principal do app: exibe o feed de notícias.
 *
 * Funcionalidades:
 * - Chips de categoria para filtrar o feed
 * - Campo de busca por palavra-chave
 * - Scroll infinito (carrega mais notícias ao chegar no fim da lista)
 * - Navegação para o SecondFragment ao tocar em "Descomplicar"
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    // activityViewModels compartilha o mesmo ViewModel com o SecondFragment,
    // permitindo passar o artigo selecionado sem usar argumentos de navegação
    private val viewModel: NewsViewModel by activityViewModels()
    private lateinit var adapter: NewsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupSearch()
        setupCategoryChips()
        setupInfiniteScroll()
        setupObservers()

        // Carrega o feed apenas na primeira vez que o fragment é criado
        if (viewModel.articles.value == null) {
            viewModel.fetchNews()
        }
    }

    private fun setupAdapter() {
        adapter = NewsAdapter { article ->
            viewModel.explainArticle(article)
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        binding.recyclerViewNews.adapter = adapter
    }

    private fun setupSearch() {
        binding.buttonSearch.setOnClickListener { doSearch() }

        // Permite disparar a busca pressionando "Search" no teclado virtual
        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }
    }

    private fun doSearch() {
        val query = binding.editTextSearch.text.toString().trim()
        when {
            query.isBlank() -> return

            // A NewsData.io exige mínimo de 4 caracteres no parâmetro q.
            // Para termos curtos (ex: "Irã", "Cão"), usamos qInTitle como fallback.
            query.length < 4 -> {
                binding.chipGroupCategories.clearCheck()
                viewModel.searchNews(query, shortQuery = true)
                hideKeyboard()
            }

            else -> {
                binding.chipGroupCategories.clearCheck()
                viewModel.searchNews(query)
                hideKeyboard()
            }
        }
    }

    private fun setupCategoryChips() {
        val chipMap = mapOf(
            binding.chipAll           to NewsViewModel.Category.ALL,
            binding.chipPolitics      to NewsViewModel.Category.POLITICS,
            binding.chipBusiness      to NewsViewModel.Category.BUSINESS,
            binding.chipTechnology    to NewsViewModel.Category.TECHNOLOGY,
            binding.chipScience       to NewsViewModel.Category.SCIENCE,
            binding.chipSports        to NewsViewModel.Category.SPORTS,
            binding.chipEntertainment to NewsViewModel.Category.ENTERTAINMENT
        )

        chipMap.forEach { (chip, category) ->
            chip.setOnClickListener {
                binding.editTextSearch.text?.clear() // limpa a busca ao trocar categoria
                viewModel.fetchNews(category)
                hideKeyboard()
            }
        }
    }

    /**
     * Implementa scroll infinito observando a posição de rolagem da RecyclerView.
     * Quando o último item visível está a 5 itens do fim da lista,
     * solicita a próxima página ao ViewModel.
     */
    private fun setupInfiniteScroll() {
        val layoutManager = binding.recyclerViewNews.layoutManager as LinearLayoutManager

        binding.recyclerViewNews.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return // ignora scroll para cima

                val totalItems  = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()

                if (lastVisible >= totalItems - 5) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.articles.observe(viewLifecycleOwner) { articles ->
            adapter.setArticles(articles ?: emptyList())
        }

        // Loading da primeira página: cobre a tela com um spinner central
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Loading de paginação: spinner pequeno no rodapé, sem bloquear a lista
        viewModel.loadingMore.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarLoadMore.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    // Libera o binding ao destruir a view para evitar memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
