package com.bugsjkeeee.tempo.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JournalRepository(private val db: TempoDatabase) {
    private val dao = db.dao()

    val entries: Flow<List<JournalEntry>> = dao.observeJournal().map { list -> list.map { it.toDomain() } }

    suspend fun all(): List<JournalEntry> = dao.journal().map { it.toDomain() }

    suspend fun get(id: Long): JournalEntry? = dao.entry(id)?.toDomain()

    /** Сохраняет новую запись или заменяет существующую вместе с подходами; возвращает id. */
    suspend fun save(entry: JournalEntry): Long = db.withTransaction {
        val id = if (entry.id == 0L) dao.insertEntry(entry.toEntity()) else dao.replaceEntry(entry.toEntity())
        dao.deleteSets(id)
        dao.insertSets(entry.copy(id = id).setEntities(id))
        id
    }

    suspend fun delete(id: Long) = dao.deleteEntry(id)
}

data class WeightPoint(val epochDay: Long, val weight: Double)

class WeightRepository(private val dao: TempoDao) {
    val weights: Flow<List<WeightPoint>> = dao.observeWeights().map { list -> list.map { WeightPoint(it.epochDay, it.weight) } }

    /** Одна запись на дату: повторный ввод заменяет значение. */
    suspend fun save(epochDay: Long, weight: Double) = dao.upsertWeight(BodyWeightEntity(epochDay, weight))

    suspend fun delete(epochDay: Long) = dao.deleteWeight(epochDay)
}
