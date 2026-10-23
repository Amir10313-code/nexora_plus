package com.mahirex.nexoraplus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mahirex.nexoraplus.data.local.entity.AccountEntity
import com.mahirex.nexoraplus.data.local.entity.ChecklistEntity
import com.mahirex.nexoraplus.data.local.entity.ChecklistItemEntity
import com.mahirex.nexoraplus.data.local.entity.FocusSessionEntity
import com.mahirex.nexoraplus.data.local.entity.FortuneHistoryEntity
import com.mahirex.nexoraplus.data.local.entity.GoalEntity
import com.mahirex.nexoraplus.data.local.entity.HabitEntity
import com.mahirex.nexoraplus.data.local.entity.HabitLogEntity
import com.mahirex.nexoraplus.data.local.entity.OccasionDatasetEntity
import com.mahirex.nexoraplus.data.local.entity.OccasionEntity
import com.mahirex.nexoraplus.data.local.entity.ScheduleCellEntity
import com.mahirex.nexoraplus.data.local.entity.SchedulePresetEntity
import com.mahirex.nexoraplus.data.local.entity.ScheduleSlotEntity
import com.mahirex.nexoraplus.data.local.entity.TaskEntity
import com.mahirex.nexoraplus.data.local.entity.UserEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE accountId = :accountId")
    fun observe(accountId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE accountId = :accountId")
    suspend fun getAll(accountId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE accountId = :accountId AND done = 0")
    suspend fun getOpen(accountId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE accountId = :accountId AND title LIKE '%' || :q || '%' ESCAPE '\\' ORDER BY done, id DESC LIMIT 50")
    suspend fun search(accountId: Long, q: String): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE accountId = :accountId")
    fun observe(accountId: Long): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE accountId = :accountId")
    suspend fun getAll(accountId: Long): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE accountId = :accountId AND title LIKE '%' || :q || '%' ESCAPE '\\' ORDER BY done, id DESC LIMIT 50")
    suspend fun search(accountId: Long, q: String): List<GoalEntity>

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE accountId = :accountId")
    fun observe(accountId: Long): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE accountId = :accountId")
    suspend fun getAll(accountId: Long): List<HabitEntity>

    @Query(
        "SELECT l.* FROM habit_logs l INNER JOIN habits h ON h.id = l.habitId WHERE h.accountId = :accountId"
    )
    fun observeLogs(accountId: Long): Flow<List<HabitLogEntity>>

    @Query(
        "SELECT l.* FROM habit_logs l INNER JOIN habits h ON h.id = l.habitId WHERE h.accountId = :accountId"
    )
    suspend fun getLogs(accountId: Long): List<HabitLogEntity>

    @Query("SELECT * FROM habits WHERE accountId = :accountId AND title LIKE '%' || :q || '%' ESCAPE '\\' ORDER BY id DESC LIMIT 50")
    suspend fun search(accountId: Long, q: String): List<HabitEntity>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteLog(habitId: Long, epochDay: Long)

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun logCount(habitId: Long, epochDay: Long): Int
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklists WHERE accountId = :accountId")
    fun observeLists(accountId: Long): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklists WHERE accountId = :accountId")
    suspend fun getLists(accountId: Long): List<ChecklistEntity>

    @Query(
        "SELECT i.* FROM checklist_items i INNER JOIN checklists c ON c.id = i.checklistId WHERE c.accountId = :accountId"
    )
    fun observeItems(accountId: Long): Flow<List<ChecklistItemEntity>>

    @Query(
        "SELECT i.* FROM checklist_items i INNER JOIN checklists c ON c.id = i.checklistId WHERE c.accountId = :accountId"
    )
    suspend fun getItems(accountId: Long): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklists WHERE accountId = :accountId AND title LIKE '%' || :q || '%' ESCAPE '\\' ORDER BY id DESC LIMIT 50")
    suspend fun searchLists(accountId: Long, q: String): List<ChecklistEntity>

    @Query(
        "SELECT i.* FROM checklist_items i INNER JOIN checklists c ON c.id = i.checklistId " +
            "WHERE c.accountId = :accountId AND i.text LIKE '%' || :q || '%' ESCAPE '\\' ORDER BY i.id DESC LIMIT 50"
    )
    suspend fun searchItems(accountId: Long, q: String): List<ChecklistItemEntity>

    @Insert
    suspend fun insertList(list: ChecklistEntity): Long

    @Update
    suspend fun updateList(list: ChecklistEntity)

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Insert
    suspend fun insertItem(item: ChecklistItemEntity): Long

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteItem(id: Long)
}

@Dao
interface UserEventDao {
    @Query("SELECT * FROM user_events WHERE accountId = :accountId")
    fun observe(accountId: Long): Flow<List<UserEventEntity>>

    @Query("SELECT * FROM user_events WHERE accountId = :accountId")
    suspend fun getAll(accountId: Long): List<UserEventEntity>

    @Query("SELECT * FROM user_events WHERE accountId = :accountId AND (title LIKE '%' || :q || '%' ESCAPE '\\' OR note LIKE '%' || :q || '%' ESCAPE '\\') ORDER BY id DESC LIMIT 50")
    suspend fun search(accountId: Long, q: String): List<UserEventEntity>

    @Insert
    suspend fun insert(event: UserEventEntity): Long

    @Update
    suspend fun update(event: UserEventEntity)

    @Query("DELETE FROM user_events WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
abstract class OccasionDao {
    @Query("SELECT * FROM occasions WHERE regionId IN (:regionIds)")
    abstract fun observe(regionIds: List<String>): Flow<List<OccasionEntity>>

    @Query("SELECT * FROM occasions WHERE regionId = :regionId")
    abstract suspend fun getForRegion(regionId: String): List<OccasionEntity>

    @Query("SELECT * FROM occasion_datasets WHERE regionId = :regionId")
    abstract suspend fun getDataset(regionId: String): OccasionDatasetEntity?

    @Query("SELECT * FROM occasion_datasets")
    abstract fun observeDatasets(): Flow<List<OccasionDatasetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertDataset(dataset: OccasionDatasetEntity)

    @Insert
    abstract suspend fun insertAll(items: List<OccasionEntity>)

    @Query("DELETE FROM occasions WHERE regionId = :regionId")
    abstract suspend fun deleteRegion(regionId: String)

    /** Replaces a whole region atomically: either everything is swapped or nothing changes. */
    @Transaction
    open suspend fun replaceRegion(regionId: String, items: List<OccasionEntity>, dataset: OccasionDatasetEntity) {
        deleteRegion(regionId)
        insertAll(items)
        upsertDataset(dataset)
    }
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_slots WHERE accountId = :accountId ORDER BY orderIndex, id")
    fun observeSlots(accountId: Long): Flow<List<ScheduleSlotEntity>>

    @Query("SELECT * FROM schedule_slots WHERE accountId = :accountId ORDER BY orderIndex, id")
    suspend fun getSlots(accountId: Long): List<ScheduleSlotEntity>

    @Query("SELECT * FROM schedule_cells WHERE accountId = :accountId")
    fun observeCells(accountId: Long): Flow<List<ScheduleCellEntity>>

    @Query("SELECT * FROM schedule_cells WHERE accountId = :accountId")
    suspend fun getCells(accountId: Long): List<ScheduleCellEntity>

    @Query("SELECT * FROM schedule_presets WHERE accountId = :accountId")
    suspend fun getPreset(accountId: Long): SchedulePresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreset(preset: SchedulePresetEntity)

    @Insert
    suspend fun insertSlot(slot: ScheduleSlotEntity): Long

    @Update
    suspend fun updateSlot(slot: ScheduleSlotEntity)

    @Query("DELETE FROM schedule_slots WHERE id = :id")
    suspend fun deleteSlot(id: Long)

    @Query("DELETE FROM schedule_slots WHERE accountId = :accountId")
    suspend fun deleteAllSlots(accountId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCell(cell: ScheduleCellEntity): Long

    @Query("DELETE FROM schedule_cells WHERE slotId = :slotId AND dayOfWeek = :dayOfWeek")
    suspend fun deleteCell(slotId: Long, dayOfWeek: Int)

    @Query("SELECT COUNT(*) FROM schedule_slots WHERE accountId = :accountId")
    suspend fun slotCount(accountId: Long): Int
}

@Dao
interface FortuneDao {
    @Query("SELECT * FROM fortune_history WHERE accountId = :accountId ORDER BY epochDay DESC LIMIT 60")
    fun observeRecent(accountId: Long): Flow<List<FortuneHistoryEntity>>

    @Query("SELECT * FROM fortune_history WHERE accountId = :accountId")
    suspend fun getAll(accountId: Long): List<FortuneHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: FortuneHistoryEntity): Long
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions WHERE accountId = :accountId ORDER BY startedAt DESC")
    fun observe(accountId: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE accountId = :accountId")
    suspend fun getAll(accountId: Long): List<FocusSessionEntity>

    @Insert
    suspend fun insert(session: FocusSessionEntity): Long
}
