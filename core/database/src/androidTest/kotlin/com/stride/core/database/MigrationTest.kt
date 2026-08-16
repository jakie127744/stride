package com.stride.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies [MIGRATION_1_2] against the real checked-in v1 schema (schemas/1.json), not just a
 * round trip through Room's own generated SQL — the failure mode this guards against is a
 * migration that "works" in dev because the dev database was never actually at v1, but breaks
 * on a real runner's device that upgraded from an earlier shipped version. See DatabaseModule's
 * comment on why there's no fallbackToDestructiveMigration(): this test is what makes that
 * policy trustworthy instead of just aspirational.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StrideDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_addsNullableLabelColumn_andPreservesExistingRows() {
        val dbName = "migration-test"

        // Create the database at v1 and seed a row the way the real app would have, before
        // `label` existed — this is the exact state a real upgrading device is in.
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO plans (id, track, name, startDate, currentWeek, isActive) " +
                    "VALUES (1, 'BEGINNER', 'Couch to 5K', '2026-01-01', 1, 1)",
            )
            execSQL(
                "INSERT INTO plan_sessions (id, planId, weekNumber, scheduledDate, sessionType, status) " +
                    "VALUES (1, 1, 1, '2026-01-01', 'WALK_RUN_INTERVAL', 'PLANNED')",
            )
            execSQL(
                "INSERT INTO session_steps (id, planSessionId, orderIndex, stepType, durationSeconds) " +
                    "VALUES (1, 1, 0, 'WALK', 240)",
            )
            close()
        }

        // Run the real migration under test, then validate the resulting schema matches what
        // Room expects at v2 (schemas/2.json) — this is the part that actually catches a
        // migration whose SQL is subtly wrong, not just "didn't throw".
        val migrated = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        val cursor = migrated.query("SELECT durationSeconds, label FROM session_steps WHERE id = 1")
        cursor.use {
            assert(it.moveToFirst()) { "Pre-migration row should survive the migration" }
            val durationIndex = it.getColumnIndexOrThrow("durationSeconds")
            val labelIndex = it.getColumnIndexOrThrow("label")
            assert(it.getInt(durationIndex) == 240) { "Existing column data should be untouched" }
            assert(it.isNull(labelIndex)) { "New `label` column should default to NULL for pre-existing rows" }
        }
    }
}
