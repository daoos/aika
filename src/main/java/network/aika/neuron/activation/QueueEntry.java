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

import network.aika.neuron.steps.Step;

import java.util.Comparator;

/**
 *
 * @author Lukas Molzberger
 */
public class QueueEntry<P extends Step, E extends Element> {

    public static final Comparator<QueueEntry> COMPARATOR = Comparator
            .<QueueEntry, Fired>comparing(qe -> qe.element.getFired())
            .thenComparing(qe -> qe.timestamp);

    private P step;
    private E element;
    private long timestamp;

    public QueueEntry(P step, E element) {
        this.step = step;
        this.element = element;
    }

    public static <S extends Step, E extends Element> void add(E e, S s) {
        QueueEntry qe = new QueueEntry(s, e);
        e.addQueuedStep(qe);
        e.getThought().addQueueEntry(qe);
    }

    public P getStep() {
        return step;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String toString() {
        return Step.toString(getStep()) + " : " + element.toString();
    }

    public String pendingStepsToString() {
        StringBuilder sb = new StringBuilder();
 //       pendingPhases.forEach(p -> sb.append(p.toString() + ", "));

        return sb.substring(0, Math.max(0, sb.length() - 2));
    }

    public Element getElement() {
        return element;
    }

    public void process() {
        step.process(element);
    }
}
