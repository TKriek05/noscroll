package com.tkriek.scrollless.data

/**
 * De apps die ScrollLess in de gaten houdt. In de MVP bewust alleen Instagram en
 * YouTube; uitbreiden betekent hier een regel toevoegen en de packagenamen in
 * res/xml/accessibility_service_config.xml bijwerken.
 */
enum class TrackedApp(val packageName: String, val label: String) {
    INSTAGRAM("com.instagram.android", "Instagram"),
    YOUTUBE("com.google.android.youtube", "YouTube");

    companion object {
        val packageNames: Set<String> = entries.map { it.packageName }.toSet()

        fun fromPackage(packageName: String?): TrackedApp? =
            entries.firstOrNull { it.packageName == packageName }
    }
}
