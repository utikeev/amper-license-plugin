import org.jetbrains.amper.plugins.Configurable
import org.jetbrains.amper.plugins.EnumValue

@Configurable
interface LicenseSettings {
    /**
     * Whether to show content when `listLicenses` is executed.
     */
    val detailedInfo: Boolean get() = false

    /**
     * Settings for header generation.
     */
    val headerSettings: HeaderSettings
}

/**
 * Settings for header generation.
 */
@Configurable
interface HeaderSettings {
    /**
     * Inception year of the project.
     */
    val inceptionYear: Int

    /**
     * License name.
     */
    val licenseName: String

    /**
     * Organization name.
     */
    val organizationName: String

    /**
     * What to do when a header is missing if `checkLicenseHeaders` is called.
     */
    val onMissingHeader: MissingHeaderAction get() = MissingHeaderAction.Warn
}

enum class MissingHeaderAction {
    @EnumValue("fail")
    Fail,
    @EnumValue("warn")
    Warn,
}