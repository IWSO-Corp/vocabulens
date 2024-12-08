package com.iwsocorp.vocabnotes.ui.note

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.common.Utils.generateRandomString
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.databinding.FragmentNoteBinding
import com.iwsocorp.vocabnotes.ui.detail.ARG_CORPUS_WORD
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

const val ARG_NOTE_ID = "noteIdParam"

@AndroidEntryPoint
class NoteFragment() : Fragment() {

    private var mediaPlayer: MediaPlayer? = null
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NoteViewModel by viewModels()
    private val noteId: String? by lazy {
        arguments?.getString(ARG_NOTE_ID)
    }
    private val wordAdapter: WordAdapter by lazy {
        WordAdapter(object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {
                findNavController().navigate(
                    R.id.action_noteFragment_to_corpusDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_WORD, corpus.word)
                    }
                )
            }

            override fun onPlay(url: String) {
                if (url.isNotEmpty()) playAudio(url)
            }
        })
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
//
//                    wordAdapter.appendData(note.content)
//
//                    binding.tvEmpty.visibility =
//                        if (note.content.isEmpty()) View.VISIBLE else View.GONE
//                }

//                viewModel.getCorpusByNoteId(id)

                viewModel.corpusListFlow(id).asLiveData().observe(viewLifecycleOwner) { corpusList ->
                    Timber.d("corpusList: $corpusList")

                    wordAdapter.appendData(corpusList)

                    binding.tvEmpty.visibility =
                        if (corpusList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        binding.rvCorpus.adapter = wordAdapter
        binding.btnAdd.setOnClickListener {
            onSubmit()
        }
    }

    private fun onSubmit() {
        val worldLang = binding.tvWordLang.text.toString()
        val meaningLang = binding.tvMeaningLang.text.toString()
        val word = binding.edWord.text.toString()
        val meaning = binding.edMeaning.text.toString()
        if (word.isEmpty() || meaning.isEmpty()) return

        val random = generateRandomString(10)
        val id = "corpus-$random"
        val noteIdNew = "note-$random"

        val corpus = Corpus(
            id = id,
            noteId = viewModel.noteId.value ?: noteIdNew,
            word = word,
            meaning = meaning,
            wordLang = worldLang,
            meaningLang = meaningLang,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModel.insertCorpus(corpus)

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
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        resetMediaPlayer()
    }

}