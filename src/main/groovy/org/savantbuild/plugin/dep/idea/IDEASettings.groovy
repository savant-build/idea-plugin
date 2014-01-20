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

import org.savantbuild.dep.DependencyService.ResolveConfiguration
import org.savantbuild.dep.DependencyService.ResolveConfiguration.TypeResolveConfiguration

/**
 * Settings for the IDEA plugin.
 *
 * @author Brian Pontarelli
 */
class IDEASettings {
  /**
   * ResolveConfiguration map. This maps IDEA scopes to ResolveConfigurations. These are used to update the .iml file
   * based on the project's dependencies. The default configuration is:
   *
   * <pre>
   *   "PROVIDED": new ResolveConfiguration().with("provided", new TypeResolveConfiguration(true, false)),
   *   "RUNTIME": new ResolveConfiguration().with("runtime", new TypeResolveConfiguration(true, false)),
   *   "COMPILE": new ResolveConfiguration().with("compile", new TypeResolveConfiguration(true, false)),
   *   "TEST": new ResolveConfiguration().with("test-compile", new TypeResolveConfiguration(true, false))
   *                                     .with("test-runtime", new TypeResolveConfiguration(true, false))
   * </pre>
   */
  Map<String, ResolveConfiguration> resolveConfigurationMap = [
      "PROVIDED": new ResolveConfiguration().with("provided", new TypeResolveConfiguration(true, "provided", "compile", "runtime")),
      "COMPILE": new ResolveConfiguration().with("compile", new TypeResolveConfiguration(true, false)),
      "RUNTIME": new ResolveConfiguration().with("runtime", new TypeResolveConfiguration(true, "compile", "runtime")),
      "TEST": new ResolveConfiguration().with("test-compile", new TypeResolveConfiguration(true, false))
                                        .with("test-runtime", new TypeResolveConfiguration(true, "compile", "runtime"))
  ]

  /**
   * Module map. This maps from Savant dependencies in the project build file to modules in the IDEA project. For
   * example, let's say one of your modules depends on another module (foo depends on bar). In your Savant build file
   * for the foo project will have this dependency:
   *
   * <pre>
   *   dependencies {
   *     group("compile") {
   *       dependency(id: "org.example:bar:1.0.0-{integration}")
   *     }
   *   }
   * </pre>
   *
   * Inside your IntelliJ IDEA project, the bar project might be included as a module named "bar-module". Therefore,
   * you will setup this moduleMap:
   *
   * <pre>
   *   idea.settings.moduleMap = ["org.example:bar:1.0.0-{integration}":"bar-module"]
   * </pre>
   */
  Map<String, String> moduleMap = [:]
}
