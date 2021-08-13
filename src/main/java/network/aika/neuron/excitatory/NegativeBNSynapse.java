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
package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.Visitor;

/**
 *
 * @author Lukas Molzberger
 */
public class NegativeBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    @Override
    public boolean isRecurrent() {
        return true;
    }

    @Override
    public void transition(ActVisitor v, Synapse s, Link l) {
        s.negativeSynapseTransitionLoop(v, l);
    }

    public void alternateBranchTransition(ActVisitor v, Synapse s, Link l) {
        l.follow(v);
    }

    @Override
    protected boolean checkCausality(Activation fromAct, Activation toAct, Visitor v) {
        return true;
    }

    @Override
    public void updateSynapse(Link l, double delta) {
        if(l.getInput().isActive(true) && l.isSelfRef())
            addWeight(-delta);
    }

    @Override
    public void updateReference(Link l) {
    }

    @Override
    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron())
            return null;

        if (!v.getSelfRef())
            oAct = oAct.createBranch(this);

        return oAct;
    }
}
