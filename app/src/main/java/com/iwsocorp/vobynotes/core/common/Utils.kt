package com.iwsocorp.vobynotes.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.iwsocorp.vobynotes.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

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

    fun containsWordRegex(sentence: String, word: String): Boolean {
        val pattern = "\\b${Regex.escape(word)}\\b".toRegex(RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(sentence)
    }

    fun generateRandomString(length: Int): String {
        val charset = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charset.size) }
            .map(charset::get)
            .joinToString("")
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

    fun Toolbar.setIconColor(context: Context) =
        this.overflowIcon?.setTint(ContextCompat.getColor(context, R.color.black))
}