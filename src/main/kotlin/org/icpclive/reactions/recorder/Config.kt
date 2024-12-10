package org.icpclive.reactions.recorder

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValue
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.cli.CdsCommandLineOptions
import org.icpclive.cds.plugins.pcms.PCMSSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import kotlin.io.path.name
import kotlin.time.Duration.Companion.seconds

object Config : CliktCommand() { // help = "java -jar reactions.jar", printHelpOnEmptyArgs = true
    //    private val standings by option("-s", "--standings", help = "The standings.xml file url").required()
//    private val jobs by option("-j", "--jobs", help = "The jobs.xml file url").required()
    val cdsSettings by CdsCommandLineOptions()
//    val storageFile by option("--storage-file", "-s").path()
//        .optionalValue(cdsSettings.configDirectory.resolve("storage.json"))
//        .default(cdsSettings.configDirectory.resolve("storage.json"))


    private val grabberUrl by option("--grabber", help = "The username for the host").required()

    //    private val password by option("-p", "--password", help = "The password for the user").required()
//    private val destination by option("-d", "--destination", help = "The destination folder on the host").default("reactions/data")
//
//    val reactionsUrlTemplate by option(
//        "-t",
//        "--reactions-url-template",
//        help = "The template of of public location of done reactions"
//    ).required()
//
//    val overlayUrl by option("-o", "--overlay-url", help = "The url for the overlay")
//        .default("http://127.0.0.1:8080")
//
//    val delayBetweenUploads by option("--delay", help = "The delay between uploads")
//        .long().default(1000 * 60 * 5)
    override fun run() {
        val loader = config.cdsSettings
            .toFlow()
            .addComputedData()

        val client = GrabberClient(grabberUrl, "admin" to "live")

        runBlocking {
            Recorder(loader, client).run(this@runBlocking)
        }
    }
}

val config: Config get() = Config
val storage: Storage get() = Storage(config.cdsSettings.configDirectory.resolve(("storage.json")))