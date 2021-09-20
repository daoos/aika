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
package network.aika.neuron.inhibitory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.steps.UpdateNet;

import static network.aika.neuron.sign.Sign.POS;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class InhibitorySynapse<I extends Neuron> extends Synapse<I, InhibitoryNeuron, Activation> {

    @Override
    public boolean checkTemplatePropagate(Direction dir, Activation act) {
        return false;
    }

    public void updateSynapse(Link l, double delta) {
        if(!l.getInput().isFired())
            return;

        addWeight(delta);
        UpdateNet.updateNet(l.getOutput(), delta * l.getInputValue(POS));
//        SumUpLink.add(l, l.getInputValue(POS) * delta);
    }

    @Override
    public void updateReference(Link l) {
        l.getOutput().propagateReference(
                l.getInput().getReference()
        );
    }

    @Override
    public Activation branchIfNecessary(Activation iAct, Activation oAct) {
        return oAct;
    }

    @Override
    public void addWeight(double weightDelta) {
        super.addWeight(weightDelta);
        input.getNeuron().setModified(true);
    }

    @Override
    public boolean checkCausality(Activation<?> iAct, Activation<?> oAct) {
        return true;
    }
}
