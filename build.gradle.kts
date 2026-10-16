plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

// Strip the "v" prefix and rewrite git describe output into a semver-ish string
val gitVersion: String = providers.exec {
    commandLine("git", "describe", "--tags", "--always", "--match", "v*")
}.standardOutput.asText.get().trim().let { raw ->
    // raw is like: v1.0-1-g3f756ed  or  v1.0  or  v1.0-dirty
    when {
        raw.startsWith("fatal:") -> "0.1.0-SNAPSHOT"
        else -> raw
            .removePrefix("v")
            .removeSuffix("-dirty")       // ignore dirty state
            .replace(Regex("-\\d+-g[0-9a-f]+$"), "-SNAPSHOT")
    }
}

val shortHash: String = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim()

group = "xyz.nothing.artaserver"
version = "$gitVersion+$shortHash"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.2.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("printGitVersion") {
    doLast {
        println("version      = ${project.version}")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}