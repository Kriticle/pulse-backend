package com.pulse

import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val artist: String,
    val bpm: Int,
    val audioFile: String,
    val durationSeconds: Int
)

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val category: String,
    val muscleGroup: String,
    val instruction: String
)

@Serializable
data class SegmentDto(
    val id: String,
    val type: String,
    val startTimeSeconds: Int,
    val endTimeSeconds: Int,
    val durationSeconds: Int,
    val exercise: ExerciseDto,
    val song: SongDto? = null,
    val loopCount: Int
)

@Serializable
data class LevelDto(
    val id: String,
    val title: String,
    val difficulty: Int,
    val unlockScoreRequired: Int,
    val segments: List<SegmentDto>
)

@Serializable
data class UserDto(
    val id: String,
    val displayName: String,
    val email: String,
    val activeThemeId: String? = null,
    val activeSkinId: String? = null,
    val totalLevelsCompleted: Int,
    val totalWorkoutSeconds: Int
)

@Serializable
data class CreateAttemptRequest(
    val userId: String,
    val levelId: String,
    val durationSeconds: Int,
    val accuracyPct: Float = 0f
)

@Serializable
data class AttemptResponse(
    val attemptId: String,
    val score: Int,
    val user: UserDto
)

@Serializable
data class BestAttemptDto(
    val levelId: String,
    val score: Int,
    val durationSeconds: Int,
    val accuracyPct: Float,
    val playedAt: Long
)