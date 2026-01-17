package org.icpclive.reactions.recorder

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.RunId
import kotlin.time.ExperimentalTime
import kotlin.time.Clock

@Serializable
@OptIn(ExperimentalTime::class)
data class RecordInfo(
    val runId: RunId,
    val startTime: Instant,
    val stopTime: Instant? = null,
    val uploaded: Boolean = false
)

@OptIn(ExperimentalTime::class)
val RecordInfo.state: RecordState
    get() = when {
        this.stopTime != null && this.stopTime <= Clock.System.now() -> RecordState.STOPPED
        else -> RecordState.STARTED
    }

enum class RecordState {
    STARTED, STOPPED
}
