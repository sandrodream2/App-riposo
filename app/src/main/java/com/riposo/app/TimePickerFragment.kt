package com.riposo.app

import android.app.TimePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class TimePickerFragment(
    private val hour: Int,
    private val minute: Int,
    private val onPicked: (Int, Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return TimePickerDialog(activity, { _, h, m -> onPicked(h, m) }, hour, minute, true)
    }
}
