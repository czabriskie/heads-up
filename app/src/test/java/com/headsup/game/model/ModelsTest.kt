package com.headsup.game.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    private fun track(id: String? = "t1", type: String = "track", isLocal: Boolean = false) =
        Track(id = id, name = "Song", uri = "spotify:track:$id", type = type, isLocal = isLocal)

    @Test
    fun `playableTrack prefers the new item field`() {
        val entry = PlaylistTrackItem(item = track("new"), track = track("old"))
        assertEquals("new", entry.playableTrack?.id)
    }

    @Test
    fun `playableTrack falls back to the deprecated track field`() {
        assertEquals("old", PlaylistTrackItem(track = track("old")).playableTrack?.id)
    }

    @Test
    fun `local files episodes and removed tracks are not playable`() {
        assertNull(PlaylistTrackItem(item = track(), isLocal = true).playableTrack)
        assertNull(PlaylistTrackItem(item = track(isLocal = true)).playableTrack)
        assertNull(PlaylistTrackItem(item = track(type = "episode")).playableTrack)
        assertNull(PlaylistTrackItem(item = track(id = null)).playableTrack)
        assertNull(PlaylistTrackItem().playableTrack)
    }

    @Test
    fun `parses a playlist items page`() {
        val page = json.decodeFromString<TrackPage>(
            """
            {"items":[
              {"is_local":false,"item":{"id":"a","type":"track","name":"A","uri":"spotify:track:a",
                 "duration_ms":1000,"artists":[{"name":"X"},{"name":"Y"}]}},
              {"is_local":true,"item":{"id":null,"type":"track","name":"Local","uri":"spotify:local:x"}},
              {"item":null}
            ],"next":null,"total":3}
            """
        )
        val playable = page.items.mapNotNull { it.playableTrack }
        assertEquals(listOf("a"), playable.map { it.id })
        assertEquals("X, Y", playable.single().artistNames)
        assertNull(page.next)
    }

    @Test
    fun `playlist is readable when owned or collaborative`() {
        val mine = SimplePlaylist(id = "p", name = "P", owner = PlaylistOwner("me"))
        val theirs = SimplePlaylist(id = "q", name = "Q", owner = PlaylistOwner("spotify"))
        val shared = theirs.copy(collaborative = true)
        assertTrue(mine.isReadableBy("me"))
        assertFalse(theirs.isReadableBy("me"))
        assertTrue(shared.isReadableBy("me"))
        assertFalse(mine.isReadableBy(null))
    }
}
