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

package org.gradle.integtests.tooling.r213

import org.gradle.integtests.tooling.fixture.CompositeToolingApiSpecification
import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.tooling.*
import org.gradle.tooling.model.eclipse.EclipseProject
/**
 * Tooling client provides progress listener for composite model request
 */
class ProgressListenerCompositeBuildCrossVersionSpec extends CompositeToolingApiSpecification {
    static final List<String> IGNORED_EVENTS = ['Validate distribution', '', 'Compiling script into cache', 'Build']
    AbstractCapturingProgressListener progressListenerForComposite
    AbstractCapturingProgressListener progressListenerForRegularBuild

    def "compare old listener events from a composite build and a regular build with single build"() {
        given:
        def builds = createBuilds(1)
        createListeners(CapturingProgressListener)

        when:
        requestModels(builds)
        then:
        assertListenerReceivedSameEventsInCompositeAndRegularConnections()
    }

    @TargetGradleVersion(">=2.5")
    def "compare new listener events from a composite build and a regular build with single build"() {
        given:
        def builds = createBuilds(1)
        createListeners(CapturingEventProgressListener)

        when:
        requestModels(builds)
        then:
        assertListenerReceivedSameEventsInCompositeAndRegularConnections()
    }

    def "compare old listener events executing tasks from a composite build and a regular build with single build"() {
        given:
        def builds = createBuilds(1)
        createListeners(CapturingProgressListener)

        when:
        executeFirstBuild(builds)
        then:
        assertListenerReceivedSameEventsInCompositeAndRegularConnections()
    }

    @TargetGradleVersion(">=2.5")
    def "compare new listener events executing tasks from a composite build and a regular build with single build"() {
        given:
        def builds = createBuilds(1)
        createListeners(CapturingEventProgressListener)

        when:
        executeFirstBuild(builds)
        then:
        assertListenerReceivedSameEventsInCompositeAndRegularConnections()
    }

    def "compare old listener events from a composite build and a regular build with 3 builds"() {
        given:
        def builds = createBuilds(3)
        createListeners(CapturingProgressListener)

        when:
        requestModels(builds)
        then:
        assertListenerReceivedSameEventsInCompositeAndRegularConnections()
    }

    @TargetGradleVersion(">=2.5")
    def "compare new listener events from a composite build and a regular build with 3 builds"() {
        given:
        def builds = createBuilds(3)
        createListeners(CapturingEventProgressListener)

        when:
        requestModels(builds)
        then:
        assertListenerReceivedSameEventsInCompositeAndRegularConnections()
    }

    def "compare old listener events from task execution from a composite build and a regular build with 3 builds"() {
        given:
        def builds = createBuilds(3)
        createListeners(CapturingProgressListener)

        when:
        executeFirstBuild(builds)
        then:
        assertListenerReceivedSameEventsInCompositeAndRegularConnections()
    }

    @TargetGradleVersion(">=2.5")
    def "compare new listener events from task execution from a composite build and a regular build with 3 builds"() {
        given:
        def builds = createBuilds(3)
        createListeners(CapturingEventProgressListener)

        when:
        executeFirstBuild(builds)
        then:
        assertListenerReceivedSameEventsInCompositeAndRegularConnections()
    }

    private void createListeners(Class progressListenerType) {
        progressListenerForComposite = DirectInstantiator.instantiate(progressListenerType)
        progressListenerForRegularBuild = DirectInstantiator.instantiate(progressListenerType)
    }

    private void assertListenerReceivedSameEventsInCompositeAndRegularConnections() {
        assert !progressListenerForRegularBuild.eventDescriptions.isEmpty()
        assert !progressListenerForComposite.eventDescriptions.isEmpty()
        progressListenerForRegularBuild.eventDescriptions.each { eventDescription ->
            if (!(eventDescription in IGNORED_EVENTS)) {
                assert progressListenerForComposite.eventDescriptions.contains(eventDescription)
                progressListenerForComposite.eventDescriptions.remove(eventDescription)
            }
        }
    }

    private List<File> createBuilds(int numberOfBuilds) {
        def builds = (1..numberOfBuilds).collect {
            populate("build-$it") {
                buildFile << "apply plugin: 'java'"
            }
        }
        return builds
    }

    private void requestModels(List<File> builds) {
        withCompositeConnection(builds) { connection ->
            getModels(connection.models(EclipseProject), progressListenerForComposite)
        }

        builds.each { buildDir ->
            GradleConnector connector = toolingApi.connector()
            connector.forProjectDirectory(buildDir.absoluteFile)
            toolingApi.withConnection(connector) { ProjectConnection connection ->
                getModels(connection.model(EclipseProject), progressListenerForRegularBuild)
            }
        }
    }

    private void executeFirstBuild(List<File> builds) {
        withCompositeBuildParticipants(builds) { connection, List buildIds ->
            def buildId = buildIds[0]
            runBuild(connection.newBuild(buildId), progressListenerForComposite)
        }

        GradleConnector connector = toolingApi.connector()
        connector.forProjectDirectory(builds[0].absoluteFile)
        toolingApi.withConnection(connector) { ProjectConnection connection ->
            runBuild(connection.newBuild(), progressListenerForRegularBuild)
        }
    }

    private def getModels(ModelBuilder modelBuilder, progressListener) {
        modelBuilder.addProgressListener(progressListener)
        modelBuilder.get()
    }

    private void runBuild(BuildLauncher buildLauncher, progressListener) {
        buildLauncher.forTasks("jar")
        buildLauncher.addProgressListener(progressListener)
        buildLauncher.run()
    }

    static abstract class AbstractCapturingProgressListener {
        def eventDescriptions = []
    }

    static class CapturingProgressListener extends AbstractCapturingProgressListener implements ProgressListener {
        @Override
        void statusChanged(ProgressEvent event) {
            eventDescriptions.add(event.description)
        }
    }

    static class CapturingEventProgressListener extends AbstractCapturingProgressListener implements org.gradle.tooling.events.ProgressListener {
        @Override
        void statusChanged(org.gradle.tooling.events.ProgressEvent event) {
            eventDescriptions.add(event.descriptor.name)
        }
    }
}
