package com.iwsocorp.vobynotes.core.common

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.gson.Gson
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.model.Language
import com.iwsocorp.vobynotes.core.model.SupportedLanguages
import com.iwsocorp.vobynotes.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {

    fun String.removePunctuation(): String {
        return this.replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun View.fadeVisibility(show: Boolean, duration: Long = 300) {
        val transition = Fade().apply { this.duration = duration }
        TransitionManager.beginDelayedTransition(this.parent as ViewGroup, transition)
        visibility = if (show) View.VISIBLE else View.GONE
    }

    fun Long.asString(): String {
        val date = Date(this)
        val format = SimpleDateFormat("MMM dd", Locale.US)
        return format.format(date)
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun Context.alertInputDialog(noteTitle: String, action: (String) -> Unit) {
        val editText = EditText(this).apply {
            setText(noteTitle)
            hint = "Note title"
            inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Create new note")
            .setMessage("Enter title:")
            .setView(editText)
            .setPositiveButton("Create") { dialog, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    action(input)
                } else {
                    Toast.makeText(this, "Input cannot be empty", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun showAlertDialog(
        context: Context,
        title: String,
        description: String?,
        positiveButton: String = "Ok",
        negativeButton: String = "Cancel",
        action: () -> Unit,
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(description)
            .setPositiveButton(positiveButton) { _, _ ->
                action()
            }
            .setNegativeButton(negativeButton, null)
            .show()
    }

    fun showPopupMenu(
        context: Context,
        view: View,
        menuItems: List<Pair<String, () -> Unit>>,
    ) {
        val popupMenu = android.widget.PopupMenu(context, view)
        menuItems.forEach { (title, action) ->
            popupMenu.menu.add(title)
                .setOnMenuItemClickListener {
                    action()
                    true
                }
        }
        popupMenu.show()
    }

    fun Context.sharePublicNoteLink(noteTitle: String, link: String) {
        val shareText = "Check this vocabulary: $noteTitle on Vocabulens! \n$link"
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun loadLanguages(context: Context): List<Language> {
        val json = context.assets.open("languages.json").bufferedReader().use { it.readText() }
        val wrapper = Gson().fromJson(json, SupportedLanguages::class.java)
        return wrapper.supported_languages
    }

    fun String.normalizeLanguageCode(): String = when (this.lowercase()) {
        "in" -> "id"
        "iw" -> "he"
        "ji" -> "yi"
        else -> this.lowercase()
    }

    fun getLocaleLang(context: Context): String {
        val languages = loadLanguages(context)
        val localeCode = Locale.getDefault().language
        return languages.find {
            it.code == localeCode.normalizeLanguageCode()
        }?.code ?: "en"
    }

    fun String.langCode(context: Context): String =
        loadLanguages(context).find { it.name == this }?.code ?: "en"

    fun String.langName(context: Context): String =
        loadLanguages(context).find { it.code == this }?.name ?: "English"

    fun ItemNoteBinding.setupMark(
        context: Context,
        familiarCount: Int,
        unfamiliarCount: Int
    ) {
        val iconFam = ContextCompat.getDrawable(context, R.drawable.baseline_star_24)
        iconFam?.setBounds(0, 0, 48, 48) // width x height dalam px
        iconFam?.setTint(
            context.resources.getColor(
                R.color.blue,
                context.theme
            )
        )
        val iconUnfam = ContextCompat.getDrawable(context, R.drawable.baseline_star_24)
        iconUnfam?.setBounds(0, 0, 48, 48) // width x height dalam px
        iconUnfam?.setTint(
            context.resources.getColor(
                R.color.red,
                context.theme
            )
        )

        tvFamiliar.setCompoundDrawables(iconFam, null, null, null)
        tvUnfamiliar.setCompoundDrawables(iconUnfam, null, null, null)

        tvFamiliar.text = familiarCount.toString()
        tvFamiliar.setTextColor(
            context.resources.getColor(
                R.color.blue,
                context.theme
            )
        )

        tvUnfamiliar.text = unfamiliarCount.toString()
        tvUnfamiliar.setTextColor(
            context.resources.getColor(
                R.color.red,
                context.theme
            )
        )
    }

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

}