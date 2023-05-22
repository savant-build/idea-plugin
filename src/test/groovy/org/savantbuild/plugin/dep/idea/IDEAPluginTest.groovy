/*
 * Copyright (c) 2014, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.dep.idea

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.savantbuild.dep.domain.Artifact
import org.savantbuild.dep.domain.Dependencies
import org.savantbuild.dep.domain.DependencyGroup
import org.savantbuild.dep.domain.License
import org.savantbuild.dep.workflow.FetchWorkflow
import org.savantbuild.dep.workflow.PublishWorkflow
import org.savantbuild.dep.workflow.Workflow
import org.savantbuild.dep.workflow.process.CacheProcess
import org.savantbuild.domain.Project
import org.savantbuild.domain.Version
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import static org.testng.Assert.assertEquals

/**
 * Tests the IDEA plugin.
 *
 * @author Brian Pontarelli
 */
class IDEAPluginTest {
  public static Path projectDir

  Output output

  Project project

  IDEAPlugin plugin

  Path cacheDir

  Path integrationDir

  @BeforeSuite
  static void beforeSuite() {
    projectDir = Paths.get("")
    if (!Files.isRegularFile(projectDir.resolve("LICENSE"))) {
      projectDir = Paths.get("../idea-plugin")
    }
  }

  @BeforeMethod
  void beforeMethod() {
    FileTools.prune(projectDir.resolve("build/test"))

    output = new SystemOutOutput(true)
    output.enableDebug()

    def path = projectDir.resolve("build/test")
    Files.createDirectories(path)
    project = new Project(path, output)
    project.group = "org.savantbuild.test"
    project.name = "idea-plugin-test"
    project.version = new Version("1.0")
    project.licenses.add(License.parse("ApacheV2_0", null))

    project.dependencies = new Dependencies(
        new DependencyGroup("compile", true,
            new Artifact("org.savantbuild.test:multiple-versions:1.0.0"),
            new Artifact("org.savantbuild.test:multiple-versions-different-dependencies:1.0.0")
        ),
        new DependencyGroup("runtime", true,
            new Artifact("org.savantbuild.test:intermediate:1.0.0")
        )
    )

    cacheDir = projectDir.resolve("../savant-dependency-management/test-deps/savant")
    integrationDir = projectDir.resolve("../savant-dependency-management/test-deps/integration")

    project.workflow = new Workflow(
        new FetchWorkflow(output,
            new CacheProcess(output, cacheDir.toString(), integrationDir.toString())
        ),
        new PublishWorkflow(
            new CacheProcess(output, cacheDir.toString(), integrationDir.toString())
        ),
        output
    )

    plugin = new IDEAPlugin(project, new RuntimeConfiguration(), output)
  }

  @Test
  void iml() throws Exception {
    def imlFile = projectDir.resolve("build/test/idea-plugin-test.iml")
    Files.copy(projectDir.resolve("src/test/resources/test.iml"), imlFile)

    plugin.iml()

    String actual = new String(Files.readAllBytes(imlFile))
    String expected = new String(Files.readAllBytes(projectDir.resolve("src/test/resources/expected.iml")))
    assertEquals(actual, expected)
  }

  @Test
  void imlModule() throws Exception {
    def imlFile = projectDir.resolve("build/test/idea-plugin-test.iml")
    Files.copy(projectDir.resolve("src/test/resources/test.iml"), imlFile)

    plugin.settings.moduleMap.put("org.savantbuild.test:leaf2:1.0.0", "leaf2-module")

    plugin.iml()

    String actual = new String(Files.readAllBytes(imlFile))
    String expected = new String(Files.readAllBytes(projectDir.resolve("src/test/resources/expected-module.iml")))
    assertEquals(actual, expected)
  }

  @Test
  void noDependencies() throws Exception {
    project.dependencies = null
    project.artifactGraph = null
    def imlFile = projectDir.resolve("build/test/idea-plugin-test.iml")
    Files.copy(projectDir.resolve("src/test/resources/test.iml"), imlFile)

    plugin.iml()

    String actual = new String(Files.readAllBytes(imlFile))
    String expected = new String(Files.readAllBytes(projectDir.resolve("src/test/resources/no-dependencies.iml")))
    assertEquals(actual, expected)
  }
}
