package dev.tortoise.application.analysis

import dev.tortoise.application.documents.DocumentSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CachedAnalysisServiceTest {
    private val service = CachedAnalysisService()

    @Test
    fun `reuse cached analysis for same uri and version`() {
        val snapshot = DocumentSnapshot(
            uri = "file:///analysis.logo",
            version = 1,
            text = "to demo\n  forward 1\nend",
        )

        val first = service.analyze(snapshot)
        val second = service.analyze(snapshot)

        assertSame(first, second)
        assertSame(first, service.getCached(snapshot.uri))
    }

    @Test
    fun `recompute analysis when version changes`() {
        val uri = "file:///analysis.logo"
        val version1 = DocumentSnapshot(uri = uri, version = 1, text = "to demo\n  missingFirst\nend")
        val version2 = DocumentSnapshot(uri = uri, version = 2, text = "to demo\n  missingSecond\nend")

        val first = service.analyze(version1)
        val second = service.analyze(version2)

        assertNotSame(first, second)
        assertEquals(1, first.diagnostics.size)
        assertEquals(1, second.diagnostics.size)
        assertEquals("Unknown procedure 'missingFirst'.", first.diagnostics.single().message)
        assertEquals("Unknown procedure 'missingSecond'.", second.diagnostics.single().message)
        assertNotNull(service.getCached(uri))
        assertEquals(2, service.getCached(uri)?.version)
    }

    @Test
    fun `clear removes cached analysis`() {
        val uri = "file:///analysis.logo"
        service.analyze(DocumentSnapshot(uri = uri, version = 1, text = "forward :x"))
        assertNotNull(service.getCached(uri))

        service.clear(uri)

        assertNull(service.getCached(uri))
    }
}
