/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.neuron.activation;

import network.aika.Config;
import network.aika.Thought;
import network.aika.steps.Step;
import network.aika.steps.QueueKey;

import java.util.*;
import java.util.stream.Stream;

/**
 * An Element is either a node (Activation) or an edge (Link) in the Activation graph.
 *
 *  @author Lukas Molzberger
 */
public abstract class Element<E extends Element> implements Comparable<E> {

    private NavigableMap<QueueKey, Step> queuedSteps = new TreeMap<>(QueueKey.ELEMENT_COMPARATOR);

    public abstract Timestamp getFired();

    public void addQueuedStep(Step s) {
        queuedSteps.put(s, s);
    }

    public boolean isQueued(Step s) {
        return queuedSteps.containsKey(s);
    }

    public void removeQueuedPhase(Step s) {
        queuedSteps.remove(s);
    }

    public void copySteps(Element newElement) {
        getQueuedSteps().forEach(s ->
                Step.add(s.copy(newElement))
        );
    }

    public Stream<Step> getQueuedSteps() {
        return queuedSteps.values().stream();
    }

    public abstract Thought getThought();

    public abstract Config getConfig();

    public abstract String toShortString();
}
