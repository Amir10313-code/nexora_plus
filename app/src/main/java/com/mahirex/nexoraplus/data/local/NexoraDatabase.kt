package com.mahirex.nexoraplus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mahirex.nexoraplus.data.local.dao.AccountDao
import com.mahirex.nexoraplus.data.local.dao.ChecklistDao
import com.mahirex.nexoraplus.data.local.dao.FocusDao
import com.mahirex.nexoraplus.data.local.dao.FortuneDao
import com.mahirex.nexoraplus.data.local.dao.GoalDao
import com.mahirex.nexoraplus.data.local.dao.HabitDao
import com.mahirex.nexoraplus.data.local.dao.OccasionDao
import com.mahirex.nexoraplus.data.local.dao.ScheduleDao
import com.mahirex.nexoraplus.data.local.dao.TaskDao
import com.mahirex.nexoraplus.data.local.dao.UserEventDao
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

@Database(
    entities = [
        AccountEntity::class, TaskEntity::class, GoalEntity::class, HabitEntity::class, HabitLogEntity::class,
        ChecklistEntity::class, ChecklistItemEntity::class, UserEventEntity::class, OccasionEntity::class,
        OccasionDatasetEntity::class, ScheduleSlotEntity::class, ScheduleCellEntity::class,
        SchedulePresetEntity::class, FortuneHistoryEntity::class, FocusSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NexoraDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
    abstract fun habitDao(): HabitDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun userEventDao(): UserEventDao
    abstract fun occasionDao(): OccasionDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun fortuneDao(): FortuneDao
    abstract fun focusDao(): FocusDao

    companion object {
        fun build(context: Context): NexoraDatabase =
            Room.databaseBuilder(context.applicationContext, NexoraDatabase::class.java, "nexora_plus.db").build()
    }
}
