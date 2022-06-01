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

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class SpotlessConventionPluginTest: IntegrationTest() {

    @Test
    fun `plugin applies`() {
        file("gradle/libs.versions.toml").writeText(
            // language = toml
            """
            [versions]
            accompanist = "0.24.8-beta"
            ktlint = "0.43.0"

            [libraries]
            accompanist-flowlayout = { group = "com.google.accompanist", name = "accompanist-flowlayout", version.ref = "accompanist" }
            """.trimIndent()
        )

        file("build.gradle").writeText(
            // language = groovy
            """
              buildscript {
                dependencies {
                  classpath files($pluginsClasspath)
                }
              }
        
              apply plugin: "nowinandroid.spotless"
        
              tasks.register("assertValues") {
                doLast {
                    assert plugins.hasPlugin("nowinandroid.spotless")
                }
              }
        """.trimIndent())

        run("assertValues").run {
            assertThat(task(":assertValues")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

    }
}