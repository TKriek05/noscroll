package com.tkriek.scrollless.util

/**
 * Een vervanger: iets concreets dat binnen een paar minuten kan en dat je
 * achteraf een beter gevoel geeft dan scrollen.
 */
data class Alternative(
    val title: String,
    val detail: String,
    val minutes: Int
)

/**
 * Fase 0-lijst. Houd 'm persoonlijk en concreet: "5 min aan portfolio" werkt,
 * "iets nuttigs doen" niet. Pas deze lijst gerust aan naarmate je merkt welke
 * suggesties je echt oppakt.
 */
object Alternatives {

    val all: List<Alternative> = listOf(
        Alternative("5 min aan je portfolio", "Open het project dat half af is en doe één kleine commit.", 5),
        Alternative("1 foto nabewerken", "Pak de bovenste foto in ON1 en werk 'm helemaal af.", 10),
        Alternative("Minecraft build-idee opschrijven", "Schets in twee zinnen wat je volgende build wordt.", 3),
        Alternative("Home Assistant opruimen", "Eén automation die je irriteert fixen of weggooien.", 8),
        Alternative("Even naar buiten", "Rondje om het blok, telefoon blijft binnen.", 10),
        Alternative("Glas water halen", "Sta op, drink, kijk even uit het raam.", 2),
        Alternative("20 push-ups of squats", "Kort, zwaar genoeg om je hoofd leeg te maken.", 3),
        Alternative("Bureau opruimen", "Alles wat er niet hoort ligt, weg of terug.", 5),
        Alternative("5 bladzijden lezen", "Boek of longread die al open staat.", 8),
        Alternative("Eén bericht sturen", "Iemand appen die je al te lang wilde spreken.", 4)
    )

    fun random(): Alternative = all.random()

    /** Een andere suggestie dan [current], zodat "volgende" ook echt iets nieuws geeft. */
    fun randomOtherThan(current: Alternative?): Alternative {
        if (current == null || all.size < 2) return random()
        return all.filter { it != current }.random()
    }
}
