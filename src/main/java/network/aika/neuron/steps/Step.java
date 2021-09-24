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
package network.aika.neuron.steps;

import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.Fired;

import java.util.Comparator;

/**
 * @author Lukas Molzberger
 */
public abstract class Step<E extends Element> implements QueueKey, Cloneable {

    public static final Comparator<Step> COMPARATOR = Comparator
            .<Step>comparingInt(s -> s.getPhase().ordinal())
            .thenComparing(s -> s.fired)
            .thenComparing(s -> s.timestamp);

    private E element;

    protected Fired fired;
    private long timestamp;

    public Step(E element) {
        this.element = element;
        this.fired = element.getFired();
    }

    public Step(E element, long timestamp) {
        this.element = element;
        this.fired = element.getFired();
        this.timestamp = timestamp;
    }

    public Step copy(Element newElement) {
        Step newStep = null;
        try {
            newStep = (Step) clone();
        } catch (CloneNotSupportedException e) {
        }
        newStep.element = newElement;
        return newStep;
    }

    public String getStepName() {
        return getClass().getSimpleName();
    }

    public abstract void process();

    public abstract Phase getPhase();

    public abstract StepType getStepType();

    public abstract boolean checkIfQueued();

    static String toString(Step p) {
        return " (" + (p != null ? p.toString() : "X") + ")";
    }

    public static void add(Step s) {
        if(s.checkIfQueued() && s.getElement().isQueued(s))
            return;

        s.getElement().addQueuedStep(s);
        s.getElement().getThought().addStep(s);
    }

    public Fired getFired() {
        return fired;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public void setTimeStamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public E getElement() {
        return element;
    }
}
