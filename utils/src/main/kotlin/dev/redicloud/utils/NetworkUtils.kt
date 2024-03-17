package dev.redicloud.utils

import java.net.NetworkInterface
import java.util.regex.Pattern

private val ipV4Pattern = Pattern.compile(
    "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"
)

private val ipV6Pattern = Pattern.compile(
    "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}"
)

fun getAllAddresses(): List<String> {
    val list = mutableListOf<String>()

    val enumNI = NetworkInterface.getNetworkInterfaces()
    while (enumNI.hasMoreElements()) {
        val ifc = enumNI.nextElement()
        if (!ifc.isUp) continue
        val enumAdds = ifc.inetAddresses
        while (enumAdds.hasMoreElements()) {
            val address = enumAdds.nextElement()
            list.add(address.hostAddress)
        }
    }

    return list
}

fun getAllIpV4(): List<String> {
    return getAllAddresses().filter { isIpv4(it) }
}

fun isIpv4(ip: String): Boolean {
    return ipV4Pattern.matcher(ip).matches()
}

fun isIpv6(ip: String): Boolean {
    return ipV6Pattern.matcher(ip).matches()
}
