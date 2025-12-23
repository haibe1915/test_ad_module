package com.example.test_kotlin_compose.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

object LoadingDialogUtil {
    private var dialog: Dialog? = null

    fun showLoadingDialog(context: Context, message: String? = "") {
        dismissLoadingDialog() // Dismiss any existing dialog

        try {
            dialog = Dialog(context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(50, 50, 50, 50)
                    setBackgroundColor(Color.WHITE) // You might want to use a drawable for rounded corners

                    val progressBar = ProgressBar(context)
                    addView(progressBar)

                    if (!message.isNullOrEmpty()) {
                        val textView = TextView(context).apply {
                            text = message
                            textSize = 16f
                            setPadding(30, 0, 0, 0)
                            setTextColor(Color.BLACK)
                        }
                        addView(textView)
                    }
                }

                setContentView(layout, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissLoadingDialog() {
        try {
            if (dialog != null && dialog!!.isShowing) {
                dialog!!.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            dialog = null
        }
    }
}

