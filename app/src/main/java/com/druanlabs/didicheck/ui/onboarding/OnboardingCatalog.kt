package com.druanlabs.didicheck.ui.onboarding

import androidx.annotation.DrawableRes
import com.druanlabs.didicheck.R
import com.druanlabs.didicheck.data.model.ItemKind
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.data.model.RoutineItem
import com.druanlabs.didicheck.util.newId

enum class OnboardingSituation(
    val title: String,
    val routineName: String?,
    val asksTake: Boolean,
    val asksConfirm: Boolean,
    val priority: Int,
    @DrawableRes val iconRes: Int,
) {
    LEAVING(
        title = "Before leaving home",
        routineName = "Leaving Home",
        asksTake = true,
        asksConfirm = true,
        priority = 0,
        iconRes = R.drawable.ic_onb_house,
    ),
    WORK(
        title = "Before work",
        routineName = "Going to Work",
        asksTake = true,
        asksConfirm = false,
        priority = 1,
        iconRes = R.drawable.ic_onb_work,
    ),
    SHOPPING(
        title = "When going shopping",
        routineName = "Going Shopping",
        asksTake = true,
        asksConfirm = false,
        priority = 2,
        iconRes = R.drawable.ic_onb_shopping,
    ),
    TRAVELLING(
        title = "When travelling",
        routineName = "Going on a Trip",
        asksTake = true,
        asksConfirm = false,
        priority = 3,
        iconRes = R.drawable.ic_onb_travel,
    ),
    BED(
        title = "Before sleeping",
        routineName = "Before Bed",
        asksTake = true,
        asksConfirm = true,
        priority = 4,
        iconRes = R.drawable.ic_onb_sleep,
    ),
    DURING_DAY(
        title = "Throughout the day",
        routineName = null,
        asksTake = false,
        asksConfirm = false,
        priority = 5,
        iconRes = R.drawable.ic_onb_day,
    ),
}

data class CatalogOption(
    val id: String,
    val label: String,
    @DrawableRes val iconRes: Int,
)

object OnboardingCatalog {
    val situations: List<OnboardingSituation> = OnboardingSituation.entries

    fun takeOptions(situation: OnboardingSituation): List<CatalogOption> = when (situation) {
        OnboardingSituation.LEAVING -> listOf(
            opt("phone", "Phone", R.drawable.ic_onb_phone),
            opt("wallet", "Wallet", R.drawable.ic_onb_wallet),
            opt("keys", "Keys", R.drawable.ic_onb_keys),
            opt("charger", "Charger", R.drawable.ic_onb_plug),
            opt("lunch", "Lunch", R.drawable.ic_onb_lunch),
            opt("documents", "Documents", R.drawable.ic_onb_docs),
        )
        OnboardingSituation.WORK -> listOf(
            opt("phone", "Phone", R.drawable.ic_onb_phone),
            opt("wallet", "Wallet", R.drawable.ic_onb_wallet),
            opt("keys", "Keys", R.drawable.ic_onb_keys),
            opt("charger", "Charger", R.drawable.ic_onb_plug),
            opt("lunch", "Lunch", R.drawable.ic_onb_lunch),
            opt("documents", "Documents", R.drawable.ic_onb_docs),
        )
        OnboardingSituation.SHOPPING -> listOf(
            opt("wallet", "Wallet", R.drawable.ic_onb_wallet),
            opt("bags", "Bags", R.drawable.ic_onb_shopping),
            opt("list", "Shopping list", R.drawable.ic_onb_docs),
            opt("phone", "Phone", R.drawable.ic_onb_phone),
            opt("keys", "Keys", R.drawable.ic_onb_keys),
        )
        OnboardingSituation.TRAVELLING -> listOf(
            opt("passport", "Passport", R.drawable.ic_onb_docs),
            opt("wallet", "Wallet", R.drawable.ic_onb_wallet),
            opt("tickets", "Tickets", R.drawable.ic_onb_docs),
            opt("charger", "Charger", R.drawable.ic_onb_plug),
            opt("phone", "Phone", R.drawable.ic_onb_phone),
            opt("keys", "Keys", R.drawable.ic_onb_keys),
        )
        OnboardingSituation.BED -> listOf(
            opt("phone_charge", "Charge phone", R.drawable.ic_onb_charge),
            opt("meds", "Medicines / vitamins", R.drawable.ic_onb_pill),
            opt("water", "Water", R.drawable.ic_onb_water),
        )
        OnboardingSituation.DURING_DAY -> emptyList()
    }

    fun confirmOptions(situation: OnboardingSituation): List<CatalogOption> = when (situation) {
        OnboardingSituation.LEAVING -> listOf(
            opt("lock", "Lock the door", R.drawable.ic_onb_lock),
            opt("lights", "Turn off the lights", R.drawable.ic_onb_lights),
            opt("stove", "Turn off the stove", R.drawable.ic_onb_stove),
            opt("windows", "Close windows", R.drawable.ic_onb_window),
        )
        OnboardingSituation.BED -> listOf(
            opt("lock", "Lock the door", R.drawable.ic_onb_lock),
            opt("lights", "Turned off lights", R.drawable.ic_onb_lights),
            opt("alarm", "Set alarm", R.drawable.ic_onb_alarm),
        )
        else -> emptyList()
    }

    val didIOptions: List<CatalogOption> = listOf(
        opt("locked", "Locked the door", R.drawable.ic_onb_lock),
        opt("lights", "Turned off the lights", R.drawable.ic_onb_lights),
        opt("vitamins", "Took my vitamins", R.drawable.ic_onb_pill),
        opt("pet", "Fed my pet", R.drawable.ic_onb_paw),
        opt("email", "Sent an email", R.drawable.ic_onb_email),
        opt("car", "Locked the car", R.drawable.ic_onb_car),
        opt("stove", "Turned off the stove", R.drawable.ic_onb_stove),
    )

    val duringDayQuickOptions: List<CatalogOption> = listOf(
        opt("meds", "Took medicine", R.drawable.ic_onb_pill),
        opt("vitamins", "Took vitamins", R.drawable.ic_onb_pill),
        opt("pet", "Fed the cat", R.drawable.ic_onb_paw),
        opt("plants", "Watered plants", R.drawable.ic_onb_water),
    )

    private fun opt(id: String, label: String, @DrawableRes iconRes: Int) =
        CatalogOption(id = id, label = label, iconRes = iconRes)

    fun defaultTakeLabels(situation: OnboardingSituation): List<String> = when (situation) {
        OnboardingSituation.LEAVING -> listOf("Phone", "Wallet", "Keys")
        OnboardingSituation.WORK -> listOf("Phone", "Wallet", "Keys")
        OnboardingSituation.SHOPPING -> listOf("Wallet", "Bags")
        OnboardingSituation.TRAVELLING -> listOf("Wallet", "Phone", "Charger")
        OnboardingSituation.BED -> listOf("Charge phone")
        OnboardingSituation.DURING_DAY -> emptyList()
    }

    fun defaultConfirmLabels(situation: OnboardingSituation): List<String> = when (situation) {
        OnboardingSituation.LEAVING -> listOf("Lock the door")
        OnboardingSituation.BED -> listOf("Lock the door")
        else -> emptyList()
    }

    fun buildRoutines(
        situations: Set<OnboardingSituation>,
        takeBySituation: Map<OnboardingSituation, List<String>>,
        confirmBySituation: Map<OnboardingSituation, List<String>>,
    ): List<Routine> {
        return situations
            .filter { it.routineName != null }
            .sortedBy { it.priority }
            .take(3)
            .map { situation ->
                val routineId = newId()
                val name = situation.routineName!!
                var take = takeBySituation[situation].orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                var confirm = confirmBySituation[situation].orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                if (take.isEmpty() && situation.asksTake) {
                    take = defaultTakeLabels(situation)
                }
                if (confirm.isEmpty() && situation.asksConfirm) {
                    confirm = defaultConfirmLabels(situation)
                }
                val items = mutableListOf<RoutineItem>()
                take.forEachIndexed { index, label ->
                    items += RoutineItem(
                        id = newId(),
                        routineId = routineId,
                        label = label,
                        kind = ItemKind.TAKE,
                        sortOrder = index,
                    )
                }
                confirm.forEachIndexed { index, label ->
                    items += RoutineItem(
                        id = newId(),
                        routineId = routineId,
                        label = label,
                        kind = ItemKind.CONFIRM,
                        sortOrder = take.size + index,
                    )
                }
                Routine(
                    id = routineId,
                    name = name,
                    sortOrder = situation.priority,
                    lastCompletedAt = null,
                    items = items,
                )
            }
    }
}
