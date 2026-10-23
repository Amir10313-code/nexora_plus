package com.mahirex.nexoraplus.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val createdAt: Long,
    val sortOrder: Int
)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("accountId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val title: String,
    val note: String = "",
    /** 0 = low, 1 = normal, 2 = high */
    val priority: Int = 1,
    val dueEpochDay: Long? = null,
    val pinned: Boolean = false,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val createdAt: Long
)

@Entity(
    tableName = "goals",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("accountId")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val title: String,
    val note: String = "",
    val targetEpochDay: Long? = null,
    /** 0..100 */
    val progress: Int = 0,
    val pinned: Boolean = false,
    val done: Boolean = false,
    val createdAt: Long
)

@Entity(
    tableName = "habits",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("accountId")]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val title: String,
    val colorArgb: Int,
    val pinned: Boolean = false,
    val createdAt: Long
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["habitId", "epochDay"], unique = true)]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val epochDay: Long
)

@Entity(
    tableName = "checklists",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("accountId")]
)
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val title: String,
    val colorArgb: Int,
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(entity = ChecklistEntity::class, parentColumns = ["id"], childColumns = ["checklistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("checklistId")]
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checklistId: Long,
    val text: String,
    val done: Boolean = false,
    val sortOrder: Int = 0
)

/** Occasions created by the user. They live in their own table and are never touched by online updates. */
@Entity(
    tableName = "user_events",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("accountId")]
)
data class UserEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val title: String,
    val note: String = "",
    /** "G", "J" or "H": the calendar the date was entered in. */
    val calendarType: String,
    /** Required when [repeatYearly] is false. */
    val year: Int? = null,
    val month: Int,
    val day: Int,
    val repeatYearly: Boolean = false,
    val createdAt: Long
)

/** Cache of bundled / downloaded occasions. */
@Entity(
    tableName = "occasions",
    indices = [Index(value = ["regionId", "defId"], unique = true), Index("regionId")]
)
data class OccasionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val regionId: String,
    val defId: String,
    /** "FIXED", "NTH_WEEKDAY" or "EASTER" */
    val ruleType: String,
    val calendarType: String?,
    val month: Int,
    val day: Int,
    val clamp: Boolean,
    val weekday: Int,
    val nth: Int,
    val offsetDays: Int,
    val category: String,
    val holiday: Boolean,
    /** JSON object: language -> title */
    val titlesJson: String,
    val verified: Boolean,
    val datasetVersion: Int
)

@Entity(tableName = "occasion_datasets")
data class OccasionDatasetEntity(
    @PrimaryKey val regionId: String,
    val appliedVersion: Int,
    /** "BUNDLED" or "ONLINE" */
    val source: String,
    val lastCheckedAt: Long?,
    val lastSuccessAt: Long?,
    /** "OK", "NOT_MODIFIED", "ERROR", "INVALID", "NOT_CONFIGURED", "OLDER" */
    val status: String,
    val message: String?,
    val etag: String?
)

@Entity(
    tableName = "schedule_slots",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("accountId")]
)
data class ScheduleSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val orderIndex: Int,
    val startMinute: Int,
    val endMinute: Int,
    val label: String,
    val colorArgb: Int
)

@Entity(
    tableName = "schedule_cells",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ScheduleSlotEntity::class, parentColumns = ["id"], childColumns = ["slotId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("accountId"), Index(value = ["slotId", "dayOfWeek"], unique = true)]
)
data class ScheduleCellEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val slotId: Long,
    /** ISO day of week 1 (Monday) .. 7 (Sunday) */
    val dayOfWeek: Int,
    val text: String,
    val colorArgb: Int? = null
)

@Entity(
    tableName = "schedule_presets",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)]
)
data class SchedulePresetEntity(
    @PrimaryKey val accountId: Long,
    val layoutJson: String,
    val updatedAt: Long
)

@Entity(
    tableName = "fortune_history",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["accountId", "epochDay"], unique = true)]
)
data class FortuneHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val epochDay: Long,
    val fortuneIndex: Int
)

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("accountId")]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val startedAt: Long,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val dndUsed: Boolean,
    val completed: Boolean
)
