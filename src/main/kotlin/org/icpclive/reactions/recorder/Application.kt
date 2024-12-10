package org.icpclive.reactions.recorder

import com.github.ajalt.clikt.core.main
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    Config.main(args)
//    val client = GrabberClient("https://grabber.kbats.ru", "admin" to "live")
//    runBlocking {
////        client.startRecord("001", "03", 20.seconds)
////        client.startRecord("001", "01", 30.seconds)
////        client.stopRecord("001", "02")
////        client.uploadRecord("001", "03")
//    }
}
