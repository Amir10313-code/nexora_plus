package com.mahirex.nexoraplus.data.repo

import com.mahirex.nexoraplus.core.Palette
import com.mahirex.nexoraplus.data.local.NexoraDatabase
import com.mahirex.nexoraplus.data.local.entity.AccountEntity
import com.mahirex.nexoraplus.data.local.entity.ChecklistEntity
import com.mahirex.nexoraplus.data.local.entity.ChecklistItemEntity
import com.mahirex.nexoraplus.data.local.entity.FocusSessionEntity
import com.mahirex.nexoraplus.data.local.entity.FortuneHistoryEntity
import com.mahirex.nexoraplus.data.local.entity.GoalEntity
import com.mahirex.nexoraplus.data.local.entity.HabitEntity
import com.mahirex.nexoraplus.data.local.entity.HabitLogEntity
import com.mahirex.nexoraplus.data.local.entity.ScheduleCellEntity
import com.mahirex.nexoraplus.data.local.entity.SchedulePresetEntity
import com.mahirex.nexoraplus.data.local.entity.ScheduleSlotEntity
import com.mahirex.nexoraplus.data.local.entity.TaskEntity
import com.mahirex.nexoraplus.data.local.entity.UserEventEntity
import com.mahirex.nexoraplus.data.prefs.SettingsStore
import com.mahirex.nexoraplus.occasions.OccasionDef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

// ------------------------------------------------------------------------------------------------
// Accounts
// ------------------------------------------------------------------------------------------------

class AccountRepository(private val db: NexoraDatabase, private val settings: SettingsStore) {
    private val dao get() = db.accountDao()

    val accounts: Flow<List<AccountEntity>> = dao.observeAll()

    /** The selected account; falls back to the first one when the stored id no longer exists. */
    val activeAccountId: Flow<Long> = combine(
        accounts,
        settings.settings.map { it.activeAccountId }.distinctUntilChanged()
    ) { list, stored ->
        if (list.any { it.id == stored }) stored else list.firstOrNull()?.id ?: 0L
    }.distinctUntilChanged()

    val activeAccount: Flow<AccountEntity?> = combine(accounts, activeAccountId) { list, id ->
        list.firstOrNull { it.id == id }
    }

    suspend fun ensureDefault(defaultName: String) {
        if (dao.count() == 0) {
            val id = createInternal(defaultName, Palette.at(0))
            settings.setActiveAccount(id)
        }
    }

    /** Returns the new id, or null when the limit of [MAX_ACCOUNTS] is reached. */
    suspend fun create(name: String, colorArgb: Int): Long? {
        if (dao.count() >= MAX_ACCOUNTS) return null
        return createInternal(name.trim().ifEmpty { "—" }, colorArgb)
    }

    private suspend fun createInternal(name: String, colorArgb: Int): Long {
        val order = dao.count()
        val id = dao.insert(AccountEntity(name = name, colorArgb = colorArgb, createdAt = System.currentTimeMillis(), sortOrder = order))
        ScheduleDefaults.insertFactory(db.scheduleDao(), id)
        return id
    }

    suspend fun update(account: AccountEntity) = dao.update(account)

    suspend fun setActive(id: Long) = settings.setActiveAccount(id)

    /** The last remaining account cannot be deleted. Returns false in that case. */
    suspend fun delete(id: Long): Boolean {
        val all = dao.getAll()
        if (all.size <= 1 || all.none { it.id == id }) return false
        dao.delete(id)
        val remaining = all.filter { it.id != id }
        if (settings.current().activeAccountId == id) settings.setActiveAccount(remaining.first().id)
        return true
    }

    companion object {
        const val MAX_ACCOUNTS = 3
    }
}

// ------------------------------------------------------------------------------------------------
// Tasks, goals, habits
// ------------------------------------------------------------------------------------------------

class PlannerRepository(private val db: NexoraDatabase) {
    fun tasks(accountId: Long): Flow<List<TaskEntity>> = db.taskDao().observe(accountId)
    fun goals(accountId: Long): Flow<List<GoalEntity>> = db.goalDao().observe(accountId)
    fun habits(accountId: Long): Flow<List<HabitEntity>> = db.habitDao().observe(accountId)
    fun habitLogs(accountId: Long): Flow<List<HabitLogEntity>> = db.habitDao().observeLogs(accountId)

    suspend fun saveTask(task: TaskEntity) {
        if (task.id == 0L) db.taskDao().insert(task) else db.taskDao().update(task)
    }

    suspend fun setTaskDone(task: TaskEntity, done: Boolean) {
        db.taskDao().update(task.copy(done = done, doneAt = if (done) System.currentTimeMillis() else null))
    }

    suspend fun deleteTask(id: Long) = db.taskDao().delete(id)

    suspend fun saveGoal(goal: GoalEntity) {
        if (goal.id == 0L) db.goalDao().insert(goal) else db.goalDao().update(goal)
    }

    suspend fun deleteGoal(id: Long) = db.goalDao().delete(id)

    suspend fun saveHabit(habit: HabitEntity) {
        if (habit.id == 0L) db.habitDao().insert(habit) else db.habitDao().update(habit)
    }

    suspend fun deleteHabit(id: Long) = db.habitDao().delete(id)

    /** Marks / unmarks a habit for one day. */
    suspend fun toggleHabit(habitId: Long, epochDay: Long) {
        val dao = db.habitDao()
        if (dao.logCount(habitId, epochDay) > 0) dao.deleteLog(habitId, epochDay)
        else dao.insertLog(HabitLogEntity(habitId = habitId, epochDay = epochDay))
    }
}

// ------------------------------------------------------------------------------------------------
// Checklists
// ------------------------------------------------------------------------------------------------

class ChecklistRepository(private val db: NexoraDatabase) {
    fun lists(accountId: Long): Flow<List<ChecklistEntity>> = db.checklistDao().observeLists(accountId)
    fun items(accountId: Long): Flow<List<ChecklistItemEntity>> = db.checklistDao().observeItems(accountId)

    suspend fun saveList(list: ChecklistEntity) {
        if (list.id == 0L) db.checklistDao().insertList(list) else db.checklistDao().updateList(list)
    }

    suspend fun deleteList(id: Long) = db.checklistDao().deleteList(id)

    suspend fun addItem(listId: Long, text: String, sortOrder: Int) {
        db.checklistDao().insertItem(ChecklistItemEntity(checklistId = listId, text = text.trim(), sortOrder = sortOrder))
    }

    suspend fun setItemDone(item: ChecklistItemEntity, done: Boolean) = db.checklistDao().updateItem(item.copy(done = done))
    suspend fun deleteItem(id: Long) = db.checklistDao().deleteItem(id)
}

// ------------------------------------------------------------------------------------------------
// Calendar: occasions (cache) and user events
// ------------------------------------------------------------------------------------------------

class CalendarRepository(private val db: NexoraDatabase) {
    fun occasionDefs(regionIds: List<String>): Flow<List<OccasionDef>> =
        db.occasionDao().observe(regionIds).map { rows -> rows.mapNotNull { OccasionMapper.toDef(it) } }

    fun datasets() = db.occasionDao().observeDatasets()

    fun userEvents(accountId: Long): Flow<List<UserEventEntity>> = db.userEventDao().observe(accountId)

    suspend fun saveUserEvent(event: UserEventEntity) {
        if (event.id == 0L) db.userEventDao().insert(event) else db.userEventDao().update(event)
    }

    suspend fun deleteUserEvent(id: Long) = db.userEventDao().delete(id)
}

// ------------------------------------------------------------------------------------------------
// Weekly schedule
// ------------------------------------------------------------------------------------------------

object ScheduleDefaults {
    /** Six two-hour blocks from 08:00 to 20:00 without names. */
    suspend fun insertFactory(dao: com.mahirex.nexoraplus.data.local.dao.ScheduleDao, accountId: Long) {
        for (i in 0 until 6) {
            dao.insertSlot(
                ScheduleSlotEntity(
                    accountId = accountId, orderIndex = i,
                    startMinute = 8 * 60 + i * 120, endMinute = 8 * 60 + (i + 1) * 120,
                    label = "", colorArgb = Palette.at(i)
                )
            )
        }
    }
}

class ScheduleRepository(private val db: NexoraDatabase) {
    private val dao get() = db.scheduleDao()

    fun slots(accountId: Long): Flow<List<ScheduleSlotEntity>> = dao.observeSlots(accountId)
    fun cells(accountId: Long): Flow<List<ScheduleCellEntity>> = dao.observeCells(accountId)

    suspend fun addSlot(accountId: Long, existing: List<ScheduleSlotEntity>) {
        val last = existing.maxByOrNull { it.endMinute }
        val start = (last?.endMinute ?: 8 * 60).coerceAtMost(23 * 60)
        val end = (start + 60).coerceAtMost(24 * 60)
        dao.insertSlot(
            ScheduleSlotEntity(
                accountId = accountId, orderIndex = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1,
                startMinute = start, endMinute = end, label = "", colorArgb = Palette.at(existing.size)
            )
        )
    }

    suspend fun updateSlot(slot: ScheduleSlotEntity) = dao.updateSlot(slot)

    suspend fun deleteSlot(id: Long) = dao.deleteSlot(id)

    /** Swaps the order of the slot with its neighbour ([delta] = -1 up, +1 down). */
    suspend fun moveSlot(slots: List<ScheduleSlotEntity>, slotId: Long, delta: Int) {
        val ordered = slots.sortedWith(compareBy({ it.orderIndex }, { it.id })).toMutableList()
        val index = ordered.indexOfFirst { it.id == slotId }
        val target = index + delta
        if (index < 0 || target !in ordered.indices) return
        java.util.Collections.swap(ordered, index, target)
        ordered.forEachIndexed { i, s -> if (s.orderIndex != i) dao.updateSlot(s.copy(orderIndex = i)) }
    }

    /** Sets or clears (blank text and no colour) the content of one cell. */
    suspend fun setCell(accountId: Long, slotId: Long, dayOfWeek: Int, text: String, colorArgb: Int?) {
        if (text.isBlank() && colorArgb == null) dao.deleteCell(slotId, dayOfWeek)
        else dao.upsertCell(
            ScheduleCellEntity(accountId = accountId, slotId = slotId, dayOfWeek = dayOfWeek, text = text.trim(), colorArgb = colorArgb)
        )
    }

    /** Stores the current block layout (and week start) as the account's default. */
    suspend fun savePreset(accountId: Long, weekStart: String) {
        val slots = dao.getSlots(accountId)
        val arr = JSONArray()
        slots.forEach {
            arr.put(
                JSONObject().put("start", it.startMinute).put("end", it.endMinute)
                    .put("label", it.label).put("color", it.colorArgb)
            )
        }
        val json = JSONObject().put("weekStart", weekStart).put("slots", arr).toString()
        dao.upsertPreset(SchedulePresetEntity(accountId, json, System.currentTimeMillis()))
    }

    /**
     * Restores the saved default layout (or the factory layout when none was saved). All cell contents are
     * cleared with their blocks. Returns the week start stored in the preset, if any.
     */
    suspend fun resetToDefault(accountId: Long): String? {
        val preset = dao.getPreset(accountId)
        dao.deleteAllSlots(accountId)
        if (preset == null) {
            ScheduleDefaults.insertFactory(dao, accountId)
            return null
        }
        return try {
            val root = JSONObject(preset.layoutJson)
            val arr = root.getJSONArray("slots")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertSlot(
                    ScheduleSlotEntity(
                        accountId = accountId, orderIndex = i, startMinute = o.getInt("start"), endMinute = o.getInt("end"),
                        label = o.optString("label", ""), colorArgb = o.optInt("color", Palette.at(i))
                    )
                )
            }
            root.optString("weekStart").takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            if (dao.slotCount(accountId) == 0) ScheduleDefaults.insertFactory(dao, accountId)
            null
        }
    }

    suspend fun hasPreset(accountId: Long): Boolean = dao.getPreset(accountId) != null
}

// ------------------------------------------------------------------------------------------------
// Fortune
// ------------------------------------------------------------------------------------------------

class FortuneRepository(private val db: NexoraDatabase) {
    fun history(accountId: Long): Flow<List<FortuneHistoryEntity>> = db.fortuneDao().observeRecent(accountId)

    suspend fun record(accountId: Long, epochDay: Long, index: Int) {
        db.fortuneDao().insert(FortuneHistoryEntity(accountId = accountId, epochDay = epochDay, fortuneIndex = index))
    }
}

// ------------------------------------------------------------------------------------------------
// Search
// ------------------------------------------------------------------------------------------------

enum class SearchKind { TASK, GOAL, HABIT, LIST, LIST_ITEM, EVENT }

data class SearchHit(val kind: SearchKind, val id: Long, val title: String, val detail: String = "")

class SearchRepository(private val db: NexoraDatabase) {
    suspend fun search(accountId: Long, query: String): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty() || accountId == 0L) return emptyList()
        val like = TextUtil.escapeLike(q)
        val hits = ArrayList<SearchHit>()
        db.taskDao().search(accountId, like).forEach { hits.add(SearchHit(SearchKind.TASK, it.id, it.title, it.note)) }
        db.goalDao().search(accountId, like).forEach { hits.add(SearchHit(SearchKind.GOAL, it.id, it.title, it.note)) }
        db.habitDao().search(accountId, like).forEach { hits.add(SearchHit(SearchKind.HABIT, it.id, it.title)) }
        db.checklistDao().searchLists(accountId, like).forEach { hits.add(SearchHit(SearchKind.LIST, it.id, it.title)) }
        db.checklistDao().searchItems(accountId, like).forEach { hits.add(SearchHit(SearchKind.LIST_ITEM, it.checklistId, it.text)) }
        db.userEventDao().search(accountId, like).forEach { hits.add(SearchHit(SearchKind.EVENT, it.id, it.title, it.note)) }
        return hits
    }
}

// ------------------------------------------------------------------------------------------------
// Focus statistics (sessions are written by FocusRepository)
// ------------------------------------------------------------------------------------------------

class FocusHistoryRepository(private val db: NexoraDatabase) {
    fun sessions(accountId: Long): Flow<List<FocusSessionEntity>> = db.focusDao().observe(accountId)
}
