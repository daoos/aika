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

import network.aika.direction.Direction;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.activation.Link;
import network.aika.neuron.bindingsignal.BranchBindingSignal;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

/**
 *
 * @author Lukas Molzberger
 */
public class NegativeFeedbackSynapse extends BindingNeuronSynapse<InhibitoryNeuron> {

    @Override
    public boolean checkBindingSignal(BindingSignal fromBS, Direction dir) {
        return fromBS instanceof BranchBindingSignal;
    }

    @Override
    public boolean checkRelatedBranchBindingSignal(BranchBindingSignal iBS, BranchBindingSignal oBS) {
        return iBS.getOrigin() == oBS;
    }

    @Override
    public boolean isRecurrent() {
        return true;
    }

    @Override
    public boolean checkCausality(Activation<?> iAct, Activation<?> oAct) {
        return true;
    }

    public boolean checkTemplateLink(Activation iAct, Activation oAct) {
        return iAct.getNeuron().getInputSynapse(oAct.getNeuronProvider()) != null;
    }

    @Override
    public void updateSynapse(Link l, double delta) {
        if(l.getInput().isFired() && l.isSelfRef())
            weight.addAndTriggerUpdate(-delta);
    }

    @Override
    public boolean propagateValue(Link<BindingActivation> l) {
        return !l.isSelfRef();
    }

    @Override
    public BindingActivation branchIfNecessary(Activation iAct, BindingActivation oAct) {
        if (getOutput().isInputNeuron())
            return null;

        if (!oAct.isSelfRef(iAct))
            oAct = oAct.createBranch(this);

        return oAct;
    }

    protected void checkConstraints() {
        assert isNegative();
    }
}