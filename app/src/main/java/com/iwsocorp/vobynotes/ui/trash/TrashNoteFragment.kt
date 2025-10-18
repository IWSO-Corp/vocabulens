package com.iwsocorp.vobynotes.ui.trash

import androidx.fragment.app.viewModels
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentTrashNoteBinding

class TrashNoteFragment : BaseFragment<FragmentTrashNoteBinding>(FragmentTrashNoteBinding::inflate) {

    private val viewModel: TrashNoteViewModel by viewModels()

}