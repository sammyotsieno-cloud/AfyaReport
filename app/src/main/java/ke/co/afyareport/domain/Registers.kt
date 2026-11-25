package ke.co.afyareport.domain

data class RegisterDef(val id: String, val name: String, val code: String, val about: String)

data class ReportToolDef(val title: String, val about: String, val route: String)

/** The registers built into the app. New registers are added here as we build them. */
val registerList = listOf(
    RegisterDef(
        id = "dmhtn",
        name = "Diabetes and Hypertension",
        code = "MOH 222",
        about = "Daily activity register for diabetes and hypertension care."
    )
)

/** The reporting tools built into the app. */
val reportToolList = listOf(
    ReportToolDef(
        title = "Diabetes and Hypertension monthly summary",
        about = "MOH 740, worked out from the MOH 222 register. Save it as a PDF or share it.",
        route = "report"
    ),
    ReportToolDef(
        title = "Admissions and deaths",
        about = "The monthly counts the 740 needs from your inpatient records.",
        route = "counts"
    )
)
