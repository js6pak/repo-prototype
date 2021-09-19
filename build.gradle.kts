import com.google.gson.*
import de.skuzzle.semantic.Version
import java.lang.reflect.Type
import java.net.URL
import java.util.*

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.google.code.gson:gson:2.8.8")
        classpath("commons-codec:commons-codec:1.15")
        classpath("de.skuzzle:semantic-version:2.1.0")
    }
}

private class SemanticVersionSerializer : JsonSerializer<Version?>, JsonDeserializer<Version?> {
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Version {
        return Version.parseVersion(json.asString)
    }

    override fun serialize(src: Version?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.toString())
    }
}

abstract class BasePluginInfo {
    abstract val name: String
    abstract val description: String
    abstract val authors: List<AuthorInfo>
    abstract val links: HashMap<String, String>
}

data class FullPluginInfo(
    override val name: String,
    override val description: String,
    override val authors: List<AuthorInfo>,
    override val links: HashMap<String, String>,
    val versions: TreeMap<Version, VersionInfo>
) : BasePluginInfo()

data class CompiledPluginInfo(
    override val name: String,
    override val description: String,
    override val authors: List<AuthorInfo>,
    override val links: HashMap<String, String>,
    val version: Version,
    val changelog: ChangelogInfo?,
    val download: DownloadInfo
) : BasePluginInfo() {
    constructor(original: FullPluginInfo) : this(
        original.name,
        original.description,
        original.authors,
        original.links,
        original.versions.keys.maxByOrNull { it }!!,
        original.versions.maxByOrNull { it.key }!!.value.changelog,
        original.versions.maxByOrNull { it.key }!!.value.download
    )
}

data class AuthorInfo(
    val id: Long,
    val name: String
)

data class VersionInfo(
    val changelog: ChangelogInfo?,
    val download: DownloadInfo
)

data class ChangelogInfo(
    val text: String,
    val media: String?
)

data class DownloadInfo(
    val url: String,
    val sha1: String
)

data class PluginManifest(
    val pluginClassName: String,
    val name: String,
    val version: Version,
    val description: String,
    val authors: List<AuthorInfo>,
    val links: HashMap<String, String>?,
    val changelog: String?,
    val changelogMedia: String?
)

val gson: Gson = GsonBuilder()
    .registerTypeAdapter(Version::class.java, SemanticVersionSerializer())
    .create()

inline fun <reified T> Gson.fromJson(json: String): T {
    return this.fromJson(json, T::class.java)
}

task("compile") {
    group = "aliucord"

    val plugins = kotlin.collections.ArrayList<CompiledPluginInfo>()

    val pluginsDirectory = buildDir.resolve("plugins")
    pluginsDirectory.mkdirs()

    fileTree("plugins").forEach {
        val plugin = gson.fromJson<FullPluginInfo>(it.readText())

        pluginsDirectory.resolve(plugin.name).mkdir()

        val file = pluginsDirectory.resolve(plugin.name + ".json")
        file.writeText(gson.toJson(plugin))

        plugins.add(CompiledPluginInfo(plugin))
    }

    buildDir.resolve("plugins.json").writeText(gson.toJson(plugins))
}

abstract class AddTask : DefaultTask() {
    private val gson = GsonBuilder()
        .registerTypeAdapter(Version::class.java, SemanticVersionSerializer())
        .setPrettyPrinting()
        .create()

    @get:Input
    @set:Option(option = "url", description = "Plugin download url")
    abstract var url: String

    private fun extractManifest(bytes: ByteArray): PluginManifest {
        var manifest: PluginManifest? = null

        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use {
            for (zipEntry in generateSequence { it.nextEntry }) {
                if (zipEntry.name == "manifest.json") {
                    manifest = this.gson.fromJson(String(it.readAllBytes()), PluginManifest::class.java)
                }
            }
        }

        requireNotNull(manifest) {
            "Couldn't find the manifest.json file"
        }

        return manifest!!
    }

    @TaskAction
    fun add() {
        val bytes = URL(url).openStream().readAllBytes()

        val hash = org.apache.commons.codec.digest.DigestUtils.sha1Hex(bytes)
        val manifest = extractManifest(bytes)

        val jsonFile = project.file("plugins").resolve(manifest.name + ".json")

        val versions =
            if (jsonFile.exists()) gson.fromJson(jsonFile.readText(), FullPluginInfo::class.java).versions
            else TreeMap()

        versions[manifest.version] = VersionInfo(
            if (manifest.changelog == null) null else ChangelogInfo(
                manifest.changelog,
                manifest.changelogMedia
            ), DownloadInfo(url, hash)
        )

        versions.comparator()

        jsonFile.writeText(
            gson.toJson(
                FullPluginInfo(
                    manifest.name,
                    manifest.description,
                    manifest.authors,
                    manifest.links ?: HashMap(),
                    versions
                )
            ) + "\n"
        )
    }
}

task<AddTask>("add") {
    group = "aliucord"
}
