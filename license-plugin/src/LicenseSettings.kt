import org.jetbrains.amper.plugins.Configurable

@Configurable
interface LicenseSettings {
    /**
     * Whether to show content when `listLicenses` is executed.
     */
    val detailedInfo: Boolean get() = false
}