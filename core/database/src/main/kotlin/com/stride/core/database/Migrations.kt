package com.stride.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: StepType gained STRETCH (enum values are stored as TEXT by name, so no schema
 * change needed for that part) and session_steps gained a nullable `label` column for named
 * stretches (e.g. "Calf stretch") — see SessionStepEntity. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE session_steps ADD COLUMN label TEXT DEFAULT NULL")
    }
}
