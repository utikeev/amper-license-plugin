import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.serialization.XML
import org.jetbrains.amper.plugins.Classpath
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText

private data class DependencyInformation(
    val coordinates: String,
    val licenses: List<String>,
    val name: String,
    val url: String?,
)

private val XMLParser = XML.recommended_1_0 {
    policy {
        typeDiscriminatorName = QName(XMLConstants.XML_NS_URI, "http://maven.apache.org/POM/4.0.0")
        ignoreUnknownChildren()
    }
}

@TaskAction
fun collectThirdPartyLicenses(
    // TODO: Would be good to have all the dependencies with resolved artifacts instead of this?
    @Input compileDependencies: Classpath,
    @Input runtimeDependencies: Classpath,
    @Output reportFile: Path,
) {
    val uniqueDependencies = mutableMapOf<String, DependencyInformation>()
    // Dependencies list itself is useless as there are no transitive deps and implicits deps aren't available
    // TODO: Would be nice to have DR information here instead
    for (dependency in compileDependencies.resolvedFiles + runtimeDependencies.resolvedFiles) {
        if (dependency.extension != "jar") continue
        val pomPath = dependency.parent.resolve(dependency.nameWithoutExtension + ".pom")
        if (!pomPath.exists()) continue // TODO: This can also skip a dependency without license or POM
        val pomXml = XMLParser.decodeFromString<PomProject>(pomPath.readText())
        if (pomXml.artifactId == null) {
            println(pomPath.readText())
            continue
        }
        val groupId = pomXml.groupId ?: pomXml.parent?.groupId
        val version = pomXml.version ?: pomXml.parent?.version
        val coordinates = buildString {
            if (groupId != null) {
                append(groupId)
                append(":")
            }
            append(pomXml.artifactId)
            if (version != null) {
                append(":")
                append(version)
            }
        }
        uniqueDependencies.putIfAbsent(
            coordinates,
            DependencyInformation(
                coordinates = coordinates,
                licenses = findLicenses(pomPath, pomXml),
                name = pomXml.name ?: coordinates,
                url = pomXml.url
            )
        )
    }
    val licenseEntries = uniqueDependencies.values.sortedBy { it.coordinates }
    val report = buildString {
        appendLine("Licenses for ${licenseEntries.size} dependencies:")
        for (info in licenseEntries) {
            val information = buildString {
                append("  ")
                // TODO: This loses the case when the license is in the parent project
                for (license in info.licenses) {
                    append("($license) ")
                }
                append(info.name)
                append(" (")
                append(info.coordinates)
                if (info.url != null) append(" - ${info.url}")
                append(")")
            }
            appendLine(information)
        }
    }
    reportFile.createParentDirectories()
    reportFile.writeText(report)
}

private fun findLicenses(pomPath: Path, pomXML: PomProject): List<String> {
    if (pomXML.licenses.isNotEmpty()) return pomXML.licenses.map { it.name }
    if (pomXML.parent == null) return emptyList()
    val parentGroup = pomXML.parent.groupId ?: return emptyList()
    if (pomXML.groupId != null && pomXML.groupId != parentGroup) return emptyList() // TODO: This might be incorrect, but this is a prototype, who cares
    val parentArtifact = pomXML.parent.artifactId ?: return emptyList()
    val parentVersion = pomXML.parent.version ?: return emptyList()
    val parentPom = pomPath
        .parent // version folder
        ?.parent // artifact folder
        ?.parent // group folder
        ?.resolve(parentArtifact)
        ?.resolve(parentVersion)
        ?.resolve("$parentArtifact-$parentVersion.pom")
    if (parentPom?.exists() != true) return emptyList()
    val parsedPom = XMLParser.decodeFromString<PomProject>(parentPom.readText())
    return findLicenses(parentPom, parsedPom)
}