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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Get the Gradle test kit directory to use. This is set in the buildSrc.gradle.kts
 * and is used so that we can clean it up if the tests are starting to have issues.
 * Top of file so it is only loaded once.
 */
private val testKitDirectory = File(System.getProperty("testKitDirectory"))

/**
 * Only load the resource once and propagated it out in IntegrationTest. See [IntegrationTest.pluginsClasspath]
 */
private val pluginsClasspath = requireNotNull(IntegrationTest::class.java.classLoader.getResource("plugin-classpath.txt"))
    .readText()
    .split("\n")
    .joinToString { "'$it'" }

/**
 * Extend this class to get helpful functions for creating functional gradle tests.
 * See [here](https://docs.gradle.org/current/userguide/test_kit.html) for more details.
 *
 * @param setup - lambda to be executed before each test is run. Use this to do the same thing before every test run.
 * Equivalent to using `@Before`, but since `IntegrationTest` already uses this annotation we want to be explicit
 * in by using the lambda and control the ordering.
 *
 * Example:
 * ```
 * MyIntegrationTest: IntegrationTest(setup = { println("HI SETUP!") })
 * ```
 *
 * Example of calling methods in overriden class:
 * ```
 * MyIntegrationTest: IntegrationTest() {
 *   init {
 *     setup = {
 *       mySetupMethod()
 *     }
 *   }
 * }
 * ```
 */
abstract class IntegrationTest(var setup: () -> Unit = {}) {

    /**
     * Directory of the gradle project that is under test. Will be cleaned up after each test run.
     */
    @Rule
    @JvmField
    val dir = TemporaryFolder()

    /**
     * Only load the resource once and propagated it out in IntegrationTest. See [IntegrationTest.pluginsClasspath]
     */
    val pluginsClasspath = requireNotNull(IntegrationTest::class.java.classLoader.getResource("plugin-classpath.txt"))
        .readText()
        .split("\n")
        .joinToString { "'$it'" }


    @Before
    fun integrationTestSetup() {
        file("settings.gradle").writeText("rootProject.name = 'test-app'")

        val androidHome = System.getProperty("ANDROID_SDK_ROOT")
        if (!androidHome.isNullOrBlank()) {
            file("local.properties").writeText("sdk.dir=$androidHome")
        }

        setup()
    }

    /**
     * Run a gradle with the input arguments. This run will fail the test if a failure occurs. If the expected
     * result is to fail use [IntegrationTest.runAndFail].
     * @param args the arguments to pass to gradle.
     */
    fun run(vararg args: String): BuildResult = gradleRunner(*args).build()

    /**
     * Like [IntegrationTest.run] but will allow build failures.
     * @param args the arguments to pass to gradle.
     */
    fun runAndFail(vararg args: String): BuildResult = gradleRunner(*args).buildAndFail()

    private fun gradleRunner(vararg args: String) = GradleRunner.create()
        .withProjectDir(dir.root)
        .forwardOutput()
        .withTestKitDir(testKitDirectory)
        .withArguments(*args)

    /**
     * Create a file in the project that is under test's directory. Paths from the root directory may be used
     * and this function will automatically create any missing directories. For example `file("src/main/kotlin/Test.kt")`
     * will create the file `Test.kt` and all of it's parent directories if they do not already exist.
     */
    fun file(path: String): File = File(dir.root, path).also { assert(it.parentFile.mkdirs() || it.parentFile.exists()) }
}
