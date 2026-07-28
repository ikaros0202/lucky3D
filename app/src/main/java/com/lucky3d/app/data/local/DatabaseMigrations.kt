package com.lucky3d.app.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `templates` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `playType` TEXT NOT NULL,
                `conditionsJson` TEXT NOT NULL,
                `conditionsSchemaVersion` INTEGER NOT NULL,
                `observationWindow` INTEGER NOT NULL,
                `ruleVersion` INTEGER NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `schemes` (
                `id` TEXT NOT NULL,
                `issue` TEXT NOT NULL,
                `templateId` TEXT,
                `playType` TEXT NOT NULL,
                `conditionsJson` TEXT NOT NULL,
                `conditionsSchemaVersion` INTEGER NOT NULL,
                `candidateNumbersJson` TEXT NOT NULL,
                `betCount` INTEGER NOT NULL,
                `multiplier` INTEGER NOT NULL,
                `amountYuan` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `ruleVersion` INTEGER NOT NULL,
                `isDrawn` INTEGER NOT NULL,
                `copiedFromSchemeId` TEXT,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_schemes_issue` ON `schemes` (`issue`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_schemes_templateId` ON `schemes` (`templateId`)")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_schemes_copiedFromSchemeId` ON `schemes` (`copiedFromSchemeId`)",
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `replays` (
                `schemeId` TEXT NOT NULL,
                `issue` TEXT NOT NULL,
                `schemeFingerprint` TEXT NOT NULL,
                `officialFingerprint` TEXT NOT NULL,
                `winningNumber` TEXT NOT NULL,
                `covered` INTEGER NOT NULL,
                `matchedCandidate` TEXT,
                `ruleVersion` INTEGER NOT NULL,
                `revision` INTEGER NOT NULL,
                `calculatedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`schemeId`),
                FOREIGN KEY(`schemeId`) REFERENCES `schemes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_replays_issue` ON `replays` (`issue`)")
    }
}
