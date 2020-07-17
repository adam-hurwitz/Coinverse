package app.coinverse.utils

import android.view.View.GONE

data class ToolbarState(val visibility: Int = GONE,
                        val titleRes: Int = 0,
                        val isActionBarEnabled: Boolean = false)