package org.icpclive.reactions.recorder

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.io.IOException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.icpclive.cds.api.RunId
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream

@OptIn(ExperimentalSerializationApi::class)
class Storage(val filePath: Path) {
    private val recordings: MutableMap<RunId, RecordInfo>
    private val mutex = Mutex()

    init {
        var recordingsMap: MutableMap<RunId, RecordInfo>
        try {
            filePath.inputStream().use { stream ->
                recordingsMap = json.decodeFromStream<List<RecordInfo>>(stream)
                    .associateBy { it.runId }
                    .toMutableMap()
            }
        } catch (e: IOException) {
            recordingsMap = mutableMapOf()
        }
        recordings = recordingsMap
    }

    private suspend fun updateRecord(id: RunId, body: (RecordInfo?) -> RecordInfo) {
        val recordingsCopy: List<RecordInfo>
        mutex.withLock {
            recordings.compute(id) { _, v -> body(v) }
            recordingsCopy = recordings.values.toList()
        }
        store(recordingsCopy)
    }

    private fun store(content: List<RecordInfo>) {
        val tmpName = filePath.resolveSibling(filePath.name + ".new")
        tmpName.outputStream().use { stream ->
            json.encodeToStream(content.sortedBy { it.startTime }, stream)
        }
        Files.move(tmpName, filePath, StandardCopyOption.REPLACE_EXISTING)
    }

    fun getRecordInfo(id: RunId): RecordInfo? = recordings[id]

    suspend fun setStartTime(id: RunId, startTime: Instant) = updateRecord(id) {
        RecordInfo(id, it?.startTime ?: startTime, it?.stopTime)
    }

    suspend fun setStopTime(id: RunId, startTime: Instant, stopTime: Instant) = updateRecord(id) {
        RecordInfo(id, it?.startTime ?: startTime, it?.stopTime ?: stopTime)
    }

    suspend fun setStopTime(id: RunId, stopTime: Instant) = updateRecord(id) {
        RecordInfo(id, it?.startTime ?: stopTime, it?.stopTime ?: stopTime)
    }
}

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    prettyPrint = true
    prettyPrintIndent = " "
}
