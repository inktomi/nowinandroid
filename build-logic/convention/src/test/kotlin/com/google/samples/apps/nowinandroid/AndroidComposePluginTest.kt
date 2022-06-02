/*
 * Copyright 2022 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.google.samples.apps.nowinandroid

import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.Optional

class AndroidComposePluginTest {
    private val project: Project = ProjectBuilder.builder().build()

    @Test
    fun `plugin applies`() {
        class FakeModuleIdentifier(private val group: String, private val name: String) :
            ModuleIdentifier {
            override fun getGroup(): String = group
            override fun getName(): String = name
        }

        class FakeVersionConstraint : VersionConstraint {
            override fun getDisplayName() = "androidxCompose"
            override fun getBranch(): String? = null
            override fun getRequiredVersion() = "1.0.0"
            override fun getPreferredVersion()= "1.0.0"
            override fun getStrictVersion()= "1.0.0"
            override fun getRejectedVersions() = mutableListOf<String>()
        }

        class FakeMinimalExternalModuleDependency(private val module: ModuleIdentifier, private val versionConstraint: VersionConstraint) :
            MinimalExternalModuleDependency {
            override fun getModule(): ModuleIdentifier = module
            override fun getVersionConstraint(): VersionConstraint = versionConstraint
        }

        open class FakeVersionCatalog(val libraries: Map<String, MinimalExternalModuleDependency>, val versions: Map<String, VersionConstraint>) : VersionCatalog by stub() {
            override fun findLibrary(alias: String): Optional<Provider<MinimalExternalModuleDependency>> =
                Optional.of(project.provider { libraries[alias] })

            override fun findVersion(alias: String): Optional<VersionConstraint> {
                return Optional.ofNullable(versions[alias])
            }
        }

        open class FakeVersionCatalogsExtension(val catalogs: Map<String, VersionCatalog>) :
            VersionCatalogsExtension {
            override fun iterator() = catalogs.toMutableMap().values.iterator()
            override fun find(name: String): Optional<VersionCatalog> = Optional.ofNullable(catalogs[name])
            override fun getCatalogNames() = catalogs.map { it.value.name }.toSet()
        }

        project.run {
            val fakeLibrary = FakeMinimalExternalModuleDependency(FakeModuleIdentifier("com.androidx.compose", "compose-ui"), FakeVersionConstraint())
            val fakeCatalog = FakeVersionCatalog(mapOf("libs" to fakeLibrary), mapOf("androidxCompose" to FakeVersionConstraint()))
            extensions.create("versionCatalogs", FakeVersionCatalogsExtension::class.java, mapOf("libs" to fakeCatalog))

            plugins.run {
                apply("nowinandroid.android.application.compose")

                assertThat(hasPlugin("nowinandroid.android.application.compose")).isTrue()
            }
        }
    }
}

inline fun <reified T : Any> stub(): T =
    Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf<Class<*>>(T::class.java)
    ) { _, method, args ->
        val argsString = buildString {
            append("(")
            if (!args.isNullOrEmpty()) {
                appendLine()
                args.forEachIndexed { paramIndex, paramValue ->
                    appendLine("  arg${paramIndex}: ${method.parameterTypes[paramIndex].canonicalName} = $paramValue (type: ${paramValue.javaClass}),")
                }
            }
            append(")")
        }
        throw NotImplementedError("stub() not implemented:\n${T::class.qualifiedName}.${method.name}$argsString : ${method.returnType.canonicalName}")
    } as T