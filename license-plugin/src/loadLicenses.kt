import java.net.URI
import java.net.URL
import java.util.Properties

data class License(
    val name: String,
    val description: String,
    val baseUrl: URL,
    val headerUrl: URL,
) {
    val headerContent: String by lazy { headerUrl.readText() }
}

private const val licensesBaseUrl = "licenses"
private const val licensesFile = "licenses.properties"

internal fun loadLicenses(): List<License> {
    val licenseProperties = Properties()
    val baseUrl = object {}::class.java.getResource(licensesBaseUrl)
    checkNotNull(baseUrl) { "Licenses not found in $licensesBaseUrl" }

    baseUrl.resolve(licensesFile).openStream().use { licenseProperties.load(it) }
    return licenseProperties.entries.map { entry ->
        val licenseName = entry.key as String
        val baseUrl = baseUrl.resolve(licenseName)
        var headerUrl = baseUrl.resolve("header.txt")
        if (headerUrl.openConnection().contentLength <= 0) headerUrl = baseUrl.resolve("header.txt.ftl")
        License(
            name = licenseName,
            description = entry.value as String,
            baseUrl = baseUrl,
            headerUrl = headerUrl,
        )
    }
}

private fun URL.resolve(path: String) =
    URI.create("${this}/$path").toURL()