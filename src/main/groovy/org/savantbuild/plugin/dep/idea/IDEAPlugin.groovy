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

import org.savantbuild.dep.domain.Artifact
import org.savantbuild.dep.domain.ResolvedArtifact
import org.savantbuild.dep.graph.ResolvedArtifactGraph
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.plugin.dep.DependencyPlugin
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.RuntimeConfiguration
import org.savantbuild.util.Graph

import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser

/**
 * IntelliJ IDEA plugin.
 *
 * @author Brian Pontarelli
 */
class IDEAPlugin extends BaseGroovyPlugin {
  DependencyPlugin dependencyPlugin

  IDEASettings settings

  IDEAPlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)
    settings = new IDEASettings(project)
    dependencyPlugin = new DependencyPlugin(project, runtimeConfiguration, output)
  }

  /**
   * Updates the project's .iml file using its dependencies. The optional closure is invoked and passed the Groovy
   * {@link Node} instance for the root element of the .iml file.
   *
   * @param closure The closure.
   */
  void iml(Closure closure = null) {
    Path imlFile = project.directory.resolve(FileTools.toPath(settings.imlFile))
    if (!Files.isRegularFile(imlFile) || !Files.isReadable(imlFile) || !Files.isWritable(imlFile)) {
      fail("IntelliJ IDEA module file [${imlFile}] doesn't exist or isn't readable and writable")
    }

    output.infoln("Updating the project IML file [${imlFile}]")

    Node root = new XmlParser().parse(imlFile.toFile())
    Node component = root.component.find { it.@name == 'NewModuleRootManager' }
    if (!component) {
      fail("Invalid IntelliJ IDEA module file [${imlFile}]. It doesn't appear to be valid.")
    }

    // Remove the libraries
    component.findAll { it.@type == "module-library" }.each { component.remove(it) }

    // Remove the modules
    component.findAll { it.@type == "module" }.each { component.remove(it) }

    // Setup the dependency/module mapping so we can exclude them
    Map<Artifact, String> dependencyModuleMap = [:]
    settings.moduleMap.each { id, module ->
      dependencyModuleMap.put(new Artifact(id), module)
    }

    // Add the dependencies
    if (project.artifactGraph != null) {
      addDependencies(dependencyModuleMap, component)
    }

    // Call the closure if it exists
    if (closure) {
      closure.call(root)
    }

    // Write out the .iml file
    def writer = new StringWriter()
    new XmlNodePrinter(new PrintWriter(writer), "  ").print(root)
    def result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + writer.toString().replace("/>", " />").trim() + "\n\n"
    output.debugln("New .iml file is\n\n%s", result)
    imlFile.toFile().write(result)

    output.infoln("Update complete")
  }

  private void addDependencies(dependencyModuleMap, component) {
    String userHome = System.getProperty("user.home")
    Set<ResolvedArtifact> addedToIML = new HashSet<>()
    settings.dependenciesMap.each { scope, dependencySet ->
      ResolvedArtifactGraph graph = dependencyPlugin.resolve {
        dependencySet.each { deps -> dependencies(deps) }
      }

      if (graph.size() > 0) {
        graph.traverse(graph.root, false, null, { origin, destination, edge, depth, isLast ->
          // If we already added the artifact, we can skip it and keep traversing because this group could be transitive
          // and we need to traverse down the graph still.
          if (addedToIML.contains(destination)) {
            return true
          }

          // Handle the module case and the library case
          Artifact dependency = destination.toArtifact()
          if (dependencyModuleMap.containsKey(dependency)) {
            component.appendNode("orderEntry", appendScope(["type": "module", "module-name": dependencyModuleMap.get(dependency)], scope))
          } else {
            Node orderEntry = component.appendNode("orderEntry", appendScope(["type": "module-library"], scope))
            Node library = orderEntry.appendNode("library")
            library.appendNode("CLASSES").appendNode("root", [url: "jar://${destination.file.toRealPath().toString().replace(userHome, "\$USER_HOME\$")}!/"])
            library.appendNode("JAVADOC")
            Node source = library.appendNode("SOURCES")
            if (destination.sourceFile != null) {
              source.appendNode("root", [url: "jar://${destination.sourceFile.toRealPath().toString().replace(userHome, "\$USER_HOME\$")}!/"])
            }
          }

          addedToIML.add(destination)
        } as Graph.GraphConsumer)
      }
    }
  }

  private static Map<String, String> appendScope(Map<String, String> attributes, String scope) {
    if (scope != "COMPILE") {
      attributes["scope"] = scope
    }
    return attributes
  }
}
