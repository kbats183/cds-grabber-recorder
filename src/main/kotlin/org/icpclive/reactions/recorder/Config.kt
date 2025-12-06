package org.icpclive.reactions.recorder

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kotlinx.coroutines.runBlocking
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.cli.CdsCommandLineOptions

object Config : CliktCommand() { // help = "java -jar reactions.jar", printHelpOnEmptyArgs = true
    val cdsSettings by CdsCommandLineOptions()

    override fun run() {
        val loader = config.cdsSettings
            .toFlow()
            .addComputedData()

        runBlocking {
            Recorder(loader).run(this@runBlocking)
        }
    }
}

val config: Config get() = Config
val storage: Storage get() = Storage(config.cdsSettings.configDirectory.resolve(("storage.json")))