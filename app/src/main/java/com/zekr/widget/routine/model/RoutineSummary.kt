package com.zekr.widget.routine.model

import com.zekr.widget.audio.AudioCatalog

object RoutineSummary {

    private val DAY_NAMES = mapOf(
        1 to "الإثنين",
        2 to "الثلاثاء",
        3 to "الأربعاء",
        4 to "الخميس",
        5 to "الجمعة",
        6 to "السبت",
        7 to "الأحد"
    )

    fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val m = minute.toString().padStart(2, '0')
        val amPm = if (hour < 12) "ص" else "م"
        return "$h:$m $amPm"
    }

    fun describeCondition(condition: RoutineCondition): String = when (condition) {
        is RoutineCondition.Bluetooth -> {
            val dev = if (condition.deviceName.isNullOrBlank()) "أي جهاز بلوتوث" else "بلوتوث «${condition.deviceName}»"
            when (condition.eventType) {
                BluetoothEventType.CONNECTED -> "الاتصال بـ $dev"
                BluetoothEventType.CONNECTED_STATE -> "بقاء $dev متصلاً"
                BluetoothEventType.DISCONNECTED -> "الانفصال عن $dev"
            }
        }
        is RoutineCondition.Time -> {
            val days = if (condition.daysOfWeek.isEmpty()) {
                "يومياً"
            } else {
                "أيام (${condition.daysOfWeek.mapNotNull { DAY_NAMES[it] }.joinToString("، ")})"
            }
            when (condition.timeType) {
                TimeConditionType.EXACT_TIME -> "الساعة ${formatTime(condition.startHour, condition.startMinute)} ($days)"
                TimeConditionType.TIME_WINDOW -> "الوقت بين ${formatTime(condition.startHour, condition.startMinute)} و ${formatTime(condition.endHour, condition.endMinute)} ($days)"
            }
        }
        is RoutineCondition.Charging -> {
            when (condition.chargingType) {
                ChargingConditionType.CONNECTED -> "توصيل الشاحن"
                ChargingConditionType.DISCONNECTED -> "فصل الشاحن"
                ChargingConditionType.CHARGING_STATE -> "أثناء الشحن"
            }
        }
    }

    fun describeAction(action: RoutineAction): String = when (action) {
        is RoutineAction.AudioTrackAction -> action.title
        is RoutineAction.QuranSurahAction -> {
            val reciter = AudioCatalog.QuranReciter.entries.firstOrNull { it.id == action.reciterId }?.shortName ?: ""
            if (reciter.isNotBlank()) "سورة ${action.surahName} ($reciter)" else "سورة ${action.surahName}"
        }
        is RoutineAction.QuranResumeAction -> {
            val reciter = AudioCatalog.QuranReciter.entries.firstOrNull { it.id == action.reciterId }?.shortName ?: ""
            if (reciter.isNotBlank()) "استكمال القرآن من آخر موضع ($reciter)" else "استكمال القرآن من آخر موضع"
        }
        is RoutineAction.QuranStartFromAction -> {
            val reciter = AudioCatalog.QuranReciter.entries.firstOrNull { it.id == action.reciterId }?.shortName ?: ""
            if (reciter.isNotBlank()) "تشغيل القرآن بدءاً من سورة ${action.startSurahName} ($reciter)" else "تشغيل القرآن بدءاً من سورة ${action.startSurahName}"
        }
    }

    fun generateSummary(routine: AudioRoutine): String {
        val conditionsText = if (routine.conditions.isEmpty()) {
            "تشغيل يدوي (لا توجد شروط)"
        } else {
            val logicWord = if (routine.logic == ConditionLogic.AND) "تحقق كل الشروط" else "تحقق أي شرط من"
            val conditionsList = routine.conditions.joinToString("، ") { describeCondition(it) }
            "عند $logicWord: [$conditionsList]"
        }

        val actionsText = if (routine.actions.isEmpty()) {
            "لم يتم تحديد أصوات"
        } else {
            val actionsList = routine.actions.mapIndexed { idx, act -> "${idx + 1}. ${describeAction(act)}" }.joinToString(" ➔ ")
            "شغّل بالترتيب: $actionsList"
        }

        return "$conditionsText، $actionsText."
    }
}
