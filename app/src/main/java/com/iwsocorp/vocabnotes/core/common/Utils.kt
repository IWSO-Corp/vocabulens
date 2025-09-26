package com.iwsocorp.vocabnotes.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.iwsocorp.vocabnotes.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object Utils {

    fun Long.asString(): String {
        val date = Date(this)
        val format = SimpleDateFormat("MMM dd", Locale.US)
        return format.format(date)
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