package org.icpclive.reactions.recorder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import kotlin.io.path.appendLines
import kotlin.io.path.createFile
import kotlin.math.exp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Recorder(private val cds: Flow<ContestUpdate>) {
    private val actionLogger = Path.of("record.log")

    init {
        try {
            actionLogger.createFile()
        } catch (ignore: FileAlreadyExistsException) {
        }
    }

    private fun log(s: String) {
        actionLogger.appendLines(listOf(s))

    }

    private val clientsCache = mutableMapOf<GrabberClient.Config, GrabberClient>()

    private fun getGrabberClient(contestInfo: ContestInfo, run: RunInfo): GrabberClient? {
        val team = contestInfo.teams[run.teamId] ?: return null
        val media = team.medias[TeamMediaType.SCREEN] as? MediaType.WebRTCGrabberConnection
        if (media == null) {
            log.info { "No media grabber info for ${run.teamId}" }
            return null
        }
        val clientConfig = GrabberClient.Config(media.url, media.credential?.let { "admin" to it })
        return clientsCache.computeIfAbsent(clientConfig) { GrabberClient(clientConfig) }
    }

    private fun getPeerName(contestInfo: ContestInfo, run: RunInfo): String? {
        val c = contestInfo.teams[run.teamId]?.let { it.customFields["grabberPeerName"] }
        if (c == null) {
            log.info { "Failed to get comp of ${run.teamId}" }
        }
        return c
    }

    suspend fun run(scope: CoroutineScope) {
        val cache = mutableMapOf<RunId, RunInfo>()
        var contestInfo: ContestInfo? = null

        cds.collect {
            if (it is InfoUpdate) {
                contestInfo = it.newInfo
                return@collect
            } else if (contestInfo == null || it !is RunUpdate) {
                return@collect
            }
            val run = it.newInfo
            if (run.teamId !in contestInfo!!.teams) {
                return@collect
            } else if (contestInfo!!.currentContestTime - run.time > DEFAULT_MAX_RECORD_DELAY) {
                log.info { "Skip too old run ${run.id}" }
                return@collect
            }

            val time = Clock.System.now().format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
            val runInCache = cache[run.id]
            val client = getGrabberClient(contestInfo!!, run)
            if (runInCache?.result is RunResult.InProgress && run.result !is RunResult.InProgress) {
                if (storage.getRecordInfo(run.id)?.state != RecordState.STOPPED) {
                    log("${time};stop;${run.id};${run.teamId};${run.problemId};${(run.result as RunResult.ICPC).verdict}")
                    storage.setStopTime(run.id, now())
                    log.info { "Schedule stop by ${run.id}" }
                    scope.launch {
                        val peerName = getPeerName(contestInfo!!, run) ?: return@launch
                        delay(atLeastContestEnd(contestInfo!!, RECORD_AFTER_STOP))
                        try {
                            client?.stopRecord(peerName, run.id.toString())
                        } catch (e: Exception) {
                            log.warning { "Failed to send stop command $peerName (${run.id}): ${e}" }
                        }
                    }
                } else {
                    log.warning { "Record already stopped for ${run.id}" }
                }
            } else if (runInCache == null && run.result is RunResult.InProgress) {
                if (storage.getRecordInfo(run.id) == null) {
                    log("${time};start;${run.id};${run.teamId};${run.problemId};")
                    storage.setStartTime(run.id, now())
                    log.info { "Start testing ${run.id}" }
                    scope.launch {
                        val peerName = getPeerName(contestInfo!!, run) ?: return@launch
                        try {
                            client?.startRecord(
                                peerName, run.id.toString(), atLeastContestEnd(contestInfo!!, DEFAULT_MAX_RECORD_DELAY)
                            )
                        } catch (e: Exception) {
                            log.warning { "Failed to send start record command $peerName (${run.id}): ${e}" }
                        }
                    }
                } else {
                    log.warning { "Record already started (as testing) for ${run.id}" }
                }
            } else if (runInCache == null) {
                if (storage.getRecordInfo(run.id) == null) {
                    log("${time};start;${run.id};${run.teamId};${run.problemId};${(run.result as RunResult.ICPC).verdict}")
                    log.info { "Start judged ${run.id} ${(run.result as RunResult.ICPC).verdict}" }
                    storage.setStopTime(run.id, now(), nowWithDefaultTimeout())
                    scope.launch {
                        val peerName = getPeerName(contestInfo!!, run) ?: return@launch
                        try {
                            client?.startRecord(
                                peerName, run.id.toString(), atLeastContestEnd(contestInfo!!, MAX_RECORD_DURATION)
                            )
                        } catch (e: Exception) {
                            log.warning { "Failed to send start record command $peerName (${run.id}): ${e}" }
                        }
                    }
                } else {
                    log.warning { "Record already started (as judged) for ${run.id}" }
                }
            }
            cache[run.id] = run
        }
    }

    private fun atLeastContestEnd(contestInfo: ContestInfo, expected: Duration): Duration {
        val now = Clock.System.now()
        val stopTime = contestInfo.instantAt(contestInfo.contestLength - 3.seconds)
        if (now <= stopTime) {
            return minOf(expected, stopTime - now)
        }
        return expected
    }

    companion object {
        val log by getLogger()

        fun now() = Clock.System.now()
        fun nowWithDefaultTimeout() = now() + MAX_RECORD_DURATION

        private val MAX_RECORD_DURATION = 3.minutes
        private val DEFAULT_MAX_RECORD_DELAY = 2.minutes
        private val RECORD_AFTER_STOP = 30.seconds
    }
}
