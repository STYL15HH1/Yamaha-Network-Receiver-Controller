package com.styl15hh1.rn301controller.data.network

import com.styl15hh1.rn301controller.data.model.ErrorKind

object HttpFailure {
    // A server answering HTTP is reachable. A rejected command is not failed discovery.
    fun kind(status: Int): ErrorKind = when (status) {
        404, 405, 501 -> ErrorKind.UNSUPPORTED_COMMAND
        else -> ErrorKind.COMMAND_FAILED
    }
}
