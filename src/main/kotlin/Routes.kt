package com.pulse

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Application.configureRouting() {
    routing {

        get("/best-attempts") {
            val userId = call.request.queryParameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "userId required")
            val bestAttempts = transaction {
                LevelAttemptTable.selectAll()
                    .where { LevelAttemptTable.userId eq userId }
                    .groupBy { it[LevelAttemptTable.levelId] }
                    .mapValues { (_, attempts) ->
                        attempts.maxBy { it[LevelAttemptTable.score] }
                    }
                    .map { (levelId, row) ->
                        BestAttemptDto(
                            levelId = levelId,
                            score = row[LevelAttemptTable.score],
                            durationSeconds = row[LevelAttemptTable.durationSeconds],
                            accuracyPct = row[LevelAttemptTable.accuracyPct],
                            playedAt = row[LevelAttemptTable.playedAt]
                        )
                    }
            }
            call.respond(HttpStatusCode.OK, bestAttempts)
        }

        get("/users/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val user = transaction {
                UserTable.selectAll()
                    .where { UserTable.id eq id }
                    .singleOrNull()
                    ?.let { row ->
                        UserDto(
                            id = row[UserTable.id],
                            displayName = row[UserTable.displayName],
                            email = row[UserTable.email],
                            activeThemeId = row[UserTable.activeThemeId],
                            activeSkinId = row[UserTable.activeSkinId],
                            totalLevelsCompleted = row[UserTable.totalLevelsCompleted],
                            totalWorkoutSeconds = row[UserTable.totalWorkoutSeconds]
                        )
                    }
            }
            if (user == null) call.respond(HttpStatusCode.NotFound)
            else call.respond(HttpStatusCode.OK, user)
        }

        get("/levels") {
            val levels = transaction {
                LevelTable.selectAll().map { levelRow ->
                    buildLevelDto(levelRow)
                }
            }
            call.respond(HttpStatusCode.OK, levels)
        }

        get("/levels/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val level = transaction {
                LevelTable.selectAll()
                    .where { LevelTable.id eq id }
                    .singleOrNull()
                    ?.let { buildLevelDto(it) }
            }
            if (level == null) call.respond(HttpStatusCode.NotFound)
            else call.respond(HttpStatusCode.OK, level)
        }

        post("/attempts") {
            val request = call.receive<CreateAttemptRequest>()
            val response = transaction {
                val attemptId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                LevelAttemptTable.insert {
                    it[id] = attemptId
                    it[userId] = request.userId
                    it[levelId] = request.levelId
                    it[score] = request.durationSeconds
                    it[durationSeconds] = request.durationSeconds
                    it[accuracyPct] = request.accuracyPct
                    it[playedAt] = now
                }

                UserTable.update({ UserTable.id eq request.userId }) {
                    with(SqlExpressionBuilder) {
                        it.update(totalWorkoutSeconds, totalWorkoutSeconds + request.durationSeconds)
                        it.update(totalLevelsCompleted, totalLevelsCompleted + 1)
                    }
                    it[updatedAt] = now
                }

                val userRow = UserTable.selectAll()
                    .where { UserTable.id eq request.userId }
                    .single()

                AttemptResponse(
                    attemptId = attemptId,
                    score = request.durationSeconds,
                    user = UserDto(
                        id = userRow[UserTable.id],
                        displayName = userRow[UserTable.displayName],
                        email = userRow[UserTable.email],
                        activeThemeId = userRow[UserTable.activeThemeId],
                        activeSkinId = userRow[UserTable.activeSkinId],
                        totalLevelsCompleted = userRow[UserTable.totalLevelsCompleted],
                        totalWorkoutSeconds = userRow[UserTable.totalWorkoutSeconds]
                    )
                )
            }
            call.respond(HttpStatusCode.Created, response)
        }


    }
}

// TODO: implement auto difficulty calculation
// difficulty = sum((bpm/60) * duration * exerciseWeight) / activeSegmentCount
// exerciseWeight: cardio=1.0, pilates=0.7, rest=0.0
fun calculateLevelDifficulty(segments: List<SegmentDto>, bpmMap: Map<String, Int>): Int {
    return 1 // placeholder — manual difficulty for now
}

fun buildLevelDto(levelRow: ResultRow): LevelDto {
    val segments = LevelSegmentTable.selectAll()
        .where { LevelSegmentTable.levelId eq levelRow[LevelTable.id] }
        .orderBy(LevelSegmentTable.orderIndex)
        .map { levelSegmentRow ->
            val segmentRow = SegmentTable.selectAll()
                .where { SegmentTable.id eq levelSegmentRow[LevelSegmentTable.segmentId] }
                .single()
            val exercise = ExerciseTable.selectAll()
                .where { ExerciseTable.id eq segmentRow[SegmentTable.exerciseId] }
                .single()
            val song = segmentRow[SegmentTable.songId]?.let { songId ->
                SongTable.selectAll()
                    .where { SongTable.id eq songId }
                    .singleOrNull()
                    ?.let { songRow ->
                        SongDto(
                            id = songRow[SongTable.id],
                            title = songRow[SongTable.title],
                            artist = songRow[SongTable.artist],
                            bpm = songRow[SongTable.bpm],
                            audioFile = songRow[SongTable.audioFile],
                            durationSeconds = songRow[SongTable.durationSeconds]
                        )
                    }
            }
            SegmentDto(
                id = segmentRow[SegmentTable.id],
                type = segmentRow[SegmentTable.type],
                startTimeSeconds = segmentRow[SegmentTable.startTimeSeconds],
                endTimeSeconds = segmentRow[SegmentTable.endTimeSeconds],
                durationSeconds = segmentRow[SegmentTable.endTimeSeconds] - segmentRow[SegmentTable.startTimeSeconds],
                exercise = ExerciseDto(
                    id = exercise[ExerciseTable.id],
                    name = exercise[ExerciseTable.name],
                    category = exercise[ExerciseTable.category],
                    muscleGroup = exercise[ExerciseTable.muscleGroup],
                    instruction = exercise[ExerciseTable.instruction],
                    animationFile = exercise[ExerciseTable.animationFile]

                ),
                song = song,
                loopCount = levelSegmentRow[LevelSegmentTable.loopCount]
            )
        }
    return LevelDto(
        id = levelRow[LevelTable.id],
        title = levelRow[LevelTable.title],
        difficulty = levelRow[LevelTable.difficulty],
        unlockScoreRequired = levelRow[LevelTable.unlockScoreRequired],
        segments = segments
    )


}