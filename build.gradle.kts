buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.google.code.gson:gson:2.8.8")
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
    val versions: HashMap<String, VersionInfo>
) : BasePluginInfo()

data class CompiledPluginInfo(
    override val name: String,
    override val description: String,
    override val authors: List<AuthorInfo>,
    override val links: HashMap<String, String>,
    val latest: VersionInfo
) : BasePluginInfo() {
    constructor(original: FullPluginInfo) : this(
        original.name,
        original.description,
        original.authors,
        original.links,
        original.versions.values.first()
    )
}

data class AuthorInfo(
    val id: Long,
    val name: String
)

data class VersionInfo(
    val changelog: String,
    val download: DownloadInfo
)

data class DownloadInfo(
    val url: String,
    val sha1: String
)

val gson = com.google.gson.Gson()

task("compile") {
    group = "aliucord"

    val plugins = kotlin.collections.ArrayList<CompiledPluginInfo>()

    val pluginsDirectory = buildDir.resolve("plugins")
    pluginsDirectory.mkdirs()

    fileTree("plugins").forEach {
        val plugin = gson.fromJson(it.readText(), FullPluginInfo::class.java)

        pluginsDirectory.resolve(plugin.name).mkdir()

        val file = pluginsDirectory.resolve(plugin.name + ".json")
        file.writeText(gson.toJson(plugin))

        plugins.add(CompiledPluginInfo(plugin))
    }

    buildDir.resolve("plugins.json").writeText(gson.toJson(plugins))
}