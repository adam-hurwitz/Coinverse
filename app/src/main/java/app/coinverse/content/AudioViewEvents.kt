package app.coinverse.content

import app.coinverse.content.AudioViewEventType.AudioPlayerLoad

/**
 * View state events for audio player
 */
interface AudioViewEvents {
    fun audioPlayerLoad(event: AudioPlayerLoad)
}

sealed class AudioViewEventType {
    data class AudioPlayerLoad(val contentId: String, val filePath: String, val previewImageUrl: String) : AudioViewEventType()
}