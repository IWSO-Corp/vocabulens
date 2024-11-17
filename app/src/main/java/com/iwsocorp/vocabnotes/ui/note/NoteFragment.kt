package com.iwsocorp.vocabnotes.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.iwsocorp.vocabnotes.data.model.Corpus
import com.iwsocorp.vocabnotes.databinding.FragmentNoteBinding
import kotlin.random.Random

class NoteFragment(
    private var noteId: String? = null
) : Fragment() {

    private val viewModel: NoteViewModel by viewModels()
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteId?.let {
            viewModel.getNote(it) { note ->
                setupRecyclerView(note.content)
            }
        }

        noteId = ""

        binding.btnAdd.setOnClickListener {
            onSubmit()
        }
    }

    private fun onSubmit() {
        val word = binding.edWord.text.toString()
        val meaning = binding.edMeaning.text.toString()
        val now = System.currentTimeMillis()
        val random = generateRandomString(10)
        val id = "corpus-$random"
        val vocabId = "vocab-$random"

        viewModel.searchWord(word) { vocab ->
            val corpus = Corpus(
                id = id,
                vocabularyId = vocabId,
                word = word,
                meaning = meaning,
                phonetic = "",
                audio = "",
                meanings = listOf(),
                createdAt = now,
                updatedAt = now
            )

            vocab?.let {
                corpus.phonetic = it.phonetic
                corpus.audio = it.audio
                corpus.meanings = it.meanings
            }

            insertCorpus(corpus)
            noteId = vocabId
        }
    }

    fun generateRandomString(length: Int): String {
        val charset = ('A'..'Z') + ('a'..'z') + ('0'..'9') // Alphanumeric characters
        return (1..length)
            .map { Random.nextInt(0, charset.size) }
            .map(charset::get)
            .joinToString("")
    }

    private fun insertCorpus(corpus: Corpus) {}

    private fun setupRecyclerView(corpusList: List<Corpus>) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

}