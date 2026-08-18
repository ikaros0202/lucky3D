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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `schemes` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''",
        )
        connection.execSQL(
            "ALTER TABLE `schemes` ADD COLUMN `observationWindow` INTEGER NOT NULL DEFAULT 30",
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `trial_numbers` (
                `issue` TEXT NOT NULL,
                `number` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `sourcePageUrl` TEXT NOT NULL,
                `sourceLocalDate` TEXT NOT NULL,
                `fetchedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`issue`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `caibao_documents` (
                `issue` TEXT NOT NULL,
                `edition` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `sourcePageUrl` TEXT NOT NULL,
                `imageUrl` TEXT NOT NULL,
                `localFileName` TEXT NOT NULL,
                `sha256` TEXT NOT NULL,
                `mimeType` TEXT NOT NULL,
                `width` INTEGER NOT NULL,
                `height` INTEGER NOT NULL,
                `cachedLocalDate` TEXT NOT NULL,
                `fetchedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`issue`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `live_content_refresh_metadata` (
                `contentType` TEXT NOT NULL,
                `attemptLocalDate` TEXT,
                `autoAttemptCount` INTEGER NOT NULL,
                `lastAttemptEpochMillis` INTEGER,
                `lastSuccessLocalDate` TEXT,
                `lastSuccessEpochMillis` INTEGER,
                `nextAllowedAutoAttemptEpochMillis` INTEGER,
                `lastFailureType` TEXT,
                PRIMARY KEY(`contentType`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `draws` ADD COLUMN `salesAmountYuan` INTEGER",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `yunnan_announcements` (
                `issue` TEXT NOT NULL,
                `drawDate` TEXT NOT NULL,
                `winningNumber` TEXT NOT NULL,
                `salesAmountYuan` INTEGER NOT NULL,
                `winningTotalYuan` INTEGER NOT NULL,
                `playsJson` TEXT NOT NULL,
                `redemptionDeadline` TEXT,
                `sourceUpdatedAt` TEXT NOT NULL,
                `fetchedAtEpochMillis` INTEGER NOT NULL,
                `fingerprint` TEXT NOT NULL,
                PRIMARY KEY(`issue`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_yunnan_announcements_drawDate` ON `yunnan_announcements` (`drawDate`)",
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `yunnan_announcements` ADD COLUMN `prizePoolBalanceFen` INTEGER",
        )
    }
}
