package org.icpclive.reactions.recorder

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.RunId

@Serializable
data class RecordInfo(
    val runId: RunId,
    val startTime: Instant,
    val stopTime: Instant? = null,
    val uploaded: Boolean = false
)

val RecordInfo.state: RecordState
    get() = when {
        this.stopTime != null && this.stopTime <= Clock.System.now() -> RecordState.STOPPED
        else -> RecordState.STARTED
    }

enum class RecordState {
    STARTED, STOPPED
}
