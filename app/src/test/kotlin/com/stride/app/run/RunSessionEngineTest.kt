package com.stride.app.run

import com.stride.app.location.LocationTracker
import com.stride.app.location.TrackPoint
import com.stride.app.ui.VoiceCueSpeaker
import com.stride.core.common.DispatcherProvider
import com.stride.core.common.WeatherSnapshot
import com.stride.core.data.repository.PlanRepository
import com.stride.core.data.repository.RunRepository
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.RunSessionEntity
import com.stride.core.database.entity.SessionStepEntity
import com.stride.core.database.entity.StepType
import com.stride.core.weather.WeatherRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Exercises the highest-risk file in the app — a singleton state machine mixing a tick loop,
 * GPS accumulation, and a Room write, previously covered by zero tests. Every fake here exists
 * only so this file (and the interface extractions on [VoiceCueSpeaker]/[LocationTracker] that
 * made it possible) can drive `RunSessionEngine` deterministically against a [TestDispatcher]
 * instead of a real 1-second `delay()`/real GPS/real TTS.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RunSessionEngineTest {

    private fun engineWith(
        steps: List<SessionStepEntity>,
        dispatcher: TestDispatcher,
        locationTracker: LocationTracker = FakeLocationTracker(),
        weatherSnapshot: WeatherSnapshot? = null,
        runRepository: FakeRunRepository = FakeRunRepository(),
        voiceCue: FakeVoiceCueSpeaker = FakeVoiceCueSpeaker(),
    ) = RunSessionEngine(
        planRepository = FakePlanRepository(steps),
        runRepository = runRepository,
        voiceCue = voiceCue,
        locationTracker = locationTracker,
        weatherRepository = FakeWeatherRepository(weatherSnapshot),
        dispatcherProvider = FixedDispatcherProvider(dispatcher),
    )

    /** A GPS fix with a realistic good accuracy — GpsSmoother's default 30m threshold otherwise
     * rejects the plain TrackPoint(...) construction's implicit accuracyMeters. */
    private fun fix(latitude: Double, longitude: Double, timestampMillis: Long, altitudeMeters: Double = 10.0) =
        TrackPoint(
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = altitudeMeters,
            timestampMillis = timestampMillis,
            accuracyMeters = 10f,
        )

    @Test
    fun `ticks down and finishes with estimated distance for treadmill sessions`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val steps = listOf(
            SessionStepEntity(planSessionId = 1, orderIndex = 0, stepType = StepType.WALK, durationSeconds = 2, targetSpeedMetersPerSecond = 1.0),
            SessionStepEntity(planSessionId = 1, orderIndex = 1, stepType = StepType.RUN, durationSeconds = 2, targetSpeedMetersPerSecond = 2.0),
        )
        val runRepository = FakeRunRepository()
        val engine = engineWith(steps, dispatcher, runRepository = runRepository)

        engine.start(planSessionId = 1, outdoor = false, shoeId = null)
        runCurrent()
        assertFalse(engine.state.value.isLoading)
        assertEquals(2, engine.state.value.remainingSeconds)

        // WALK(2s) -> RUN(2s) -> finish. Poll rather than assume an exact tick count needed:
        // advanceTimeBy's exact-boundary behavior isn't worth pinning a test to.
        var ticks = 0
        while (!engine.state.value.isFinished && ticks < 10) {
            testScheduler.advanceTimeBy(1_000)
            runCurrent()
            ticks++
        }

        val finalState = engine.state.value
        assertTrue(finalState.isFinished)
        assertEquals(42L, finalState.finishedRunId)

        val recorded = requireNotNull(runRepository.recordedRun)
        // (2s * 1.0 m/s) + (2s * 2.0 m/s) = 6.0m — no GPS fixes on a treadmill session, so this
        // must come from the estimated-distance fallback, not gpsDistanceMeters.
        assertEquals(6.0, recorded.distanceMeters, 0.001)
        assertEquals(4, recorded.durationSeconds)
    }

    @Test
    fun `pausing drops GPS fixes instead of counting drift into the distance`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val steps = listOf(
            SessionStepEntity(planSessionId = 1, orderIndex = 0, stepType = StepType.WALK, durationSeconds = 60),
        )
        val locationTracker = FakeLocationTracker()
        val engine = engineWith(steps, dispatcher, locationTracker = locationTracker)

        engine.start(planSessionId = 1, outdoor = true, shoeId = null)
        runCurrent()

        // ~11m per fix, 3s apart ≈ 3.7 m/s — a real running pace, comfortably under
        // GpsSmoother's default plausible-speed cap (10 m/s) so these aren't rejected as jumps.
        locationTracker.emit(fix(latitude = 40.00000, longitude = -73.0000, timestampMillis = 0))
        runCurrent()
        locationTracker.emit(fix(latitude = 40.00010, longitude = -73.0000, timestampMillis = 3_000))
        runCurrent()

        val distanceBeforePause = engine.state.value.gpsDistanceMeters
        val pointsBeforePause = engine.state.value.trackPoints.size
        assertTrue("expected real distance from two distinct fixes", distanceBeforePause > 0.0)
        assertEquals(2, pointsBeforePause)

        engine.togglePause()
        assertTrue(engine.state.value.isPaused)

        // Drift while "paused" — must not be counted.
        locationTracker.emit(fix(latitude = 40.00020, longitude = -73.0000, timestampMillis = 6_000))
        runCurrent()

        assertEquals(distanceBeforePause, engine.state.value.gpsDistanceMeters, 0.0001)
        assertEquals(pointsBeforePause, engine.state.value.trackPoints.size)
    }

    @Test
    fun `cancel stops the session without recording a run`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val steps = listOf(
            SessionStepEntity(planSessionId = 1, orderIndex = 0, stepType = StepType.WALK, durationSeconds = 30),
        )
        val runRepository = FakeRunRepository()
        val engine = engineWith(steps, dispatcher, runRepository = runRepository)

        engine.start(planSessionId = 1, outdoor = false, shoeId = null)
        runCurrent()

        engine.cancel()

        assertTrue(engine.state.value.isCancelled)
        assertFalse(engine.state.value.isFinished)
        assertNull(runRepository.recordedRun)

        // No further ticks should land after cancel — the tick job was cancelled, not just paused.
        val remainingAtCancel = engine.state.value.remainingSeconds
        testScheduler.advanceTimeBy(5_000)
        runCurrent()
        assertEquals(remainingAtCancel, engine.state.value.remainingSeconds)
    }
}

private class FixedDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
}

private class FakeVoiceCueSpeaker : VoiceCueSpeaker {
    val spoken = mutableListOf<String>()
    override fun speak(text: String) {
        spoken += text
    }
    override fun shutdown() {}
}

private class FakeLocationTracker : LocationTracker {
    var lastKnown: TrackPoint? = null
    // Buffered so emit() never suspends waiting for the engine's collector to be scheduled —
    // the test drives both sides of the flow on the same TestDispatcher.
    private val updates = MutableSharedFlow<TrackPoint>(extraBufferCapacity = 10)

    override suspend fun lastKnownLocation(): TrackPoint? = lastKnown
    override fun observeLocationUpdates(): Flow<TrackPoint> = updates

    suspend fun emit(point: TrackPoint) = updates.emit(point)
}

private class FakeWeatherRepository(private val snapshot: WeatherSnapshot?) : WeatherRepository {
    override suspend fun fetchCurrentConditions(latitude: Double, longitude: Double): WeatherSnapshot? = snapshot
}

private class FakePlanRepository(
    private val steps: List<SessionStepEntity>,
    private val activePlan: PlanEntity? = null,
) : PlanRepository {
    val completedSessionIds = mutableListOf<Long>()

    override fun observeActivePlan(): Flow<PlanEntity?> = flowOf(activePlan)
    override fun observeSessionsForPlan(planId: Long): Flow<List<PlanSessionEntity>> = flowOf(emptyList())
    override suspend fun getSessionOn(planId: Long, date: LocalDate): PlanSessionEntity? = null
    override suspend fun createPlan(plan: PlanEntity, sessions: List<PlanSessionEntity>): Long = 0
    override suspend fun markSessionCompleted(planSessionId: Long) {
        completedSessionIds += planSessionId
    }
    override fun observeStepsForSession(planSessionId: Long): Flow<List<SessionStepEntity>> = flowOf(steps)
    override suspend fun setStepsForSession(planSessionId: Long, steps: List<SessionStepEntity>) {}
}

private class FakeRunRepository : RunRepository {
    var recordedRun: RunSessionEntity? = null

    override fun observeRuns(): Flow<List<RunSessionEntity>> = flowOf(emptyList())
    override fun observeRunsSince(sinceInclusive: java.time.Instant): Flow<List<RunSessionEntity>> = flowOf(emptyList())
    override suspend fun getRun(id: Long): RunSessionEntity? = null
    override suspend fun recordRun(run: RunSessionEntity): Long {
        recordedRun = run
        return 42L
    }
    override suspend fun updateRpe(runId: Long, rpe: Int) {}
}
