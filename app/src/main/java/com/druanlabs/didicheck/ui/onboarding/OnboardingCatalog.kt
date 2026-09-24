package com.druanlabs.didicheck.ui.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
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
) {
    LEAVING(
        title = "Before leaving home",
        routineName = "Leaving Home",
        asksTake = true,
        asksConfirm = true,
        priority = 0,
    ),
    WORK(
        title = "Before work",
        routineName = "Going to Work",
        asksTake = true,
        asksConfirm = false,
        priority = 1,
    ),
    SHOPPING(
        title = "When going shopping",
        routineName = "Going Shopping",
        asksTake = true,
        asksConfirm = false,
        priority = 2,
    ),
    TRAVELLING(
        title = "When travelling",
        routineName = "Going on a Trip",
        asksTake = true,
        asksConfirm = false,
        priority = 3,
    ),
    BED(
        title = "Before sleeping",
        routineName = "Before Bed",
        asksTake = true,
        asksConfirm = true,
        priority = 4,
    ),
    DURING_DAY(
        title = "Throughout the day",
        routineName = null,
        asksTake = false,
        asksConfirm = false,
        priority = 5,
    ),
}

data class CatalogOption(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

object OnboardingCatalog {
    val situations: List<Pair<OnboardingSituation, ImageVector>> = listOf(
        OnboardingSituation.LEAVING to Icons.Outlined.Home,
        OnboardingSituation.WORK to Icons.Outlined.Work,
        OnboardingSituation.SHOPPING to Icons.Outlined.ShoppingBag,
        OnboardingSituation.TRAVELLING to Icons.Outlined.Flight,
        OnboardingSituation.BED to Icons.Outlined.Bedtime,
        OnboardingSituation.DURING_DAY to Icons.Outlined.Bolt,
    )

    fun takeOptions(situation: OnboardingSituation): List<CatalogOption> = when (situation) {
        OnboardingSituation.LEAVING -> listOf(
            opt("phone", "Phone", Icons.Outlined.PhoneAndroid),
            opt("wallet", "Wallet", Icons.Outlined.AccountBalanceWallet),
            opt("keys", "Keys", Icons.Outlined.Key),
            opt("charger", "Charger", Icons.Outlined.Bolt),
            opt("lunch", "Lunch", Icons.Outlined.Restaurant),
            opt("documents", "Documents", Icons.Outlined.Description),
        )
        OnboardingSituation.WORK -> listOf(
            opt("phone", "Phone", Icons.Outlined.PhoneAndroid),
            opt("wallet", "Wallet", Icons.Outlined.AccountBalanceWallet),
            opt("keys", "Keys", Icons.Outlined.Key),
            opt("charger", "Charger", Icons.Outlined.Bolt),
            opt("lunch", "Lunch", Icons.Outlined.Restaurant),
            opt("documents", "Documents", Icons.Outlined.Description),
        )
        OnboardingSituation.SHOPPING -> listOf(
            opt("wallet", "Wallet", Icons.Outlined.AccountBalanceWallet),
            opt("bags", "Bags", Icons.Outlined.ShoppingBag),
            opt("list", "Shopping list", Icons.Outlined.Description),
            opt("phone", "Phone", Icons.Outlined.PhoneAndroid),
            opt("keys", "Keys", Icons.Outlined.Key),
        )
        OnboardingSituation.TRAVELLING -> listOf(
            opt("passport", "Passport", Icons.Outlined.Description),
            opt("wallet", "Wallet", Icons.Outlined.AccountBalanceWallet),
            opt("tickets", "Tickets", Icons.Outlined.Description),
            opt("charger", "Charger", Icons.Outlined.Bolt),
            opt("phone", "Phone", Icons.Outlined.PhoneAndroid),
            opt("keys", "Keys", Icons.Outlined.Key),
        )
        OnboardingSituation.BED -> listOf(
            opt("phone_charge", "Charge phone", Icons.Outlined.PhoneAndroid),
            opt("meds", "Medicines / vitamins", Icons.Outlined.LocalPharmacy),
            opt("water", "Water", Icons.Outlined.WaterDrop),
        )
        OnboardingSituation.DURING_DAY -> emptyList()
    }

    fun confirmOptions(situation: OnboardingSituation): List<CatalogOption> = when (situation) {
        OnboardingSituation.LEAVING -> listOf(
            opt("lock", "Lock the door", Icons.Outlined.Lock),
            opt("lights", "Turn off the lights", Icons.Outlined.Lightbulb),
            opt("stove", "Turn off the stove", Icons.Outlined.Bolt),
            opt("windows", "Close windows", Icons.Outlined.Home),
        )
        OnboardingSituation.BED -> listOf(
            opt("lock", "Lock the door", Icons.Outlined.Lock),
            opt("lights", "Turned off lights", Icons.Outlined.Lightbulb),
            opt("alarm", "Set alarm", Icons.Outlined.Bolt),
        )
        else -> emptyList()
    }

    val didIOptions: List<CatalogOption> = listOf(
        opt("locked", "Locked the door", Icons.Outlined.Lock),
        opt("lights", "Turned off the lights", Icons.Outlined.Lightbulb),
        opt("vitamins", "Took my vitamins", Icons.Outlined.LocalPharmacy),
        opt("pet", "Fed my pet", Icons.Outlined.Pets),
        opt("email", "Sent an email", Icons.Outlined.Email),
        opt("car", "Locked the car", Icons.Outlined.DirectionsCar),
        opt("stove", "Turned off the stove", Icons.Outlined.Bolt),
    )

    val duringDayQuickOptions: List<CatalogOption> = listOf(
        opt("meds", "Took medicine", Icons.Outlined.LocalPharmacy),
        opt("vitamins", "Took vitamins", Icons.Outlined.LocalPharmacy),
        opt("pet", "Fed the cat", Icons.Outlined.Pets),
        opt("plants", "Watered plants", Icons.Outlined.WaterDrop),
    )

    private fun opt(id: String, label: String, icon: ImageVector) =
        CatalogOption(id = id, label = label, icon = icon)

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

    /**
     * Build up to 3 routines from selected situations and item picks.
     */
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
