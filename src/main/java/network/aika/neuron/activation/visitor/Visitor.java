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
package network.aika.neuron.activation.visitor;

import network.aika.Thought;
import network.aika.callbacks.VisitorEvent;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.scopes.Scope;
import network.aika.neuron.steps.VisitorStep;

import java.util.Set;
import java.util.TreeSet;

import static network.aika.neuron.activation.direction.Direction.*;

/**
 * The Visitor is a finite state machine used to implement the linking process in the neural network.
 * It traverses the network of activations in order to find yet unlinked synapses.
 *
 * @author Lukas Molzberger
 */
public abstract class Visitor {
    protected ActVisitor origin;
    private Visitor previousStep;
    protected VisitorStep phase;

    public Direction downUpDir;
    public Direction startDir;

    public int downSteps = 0;
    public int upSteps = 0;

    protected Set<Scope> visitedScopes;

    protected Visitor() {}

    protected Visitor prepareNextStep(Visitor nv) {
        nv.phase = phase;
        nv.previousStep = this;
        nv.origin = origin;
        nv.downUpDir = downUpDir;
        nv.startDir = startDir;
        nv.upSteps = upSteps;

        return nv;
    }

    public Visitor getOrigin() {
        return origin;
    }

    public Visitor getPreviousStep() {
        return previousStep;
    }

    public Direction getDownUpDir() {
        return downUpDir;
    }

    public Direction getStartDir() {
        return startDir;
    }

    public int getDownSteps() {
        return downSteps;
    }

    public int getUpSteps() {
        return upSteps;
    }

    public VisitorStep getPhase() {
        return phase;
    }

    public Activation getOriginAct() {
        return origin.act;
    }

    public void incrementPathLength() {
        if (downUpDir == INPUT) {
            this.downSteps++;
        } else {
            this.upSteps++;
        }
    }

    public boolean getSelfRef() {
        return downSteps == 0 || upSteps == 0;
    }

    public int numSteps() {
        return downSteps + upSteps;
    }

    private Thought getThought() {
        return origin.act.getThought();
    }

    public void onEvent(VisitorEvent ve, Synapse s) {
        getThought().onVisitorEvent(this, ve, s);
    }

    public String toStringRecursive() {
        if(previousStep != null)
            return previousStep.toStringRecursive() + "\n" + this;
        else
            return "" + this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("DownUp:" + downUpDir + ", ");
        sb.append("StartDir:" + startDir + ", ");

        sb.append("DownSteps:" + downSteps + ", ");
        sb.append("UpSteps:" + upSteps + "");

        return sb.toString();
    }
}