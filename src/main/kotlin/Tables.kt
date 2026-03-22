package com.pulse

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
    val id = varchar("id", 36)
    val authProviderId = varchar("auth_provider_id", 128).uniqueIndex()
    val email = varchar("email", 256).uniqueIndex()
    val displayName = varchar("display_name", 128)
    val activeThemeId = varchar("active_theme_id", 36).nullable()
    val activeSkinId = varchar("active_skin_id", 36).nullable()
    val totalLevelsCompleted = integer("total_levels_completed").default(0)
    val totalWorkoutSeconds = integer("total_workout_seconds").default(0)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object LevelTable : Table("levels") {
    val id = varchar("id", 36)
    val title = varchar("title", 256)
    val difficulty = integer("difficulty")
    val themeId = varchar("theme_id", 36).nullable()
    val unlockScoreRequired = integer("unlock_score_required").default(0)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object SegmentTable : Table("segments") {
    val id = varchar("id", 36)
    val exerciseId = varchar("exercise_id", 36).references(ExerciseTable.id)
    val songId = varchar("song_id", 36).references(SongTable.id).nullable()
    val startTimeSeconds = integer("start_time_seconds").default(0)
    val endTimeSeconds = integer("end_time_seconds")
    val type = varchar("type", 32)
    override val primaryKey = PrimaryKey(id)
}

object LevelSegmentTable : Table("level_segments") {
    val id = varchar("id", 36)
    val levelId = varchar("level_id", 36).references(LevelTable.id)
    val segmentId = varchar("segment_id", 36).references(SegmentTable.id)
    val orderIndex = integer("order_index")
    val loopCount = integer("loop_count").default(1)
    override val primaryKey = PrimaryKey(id)
}

object SongTable : Table("songs") {
    val id = varchar("id", 36)
    val title = varchar("title", 256)
    val artist = varchar("artist", 256)
    val bpm = integer("bpm")
    val audioFile = varchar("audio_file", 512)
    val durationSeconds = integer("duration_seconds")
    override val primaryKey = PrimaryKey(id)
}

object ExerciseTable : Table("exercises") {
    val id = varchar("id", 36)
    val name = varchar("name", 256)
    val category = varchar("category", 64)
    val muscleGroup = varchar("muscle_group", 128)
    val instruction = text("instruction")
    val animationFile = varchar("animation_file", 256).nullable()
    override val primaryKey = PrimaryKey(id)
}

object LevelAttemptTable : Table("level_attempts") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UserTable.id)
    val levelId = varchar("level_id", 36).references(LevelTable.id)
    val score = integer("score")
    val durationSeconds = integer("duration_seconds")
    val accuracyPct = float("accuracy_pct").default(0f)
    val playedAt = long("played_at")
    override val primaryKey = PrimaryKey(id)
}

object UnlockedLevelTable : Table("unlocked_levels") {
    val userId = varchar("user_id", 36).references(UserTable.id)
    val levelId = varchar("level_id", 36).references(LevelTable.id)
    val unlockedAt = long("unlocked_at")
    override val primaryKey = PrimaryKey(userId, levelId)
}

object ItemTable : Table("items") {
    val id = varchar("id", 36)
    val type = varchar("type", 64)
    val name = varchar("name", 256)
    val unlockScoreRequired = integer("unlock_score_required").default(0)
    override val primaryKey = PrimaryKey(id)
}

object UnlockedItemTable : Table("unlocked_items") {
    val userId = varchar("user_id", 36).references(UserTable.id)
    val itemId = varchar("item_id", 36).references(ItemTable.id)
    val unlockedAt = long("unlocked_at")
    override val primaryKey = PrimaryKey(userId, itemId)
}

object LevelCategoryTable : Table("level_categories") {
    val levelId = varchar("level_id", 36).references(LevelTable.id)
    val category = varchar("category", 64)
    override val primaryKey = PrimaryKey(levelId, category)
}