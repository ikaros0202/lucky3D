package com.lucky3d.app.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface YunnanAnnouncementDao {
    @Upsert
    suspend fun upsertAll(announcements: List<YunnanAnnouncementEntity>)

    @Query(
        """
        SELECT announcement.*
        FROM yunnan_announcements AS announcement
        INNER JOIN draws AS draw
          ON draw.issue = announcement.issue
         AND draw.drawDate = announcement.drawDate
         AND announcement.winningNumber =
             CAST(draw.hundreds AS TEXT) || CAST(draw.tens AS TEXT) || CAST(draw.ones AS TEXT)
        ORDER BY announcement.issue DESC
        LIMIT 1
        """,
    )
    fun observeLatest(): Flow<YunnanAnnouncementEntity?>

    @Query(
        """
        SELECT announcement.*
        FROM yunnan_announcements AS announcement
        INNER JOIN draws AS draw
          ON draw.issue = announcement.issue
         AND draw.drawDate = announcement.drawDate
         AND announcement.winningNumber =
             CAST(draw.hundreds AS TEXT) || CAST(draw.tens AS TEXT) || CAST(draw.ones AS TEXT)
        WHERE announcement.issue = :issue
        LIMIT 1
        """,
    )
    fun observeByIssue(issue: String): Flow<YunnanAnnouncementEntity?>

    @Query(
        """
        SELECT announcement.*
        FROM yunnan_announcements AS announcement
        INNER JOIN draws AS draw
          ON draw.issue = announcement.issue
         AND draw.drawDate = announcement.drawDate
         AND announcement.winningNumber =
             CAST(draw.hundreds AS TEXT) || CAST(draw.tens AS TEXT) || CAST(draw.ones AS TEXT)
        WHERE announcement.issue = :issue
        LIMIT 1
        """,
    )
    suspend fun byIssue(issue: String): YunnanAnnouncementEntity?

    @Query(
        """
        SELECT announcement.*
        FROM yunnan_announcements AS announcement
        INNER JOIN draws AS draw
          ON draw.issue = announcement.issue
         AND draw.drawDate = announcement.drawDate
         AND announcement.winningNumber =
             CAST(draw.hundreds AS TEXT) || CAST(draw.tens AS TEXT) || CAST(draw.ones AS TEXT)
        ORDER BY announcement.issue DESC
        LIMIT :limit
        """,
    )
    suspend fun recent(limit: Int): List<YunnanAnnouncementEntity>

    @Query("SELECT * FROM draws WHERE issue IN (:issues)")
    suspend fun drawsForValidation(issues: Set<String>): List<DrawEntity>

    @Transaction
    suspend fun commitValidated(announcements: List<YunnanAnnouncementEntity>): Boolean {
        if (announcements.isEmpty()) return false
        val issues = announcements.mapTo(linkedSetOf()) { it.issue }
        if (issues.size != announcements.size) return false
        val drawsByIssue = drawsForValidation(issues).associateBy { it.issue }
        val aligned = announcements.all { announcement ->
            val draw = drawsByIssue[announcement.issue] ?: return@all false
            announcement.drawDate == draw.drawDate &&
                announcement.winningNumber == "${draw.hundreds}${draw.tens}${draw.ones}"
        }
        if (!aligned) return false
        upsertAll(announcements)
        return true
    }
}
