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
package network.aika;


import network.aika.callbacks.EventListener;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.StepType;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Thought<M extends Model> {

    protected final M model;
    private long absoluteBegin;

    private Timestamp timestampOnProcess = new Timestamp(0);
    private long timestampCounter = 0;
    private int activationIdCounter = 0;

    private final TreeSet<Step> queue = new TreeSet<>(Step.COMPARATOR);

    private final Set<StepType> filters = new TreeSet<>();

    private final TreeMap<Integer, Activation> activationsById = new TreeMap<>();

    private Map<NeuronProvider, SortedSet<Activation>> actsPerNeuron = null;

    private final List<EventListener> eventListeners = new ArrayList<>();

    private Config config;


    public Thought(M m) {
        model = m;
        absoluteBegin = m.getN();
    }

    public void updateModel() {
        model.addToN(length());
    }

    public M getModel() {
        return model;
    }

    public abstract int length();

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void addFilters(StepType... st) {
        filters.addAll(Set.of(st));
    }

    public void onActivationCreationEvent(Activation act, Synapse originSynapse, Activation originAct) {
        getEventListeners()
                .forEach(
                        el -> el.onActivationCreationEvent(act, originSynapse, originAct)
                );
    }

    public void beforeProcessedEvent(Step s) {
        getEventListeners()
                .forEach(
                        el -> el.beforeProcessedEvent(s)
                );
    }

    public void afterProcessedEvent(Step s) {
        getEventListeners()
                .forEach(
                        el -> el.afterProcessedEvent(s)
                );
    }

    public void onLinkCreationEvent(Link l) {
        getEventListeners()
                .forEach(
                        el -> el.onLinkCreationEvent(l)
                );
    }

    public synchronized Collection<EventListener> getEventListeners() {
        return new ArrayList<>(eventListeners);
    }

    public synchronized void addEventListener(EventListener l) {
        eventListeners.add(l);
    }

    public synchronized void removeEventListener(EventListener l) {
        eventListeners.remove(l);
    }

    public void registerActivation(Activation act) {
        activationsById.put(act.getId(), act);
    }

    public void registerBindingSignal(Activation act, BindingSignal bs) {
    }

    public void addStep(Step s) {
        if(filters.contains(s.getStepType()))
            return;

        s.setTimeStamp(getNextTimestamp());
        queue.add(s);
    }

    public void removeQueueEntry(Step s) {
        boolean isRemoved = queue.remove(s);
        assert isRemoved;
    }

    public void removeQueueEntries(Collection<Step> s) {
        queue.removeAll(s);
    }

    public SortedSet<Step> getQueue() {
        return queue;
    }

    public Range getRange() {
        return new Range(absoluteBegin, absoluteBegin + length());
    }

    public void process() {
        while (!queue.isEmpty()) {
            Step s = queue.pollFirst();

            timestampOnProcess = getCurrentTimestamp();

            s.getElement().removeQueuedPhase(s);

            beforeProcessedEvent(s);
            s.process();
            afterProcessedEvent(s);
        }
    }

    public Timestamp getTimestampOnProcess() {
        return timestampOnProcess;
    }

    public Timestamp getCurrentTimestamp() {
        return new Timestamp(timestampCounter);
    }

    public Timestamp getNextTimestamp() {
        return new Timestamp(timestampCounter++);
    }

    public <E extends Element> List<Step> getStepsByElement(E element) {
        return queue
                .stream()
                .filter(s -> s.getElement() == element)
                .collect(Collectors.toList());
    }

    public int createActivationId() {
        return activationIdCounter++;
    }

    public Activation getActivation(Integer id) {
        return activationsById.get(id);
    }

    public Collection<Activation> getActivations() {
        return activationsById.values();
    }

    public int getNumberOfActivations() {
        return activationsById.size();
    }

    public Set<Activation> getActivations(NeuronProvider n) {
        return getActivations(n.getNeuron());
    }

    public Set<Activation> getActivations(Neuron n) {
        if(actsPerNeuron == null) {
            actsPerNeuron = getActivationsPerNeuron();
        }

        Set<Activation> acts = actsPerNeuron.get(n.getProvider());
        return acts != null ? acts : Collections.emptySet();
    }

    private Map<NeuronProvider, SortedSet<Activation>> getActivationsPerNeuron() {
        Map<NeuronProvider, SortedSet<Activation>> results = new TreeMap<>();

        activationsById.values().stream()
                .filter(act -> act.isFired())
                .forEach(act -> {
                    Set<Activation> acts = results.computeIfAbsent(
                            act.getNeuronProvider(),
                            n -> new TreeSet<>()
                    );
                    acts.add(act);
                });

        return results;
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean includeLink) {
        StringBuilder sb = new StringBuilder();
        for(Activation act: activationsById.values()) {
/*            if(!act.isActive())
                continue;
*/
            sb.append(act.toString(includeLink));
            sb.append("\n");
        }

        return sb.toString();
    }

    public String gradientsToString() {
        StringBuilder sb = new StringBuilder();

        activationsById.values()
                .forEach(act -> sb.append(act.gradientsToString()));

        return sb.toString();
    }
}
