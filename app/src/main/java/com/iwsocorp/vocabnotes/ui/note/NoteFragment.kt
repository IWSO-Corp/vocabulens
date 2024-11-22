package com.iwsocorp.vocabnotes.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.iwsocorp.vocabnotes.core.common.Utils.generateRandomString
import com.iwsocorp.vocabnotes.core.data.model.createCorpus
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.databinding.FragmentNoteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class NoteFragment(
    private val noteId: String? = null,
) : Fragment() {

    private val viewModel: NoteViewModel by viewModels()
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteId?.let {
            viewModel.updateNoteId(it)
        }

        viewModel.noteId.observe(viewLifecycleOwner) {
            it?.let { id ->
//                viewModel.getNote(id) { note ->
//                    Timber.d("note: $note")
//                    Timber.d("noteCorpus: ${note.content}")
//                    setupRecyclerView(note.content)
//                }
                viewModel.getCorpusByNoteId(id)
                viewModel.corpusList.observe(viewLifecycleOwner) { corpusList ->
                    Timber.d("corpusList: $corpusList")
                    setupRecyclerView(corpusList)
                }
            }
        }

        binding.btnAdd.setOnClickListener {
            lifecycleScope.launch {
                onSubmit()
            }
        }
    }

    private suspend fun onSubmit() {
        val word = binding.edWord.text.toString()
        val meaning = binding.edMeaning.text.toString()
        if (word.isEmpty() || meaning.isEmpty()) return

        val random = generateRandomString(10)
        val id = "corpus-$random"
        val noteIdNew = "note-$random"

        val vocab = viewModel.getVocabulary(word)
        val corpus: Corpus =
            vocab.createCorpus(id, viewModel.noteId.value ?: noteIdNew, word, meaning)

        viewModel.insertCorpus(corpus)
        viewModel.insertWordMeanings(id, vocab.meanings)

        if (viewModel.noteId.value == null) {
            val now = System.currentTimeMillis()
            val note = Note(
                id = noteIdNew,
                title = "",
                wordLang = "",
                meaningLang = "",
                content = listOf(corpus),
                createdAt = now,
                updatedAt = now
            )
            viewModel.createNewNote(note)
            viewModel.updateNoteId(noteIdNew)
        }

        binding.edWord.text?.clear()
        binding.edMeaning.text?.clear()
    }

    private fun setupRecyclerView(corpusList: List<Corpus>) {
        binding.tvEmpty.visibility = if (corpusList.isEmpty()) View.VISIBLE else View.GONE

        val listener = object : WordAdapter.ClickListener {

            override fun onClick(corpus: Corpus) {
                val def = corpus.meanings.takeIf { it.isNotEmpty() }
                    ?.first()?.definitions?.first()?.definition
                Snackbar.make(
                    binding.root,
                    def ?: "Undefined",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

        }

        binding.rvNote.apply {
            adapter = WordAdapter(corpusList, listener)
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNoteBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}