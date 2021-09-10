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
package network.aika.neuron.steps.link;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.activation.CheckIfFired;
import network.aika.neuron.steps.activation.PropagateGradientsNet;
import network.aika.utils.Utils;


/**
 * Uses the input activation value, and the synapse weight to update the net value of the output activation.
 *
 * @author Lukas Molzberger
 */
public class SumUpLink extends Step<Link> {

    private final double delta;

    public static void add(Link l, double delta) {
    }

    public SumUpLink(Link l, double delta) {
        super(l);
        this.delta = delta;
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    public boolean checkIfQueued() {
        return false;
    }

    @Override
    public void process() {
        getElement().sumUpLink(delta);

        Activation oAct = getElement().getOutput();

        PropagateGradientsNet.add(oAct);
        CheckIfFired.add(oAct);
    }

    public String toString() {
        return "Link-Step: Sum up Link (" + Utils.round(delta) + ")";
    }
}
