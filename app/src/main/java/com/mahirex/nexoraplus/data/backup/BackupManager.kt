package com.mahirex.nexoraplus.data.backup

import androidx.room.withTransaction
import com.mahirex.nexoraplus.core.settings.ThemeMode
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
import com.mahirex.nexoraplus.data.repo.AccountRepository
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

sealed class ImportResult {
    data class Success(val accounts: Int) : ImportResult()
    object InvalidFile : ImportResult()
    object TooManyAccounts : ImportResult()
    object UnsupportedVersion : ImportResult()
}

/** JSON export / import of the user's data (one account or all accounts) plus calendar & weekly settings. */
class BackupManager(
    private val db: NexoraDatabase,
    private val settings: SettingsStore
) {
    /** [accountIds] = the accounts to export. */
    suspend fun export(accountIds: List<Long>, appVersionName: String): String {
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("backupVersion", VERSION)
        root.put("appVersion", appVersionName)
        root.put("exportedAt", System.currentTimeMillis())

        val s = settings.current()
        root.put(
            "settings",
            JSONObject()
                .put("themeMode", s.themeMode.name)
                .put("calendarMode", s.calendarMode)
                .put("occasionRegion", s.occasionRegion)
                .put("hijriCorrection", s.hijriCorrection)
                .put("showSecondaryHijri", s.showSecondaryHijri)
                .put("weekStart", s.weekStart)
                .put("autoUpdate", s.autoUpdate)
        )

        val accounts = JSONArray()
        val all = db.accountDao().getAll().filter { it.id in accountIds }
        for (a in all) accounts.put(exportAccount(a))
        root.put("accounts", accounts)
        return root.toString(2)
    }

    private suspend fun exportAccount(a: AccountEntity): JSONObject {
        val o = JSONObject()
        o.put("name", a.name).put("color", a.colorArgb).put("createdAt", a.createdAt)

        val tasks = JSONArray()
        db.taskDao().getAll(a.id).forEach {
            tasks.put(
                JSONObject().put("title", it.title).put("note", it.note).put("priority", it.priority)
                    .put("due", it.dueEpochDay ?: JSONObject.NULL).put("pinned", it.pinned).put("done", it.done)
                    .put("doneAt", it.doneAt ?: JSONObject.NULL).put("createdAt", it.createdAt)
            )
        }
        o.put("tasks", tasks)

        val goals = JSONArray()
        db.goalDao().getAll(a.id).forEach {
            goals.put(
                JSONObject().put("title", it.title).put("note", it.note).put("target", it.targetEpochDay ?: JSONObject.NULL)
                    .put("progress", it.progress).put("pinned", it.pinned).put("done", it.done).put("createdAt", it.createdAt)
            )
        }
        o.put("goals", goals)

        val logsByHabit = db.habitDao().getLogs(a.id).groupBy { it.habitId }
        val habits = JSONArray()
        db.habitDao().getAll(a.id).forEach { h ->
            val logs = JSONArray()
            logsByHabit[h.id].orEmpty().forEach { logs.put(it.epochDay) }
            habits.put(
                JSONObject().put("title", h.title).put("color", h.colorArgb).put("pinned", h.pinned)
                    .put("createdAt", h.createdAt).put("logs", logs)
            )
        }
        o.put("habits", habits)

        val itemsByList = db.checklistDao().getItems(a.id).groupBy { it.checklistId }
        val lists = JSONArray()
        db.checklistDao().getLists(a.id).forEach { l ->
            val items = JSONArray()
            itemsByList[l.id].orEmpty().forEach {
                items.put(JSONObject().put("text", it.text).put("done", it.done).put("order", it.sortOrder))
            }
            lists.put(
                JSONObject().put("title", l.title).put("color", l.colorArgb).put("pinned", l.pinned)
                    .put("order", l.sortOrder).put("createdAt", l.createdAt).put("items", items)
            )
        }
        o.put("checklists", lists)

        val events = JSONArray()
        db.userEventDao().getAll(a.id).forEach {
            events.put(
                JSONObject().put("title", it.title).put("note", it.note).put("calendar", it.calendarType)
                    .put("year", it.year ?: JSONObject.NULL).put("month", it.month).put("day", it.day)
                    .put("repeatYearly", it.repeatYearly).put("createdAt", it.createdAt)
            )
        }
        o.put("events", events)

        val slots = db.scheduleDao().getSlots(a.id)
        val slotIndex = slots.withIndex().associate { it.value.id to it.index }
        val slotArr = JSONArray()
        slots.forEach {
            slotArr.put(
                JSONObject().put("start", it.startMinute).put("end", it.endMinute).put("label", it.label).put("color", it.colorArgb)
            )
        }
        val cellArr = JSONArray()
        db.scheduleDao().getCells(a.id).forEach {
            val idx = slotIndex[it.slotId] ?: return@forEach
            cellArr.put(
                JSONObject().put("slot", idx).put("day", it.dayOfWeek).put("text", it.text).put("color", it.colorArgb ?: JSONObject.NULL)
            )
        }
        val preset = db.scheduleDao().getPreset(a.id)
        o.put(
            "schedule",
            JSONObject().put("slots", slotArr).put("cells", cellArr).put("preset", preset?.layoutJson ?: JSONObject.NULL)
        )

        val fortunes = JSONArray()
        db.fortuneDao().getAll(a.id).forEach { fortunes.put(JSONObject().put("day", it.epochDay).put("index", it.fortuneIndex)) }
        o.put("fortunes", fortunes)

        val focus = JSONArray()
        db.focusDao().getAll(a.id).forEach {
            focus.put(
                JSONObject().put("startedAt", it.startedAt).put("planned", it.plannedMinutes).put("actual", it.actualMinutes)
                    .put("dnd", it.dndUsed).put("completed", it.completed)
            )
        }
        o.put("focus", focus)
        return o
    }

    /** Imports the accounts of a backup as *new* accounts; nothing existing is overwritten. */
    suspend fun import(text: String): ImportResult {
        val root = try {
            JSONObject(text)
        } catch (_: JSONException) {
            return ImportResult.InvalidFile
        }
        if (root.optString("app") != APP_ID) return ImportResult.InvalidFile
        if (root.optInt("backupVersion", -1) != VERSION) return ImportResult.UnsupportedVersion
        val accounts = root.optJSONArray("accounts") ?: return ImportResult.InvalidFile
        if (accounts.length() == 0) return ImportResult.InvalidFile

        val existing = db.accountDao().count()
        if (existing + accounts.length() > AccountRepository.MAX_ACCOUNTS) return ImportResult.TooManyAccounts

        try {
            db.withTransaction {
                for (i in 0 until accounts.length()) {
                    importAccount(accounts.getJSONObject(i), existing + i)
                }
            }
        } catch (_: JSONException) {
            return ImportResult.InvalidFile
        }

        root.optJSONObject("settings")?.let { applySettings(it) }
        return ImportResult.Success(accounts.length())
    }

    private suspend fun applySettings(o: JSONObject) {
        runCatching { settings.setThemeMode(ThemeMode.valueOf(o.getString("themeMode"))) }
        o.optString("calendarMode").takeIf { it in setOf("auto", "G", "J", "H") }?.let { settings.setCalendarMode(it) }
        o.optString("occasionRegion").takeIf { it.isNotEmpty() }?.let { settings.setOccasionRegion(it) }
        if (o.has("hijriCorrection")) settings.setHijriCorrection(o.optInt("hijriCorrection", 0))
        if (o.has("showSecondaryHijri")) settings.setShowSecondaryHijri(o.optBoolean("showSecondaryHijri", true))
        o.optString("weekStart").takeIf { it in setOf("auto", "SAT", "SUN", "MON") }?.let { settings.setWeekStart(it) }
        if (o.has("autoUpdate")) settings.setAutoUpdate(o.optBoolean("autoUpdate", true))
    }

    private suspend fun importAccount(o: JSONObject, order: Int) {
        val now = System.currentTimeMillis()
        val accountId = db.accountDao().insert(
            AccountEntity(name = o.getString("name"), colorArgb = o.getInt("color"), createdAt = o.optLong("createdAt", now), sortOrder = order)
        )

        o.optJSONArray("tasks")?.forEachObject { t ->
            db.taskDao().insert(
                TaskEntity(
                    accountId = accountId, title = t.getString("title"), note = t.optString("note", ""),
                    priority = t.optInt("priority", 1).coerceIn(0, 2), dueEpochDay = t.longOrNull("due"),
                    pinned = t.optBoolean("pinned"), done = t.optBoolean("done"), doneAt = t.longOrNull("doneAt"),
                    createdAt = t.optLong("createdAt", now)
                )
            )
        }
        o.optJSONArray("goals")?.forEachObject { g ->
            db.goalDao().insert(
                GoalEntity(
                    accountId = accountId, title = g.getString("title"), note = g.optString("note", ""),
                    targetEpochDay = g.longOrNull("target"), progress = g.optInt("progress").coerceIn(0, 100),
                    pinned = g.optBoolean("pinned"), done = g.optBoolean("done"), createdAt = g.optLong("createdAt", now)
                )
            )
        }
        o.optJSONArray("habits")?.forEachObject { h ->
            val habitId = db.habitDao().insert(
                HabitEntity(
                    accountId = accountId, title = h.getString("title"), colorArgb = h.getInt("color"),
                    pinned = h.optBoolean("pinned"), createdAt = h.optLong("createdAt", now)
                )
            )
            val logs = h.optJSONArray("logs")
            if (logs != null) for (k in 0 until logs.length()) {
                db.habitDao().insertLog(HabitLogEntity(habitId = habitId, epochDay = logs.getLong(k)))
            }
        }
        o.optJSONArray("checklists")?.forEachObject { l ->
            val listId = db.checklistDao().insertList(
                ChecklistEntity(
                    accountId = accountId, title = l.getString("title"), colorArgb = l.getInt("color"),
                    pinned = l.optBoolean("pinned"), sortOrder = l.optInt("order"), createdAt = l.optLong("createdAt", now)
                )
            )
            l.optJSONArray("items")?.forEachObject { it ->
                db.checklistDao().insertItem(
                    ChecklistItemEntity(checklistId = listId, text = it.getString("text"), done = it.optBoolean("done"), sortOrder = it.optInt("order"))
                )
            }
        }
        o.optJSONArray("events")?.forEachObject { e ->
            db.userEventDao().insert(
                UserEventEntity(
                    accountId = accountId, title = e.getString("title"), note = e.optString("note", ""),
                    calendarType = e.getString("calendar"), year = e.intOrNull("year"), month = e.getInt("month"),
                    day = e.getInt("day"), repeatYearly = e.optBoolean("repeatYearly"), createdAt = e.optLong("createdAt", now)
                )
            )
        }

        val schedule = o.optJSONObject("schedule")
        if (schedule != null) {
            val slotIds = ArrayList<Long>()
            schedule.optJSONArray("slots")?.forEachObject { s ->
                slotIds.add(
                    db.scheduleDao().insertSlot(
                        ScheduleSlotEntity(
                            accountId = accountId, orderIndex = slotIds.size, startMinute = s.getInt("start"),
                            endMinute = s.getInt("end"), label = s.optString("label", ""), colorArgb = s.getInt("color")
                        )
                    )
                )
            }
            schedule.optJSONArray("cells")?.forEachObject { c ->
                val slotId = slotIds.getOrNull(c.getInt("slot")) ?: return@forEachObject
                db.scheduleDao().upsertCell(
                    ScheduleCellEntity(
                        accountId = accountId, slotId = slotId, dayOfWeek = c.getInt("day").coerceIn(1, 7),
                        text = c.optString("text", ""), colorArgb = c.intOrNull("color")
                    )
                )
            }
            val preset = schedule.opt("preset")
            if (preset is String) db.scheduleDao().upsertPreset(SchedulePresetEntity(accountId, preset, now))
        }

        o.optJSONArray("fortunes")?.forEachObject { f ->
            db.fortuneDao().insert(FortuneHistoryEntity(accountId = accountId, epochDay = f.getLong("day"), fortuneIndex = f.getInt("index")))
        }
        o.optJSONArray("focus")?.forEachObject { f ->
            db.focusDao().insert(
                FocusSessionEntity(
                    accountId = accountId, startedAt = f.getLong("startedAt"), plannedMinutes = f.getInt("planned"),
                    actualMinutes = f.getInt("actual"), dndUsed = f.optBoolean("dnd"), completed = f.optBoolean("completed")
                )
            )
        }
    }

    private companion object {
        const val APP_ID = "nexora-plus"
        const val VERSION = 1
    }
}

private suspend fun JSONArray.forEachObject(block: suspend (JSONObject) -> Unit) {
    for (i in 0 until length()) block(getJSONObject(i))
}

private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
