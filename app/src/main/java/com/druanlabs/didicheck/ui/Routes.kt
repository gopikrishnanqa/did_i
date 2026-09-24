package com.druanlabs.didicheck.ui

object Routes {
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val History = "history?openId={openId}"
    const val Settings = "settings"
    const val Routines = "routines"
    const val Checklist = "checklist/{routineId}"
    const val DidIDoIt = "did_i_do_it"
    const val ManageRoutines = "manage_routines"
    const val EditRoutine = "edit_routine/{routineId}"
    const val ManageActions = "manage_actions"

    fun checklist(routineId: String) = "checklist/$routineId"
    fun editRoutine(routineId: String = "new") = "edit_routine/$routineId"
    fun history(openId: String? = null): String =
        if (openId.isNullOrBlank()) "history?openId=" else "history?openId=$openId"
}
