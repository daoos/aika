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
package network.aika.neuron.conjunctive;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.*;
import network.aika.neuron.axons.Axon;
import network.aika.neuron.bindingsignal.BindingSignal;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class BindingNeuronSynapse<S extends BindingNeuronSynapse, I extends Neuron & Axon, L extends Link<S, IA, BindingActivation>, IA extends Activation> extends ConjunctiveSynapse<S, I, BindingNeuron, L, IA, BindingActivation> {

    @Override
    public boolean linkingCheck(BindingSignal<IA> iBS, BindingSignal<BindingActivation> oBS) {
        if(!Link.isCausal(iBS.getActivation(), oBS.getActivation()))
            return false;

        return super.linkingCheck(iBS, oBS);
    }

    @Override
    protected boolean commonLinkingCheck(BindingSignal<IA> iBS, BindingSignal<BindingActivation> oBS) {
        if(!oBS.getActivation().isMainBranch() &&
                iBS.getActivation().isBoundToConflictingBS(
                        oBS.getActivation().getMainBranch().getBoundPatternBindingSignal()
                )
        )
            return false;

        return super.commonLinkingCheck(iBS, oBS);
    }
}
