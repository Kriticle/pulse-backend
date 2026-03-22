package com.pulse

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val url = environment.config.property("postgres.url").getString()
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()

    val database = Database.connect(
        url = url,
        driver = "org.postgresql.Driver",
        user = user,
        password = password
    )
    transaction(database) {
        SchemaUtils.create(
            UserTable,
            LevelTable,
            SongTable,
            ExerciseTable,
            SegmentTable,
            LevelSegmentTable,
            LevelAttemptTable,
            UnlockedLevelTable,
            ItemTable,
            UnlockedItemTable,
            LevelCategoryTable
        )
    }
}