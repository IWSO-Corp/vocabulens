package com.iwsocorp.vocabnotes.ui.note

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.iwsocorp.vocabnotes.core.common.Utils.generateRandomString
import com.iwsocorp.vocabnotes.core.data.model.createCorpus
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.databinding.FragmentNoteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

const val noteIdKey = "NOTE_ID"

@AndroidEntryPoint
class NoteFragment() : Fragment() {

    private var mediaPlayer: MediaPlayer? = null
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NoteViewModel by viewModels()
    private val noteId: String? by lazy {
        arguments?.getString(noteIdKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteId?.let {
            viewModel.updateNoteId(it)
        } ?: run {
            binding.tvEmpty.visibility = View.VISIBLE
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
        val worldLang = binding.tvWordLang.text.toString()
        val meaningLang = binding.tvMeaningLang.text.toString()
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
                wordLang = worldLang,
                meaningLang = meaningLang,
                content = listOf(corpus),
                createdAt = now,
                updatedAt = now
            )
            viewModel.createNewNote(note)
            viewModel.updateNoteId(noteIdNew)
        } else {
            viewModel.updateUpdatedAt(viewModel.noteId.value!!, System.currentTimeMillis())
        }

        binding.edWord.text?.clear()
        binding.edMeaning.text?.clear()
    }

    private fun setupRecyclerView(corpusList: List<Corpus>) {
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

            override fun onPlay(url: String) {
                if (url.isNotEmpty()) playAudio(url)
            }

        }

        binding.rvCorpus.adapter = WordAdapter(corpusList.sortedBy { it.word }, listener)
        binding.tvEmpty.visibility = if (corpusList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun playAudio(url: String) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    Timber.d("Audio started playing")
                }
                setOnCompletionListener {
                    resetMediaPlayer()
                    Timber.d("Audio finished playing")
                }
                setOnErrorListener { _, what, extra ->
                    resetMediaPlayer()
                    Timber.e("Error occurred while playing audio: what=$what, extra=$extra")
                    true
                }
            }
        } else {
            mediaPlayer?.start()
        }
    }

    private fun resetMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
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
        resetMediaPlayer()
    }

}