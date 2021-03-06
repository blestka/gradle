/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.tooling.internal.connection;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.connection.ProjectIdentity;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.tooling.internal.protocol.DefaultProjectIdentity;

import java.io.File;
import java.util.concurrent.TimeUnit;

class ParticipantConnector {
    private final GradleConnectionParticipant build;
    private final File gradleUserHome;
    private final File projectDirectory;
    private final File daemonBaseDir;
    private final Integer daemonMaxIdleTimeValue;
    private final TimeUnit daemonMaxIdleTimeUnits;

    public ParticipantConnector(GradleConnectionParticipant build, File gradleUserHome, File daemonBaseDir, Integer daemonMaxIdleTimeValue, TimeUnit daemonMaxIdleTimeUnits) {
        this(build, build.getProjectDir(), gradleUserHome, daemonBaseDir, daemonMaxIdleTimeValue, daemonMaxIdleTimeUnits);
    }

    private ParticipantConnector(GradleConnectionParticipant build, File projectDirectory, File gradleUserHome, File daemonBaseDir, Integer daemonMaxIdleTimeValue, TimeUnit daemonMaxIdleTimeUnits) {
        this.build = build;
        this.projectDirectory = projectDirectory;
        this.gradleUserHome = gradleUserHome;
        this.daemonBaseDir = daemonBaseDir;
        this.daemonMaxIdleTimeValue = daemonMaxIdleTimeValue;
        this.daemonMaxIdleTimeUnits = daemonMaxIdleTimeUnits;
    }

    public ParticipantConnector withProjectDirectory(File projectDirectory) {
        return new ParticipantConnector(build, projectDirectory, gradleUserHome, daemonBaseDir, daemonMaxIdleTimeValue, daemonMaxIdleTimeUnits);
    }

    public ProjectIdentity toProjectIdentity(String projectPath) {
        return new DefaultProjectIdentity(build.toBuildIdentity(), projectPath);
    }

    public ProjectConnection connect() {
        return connector().forProjectDirectory(projectDirectory).connect();
    }

    private GradleConnector connector() {
        DefaultGradleConnector connector = (DefaultGradleConnector) GradleConnector.newConnector();
        connector.useGradleUserHomeDir(gradleUserHome);
        if (daemonBaseDir != null) {
            connector.daemonBaseDir(daemonBaseDir);
        }
        if (daemonMaxIdleTimeValue != null) {
            connector.daemonMaxIdleTime(daemonMaxIdleTimeValue, daemonMaxIdleTimeUnits);
        }
        if (isRoot()) {
            connector.searchUpwards(false);
        }
        configureDistribution(connector);
        return connector;
    }

    private boolean isRoot() {
        return build.getProjectDir().equals(projectDirectory);
    }

    private void configureDistribution(GradleConnector connector) {
        if (build.getGradleDistribution() == null) {
            if (build.getGradleHome() == null) {
                if (build.getGradleVersion() == null) {
                    connector.useBuildDistribution();
                } else {
                    connector.useGradleVersion(build.getGradleVersion());
                }
            } else {
                connector.useInstallation(build.getGradleHome());
            }
        } else {
            connector.useDistribution(build.getGradleDistribution());
        }
    }

}
