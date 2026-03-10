package com.example.newsdecode

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.newsdecode.databinding.FragmentSecondBinding
import com.example.newsdecode.viewmodel.NewsViewModel

/**
 * Tela de explicação: exibe o título do artigo e a explicação gerada pelo Gemini.
 *
 * O conteúdo vem do ViewModel compartilhado — o artigo já foi enviado ao Gemini
 * pelo FirstFragment antes da navegação ocorrer.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Preenche cabeçalho com título e fonte do artigo selecionado
        viewModel.currentArticle.observe(viewLifecycleOwner) { article ->
            binding.textViewArticleTitle.text = article.title
            binding.textViewArticleSource.text = article.source.name
        }

        // Alterna visibilidade entre o spinner e o texto conforme o Gemini processa
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarSecond.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.textviewSecond.visibility    = if (isLoading) View.INVISIBLE else View.VISIBLE
        }

        viewModel.explanation.observe(viewLifecycleOwner) { explanation ->
            binding.textviewSecond.text = explanation
        }

        binding.buttonSecond.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
