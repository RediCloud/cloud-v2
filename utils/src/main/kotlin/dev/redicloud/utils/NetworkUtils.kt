package dev.redicloud.utils

import java.net.NetworkInterface
import java.util.regex.Pattern

private val ipV4Pattern = Pattern.compile(
    """((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])"""
)

private val ipV6Pattern = Pattern.compile(
    """(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))"""
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
    if (ip.startsWith("[") || ip.endsWith("]")) {
        return ipV6Pattern.matcher(ip.substring(1, ip.length - 1)).matches()
    }
    return ipV6Pattern.matcher(ip).matches()
}
