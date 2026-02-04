import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * Stripped version of Maven POM needed for licensing purposes.
 */
@Serializable
@XmlSerialName("project", namespace = "http://maven.apache.org/POM/4.0.0")
internal data class PomProject(
    @XmlElement val parent: PomParent?,
    @XmlElement val groupId: String?,
    @XmlElement val artifactId: String?,
    @XmlElement val version: String?,
    @XmlElement val name: String?,
    @XmlElement val url: String?,
    @XmlElement
    @XmlChildrenName("license")
    val licenses: List<PomLicense>
)

@Serializable
@XmlSerialName("parent", namespace = "http://maven.apache.org/POM/4.0.0")
internal data class PomParent(
    @XmlElement val groupId: String?,
    @XmlElement val artifactId: String?,
    @XmlElement val version: String?,
)

@Serializable
internal data class PomLicense(
    @XmlElement val name: String,
    @XmlElement val url: String,
)