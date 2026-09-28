package com.styl15hh1.rn301controller.data.network

import com.styl15hh1.rn301controller.data.model.*
import java.net.Inet4Address
import java.net.InetAddress

@JvmInline
value class ReceiverAddress private constructor(val host: String) {
    companion object {
        fun parse(value: String): ReceiverAddress {
            val host = value.trim().removeSuffix(".")
            val labels = host.split(".")
            val validHost = host.length in 1..253 && labels.all {
                it.length in 1..63 && it.matches(Regex("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"))
            }
            val numeric = host.all { it.isDigit() || it == '.' }
            val validIp = !numeric || (labels.size == 4 && labels.all {
                it.toIntOrNull() in 0..255 && (it == "0" || !it.startsWith("0"))
            })
            if (!validHost || !validIp) throw YamahaException(ReceiverError(ErrorKind.INVALID_ADDRESS))
            return ReceiverAddress(host)
        }

        fun isLocal(address: InetAddress): Boolean {
            if (address !is Inet4Address) return false
            val b = address.address.map { it.toInt() and 255 }
            return b[0] == 10 || (b[0] == 172 && b[1] in 16..31) ||
                (b[0] == 192 && b[1] == 168) || (b[0] == 169 && b[1] == 254)
        }
    }
}
