package org.move.cli

import com.intellij.openapi.project.Project
import org.move.openapiext.*
import org.move.stdext.chain
import org.toml.lang.psi.*
import java.nio.file.Path
import java.util.*

typealias RawAddressVal = Pair<String, TomlKeyValue>

data class AddressVal(
    val value: String,
    val keyValue: TomlKeyValue?,
    val placeholderKeyValue: TomlKeyValue?
)

typealias RawAddressMap = MutableMap<String, RawAddressVal>
typealias AddressMap = MutableMap<String, AddressVal>
typealias PlaceholderMap = MutableMap<String, TomlKeyValue>

typealias DependenciesMap = SortedMap<String, Dependency>

fun mutableRawAddressMap(): RawAddressMap = mutableMapOf()
fun mutableAddressMap(): AddressMap = mutableMapOf()
fun placeholderMap(): PlaceholderMap = mutableMapOf()

fun AddressMap.copyMap(): AddressMap = this.toMutableMap()

sealed class Dependency {
    data class Local(val absoluteLocalPath: Path, val subst: RawAddressMap) : Dependency()
    data class Git(val dirPath: Path, val subst: RawAddressMap) : Dependency()
}

typealias TomlElementMap = Map<String, TomlValue?>

fun TomlElement.toMap(): TomlElementMap {
    val namedEntries = when (this) {
        is TomlTable -> this.namedEntries()
        is TomlInlineTable -> this.namedEntries()
        else -> null
    }
    return namedEntries.orEmpty().associate { Pair(it.first, it.second) }
}

data class MoveTomlPackageTable(
    val name: String?,
    val version: String?,
    val authors: List<String>,
    val license: String?
)

class MoveToml(
    val project: Project,
    val tomlFile: TomlFile? = null,
    val packageTable: MoveTomlPackageTable? = null,
    val addresses: RawAddressMap = mutableRawAddressMap(),
    val dev_addresses: RawAddressMap = mutableRawAddressMap(),
    val dependencies: DependenciesMap = sortedMapOf(),
    val dev_dependencies: DependenciesMap = sortedMapOf(),
) {
    companion object {
        fun fromTomlFile(tomlFile: TomlFile, projectRoot: Path): MoveToml {
            val packageTomlTable = tomlFile.getTable("package")
            var packageTable: MoveTomlPackageTable? = null
            if (packageTomlTable != null) {
                val name = packageTomlTable.findValue("name")?.stringValue()
                val version = packageTomlTable.findValue("version")?.stringValue()
                val authors =
                    packageTomlTable.findValue("authors")?.arrayValue()
                        .orEmpty()
                        .mapNotNull { it.stringValue() }
                val license = packageTomlTable.findValue("license")?.stringValue()
                packageTable = MoveTomlPackageTable(name, version, authors, license)
            }

            val addresses = parseAddresses("addresses", tomlFile)
            val devAddresses = parseAddresses("dev-addresses", tomlFile)

            val dependencies = parseDependencies("dependencies", tomlFile, projectRoot)
            val devDependencies = parseDependencies("dev-dependencies", tomlFile, projectRoot)

            return MoveToml(
                tomlFile.project,
                tomlFile,
                packageTable,
                addresses,
                devAddresses,
                dependencies,
                devDependencies
            )
        }

        private fun parseAddresses(tableKey: String, tomlFile: TomlFile): RawAddressMap {
            val addresses = mutableMapOf<String, RawAddressVal>()
            val tomlAddresses = tomlFile.getTable(tableKey)?.namedEntries().orEmpty()
            for ((addressName, tomlValue) in tomlAddresses) {
                val value = tomlValue?.stringValue() ?: continue
                addresses[addressName] = Pair(value, tomlValue.keyValue)
            }
            return addresses
        }

        private fun parseDependencies(
            tableKey: String,
            tomlFile: TomlFile,
            projectRoot: Path
        ): DependenciesMap {
            val tomlInlineTableDeps = tomlFile.getTable(tableKey)
                ?.namedEntries().orEmpty()
                .map { Pair(it.first, it.second?.toMap().orEmpty()) }
            val tomlTableDeps = tomlFile
                .getTablesByFirstSegment(tableKey)
                .map { Pair(it.header.key?.segments?.get(1)!!.text, it.toMap()) }

            val dependencies = sortedMapOf<String, Dependency>()
            for ((depName, depMap) in tomlInlineTableDeps.chain(tomlTableDeps)) {
                val dep = when {
                    depMap.containsKey("local") -> parseLocalDependency(depMap, projectRoot)
                    depMap.containsKey("git") -> parseGitDependency(depName, depMap, projectRoot)
                    else -> null
                }
                dependencies[depName] = dep
            }
            return dependencies
        }

        private fun parseLocalDependency(depTable: TomlElementMap, projectRoot: Path): Dependency.Local? {
            val localPathValue = depTable["local"]?.stringValue() ?: return null
            val localPath =
                projectRoot.resolve(localPathValue).toAbsolutePath().normalize()
            val subst = parseAddrSubst(depTable)
            return Dependency.Local(localPath, subst)
        }

        private fun parseGitDependency(
            depName: String,
            depTable: TomlElementMap,
            projectRoot: Path
        ): Dependency.Git {
            val dirPath = projectRoot.resolve("build").resolve(depName)
            val subst = parseAddrSubst(depTable)
            return Dependency.Git(dirPath, subst)
        }

        private fun parseAddrSubst(depTable: TomlElementMap): RawAddressMap {
            val substEntries =
                depTable["addr_subst"]?.inlineTableValue()?.namedEntries().orEmpty()
            val subst = mutableRawAddressMap()
            for ((name, tomlValue) in substEntries) {
                if (tomlValue == null) continue
                val value = tomlValue.stringValue() ?: continue
                val keyValue = tomlValue.keyValue
                subst[name] = Pair(value, keyValue)
            }
            return subst
        }
    }
}