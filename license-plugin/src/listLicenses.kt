import org.jetbrains.amper.plugins.TaskAction

/**
 * Lists available licenses.
 */
@TaskAction
fun listLicenses(
    settings: LicenseSettings,
) {
    val licenses = loadLicenses()
    val builder = StringBuilder()
    builder.append("Available licenses:\n")
    val maxLength = licenses.maxOf { it.name.length }
    val pattern = $$" * %1$-$${maxLength}s : %2$s"
    for (license in licenses.sortedBy { it.name }) {
        builder.appendLine(pattern.format(license.name, license.description))
        if (settings.detailedInfo) {
            builder.appendLine()
            builder.appendLine(license.headerContent)
            builder.appendLine()
        }
    }
    println(builder)
}