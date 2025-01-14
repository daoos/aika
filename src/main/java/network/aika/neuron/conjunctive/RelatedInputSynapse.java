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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.PrimaryInputLink;
import network.aika.neuron.activation.RelatedInputLink;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.State.*;
import static network.aika.neuron.bindingsignal.Transition.transition;


/**
 *
 * @author Lukas Molzberger
 */
public class RelatedInputSynapse extends BindingNeuronSynapse<RelatedInputSynapse, BindingNeuron, RelatedInputLink, BindingActivation> {

    private static List<Transition> TRANSITIONS = List.of(
            transition(SAME, INPUT, true, Integer.MAX_VALUE),
            transition(INPUT, INPUT, true, Integer.MAX_VALUE)
    );

    private static List<Transition> TRANSITIONS_TEMPLATE = List.of(
            transition(SAME, INPUT, true, Integer.MAX_VALUE)
    );

    @Override
    public RelatedInputLink createLink(BindingActivation input, BindingActivation output, boolean isSelfRef) {
        return new RelatedInputLink(this, input, output, isSelfRef);
    }

    @Override
    public List<Transition> getTransitions() {
        return isTemplate() ?
                TRANSITIONS_TEMPLATE :
                TRANSITIONS;
    }

    @Override
    public boolean linkingCheck(BindingSignal<BindingActivation> iBS, BindingSignal<BindingActivation> oBS) {
        if(iBS.getState() == SAME && !(oBS.getLink() instanceof PrimaryInputLink<?>))
            return false;

        if(iBS.getState() == INPUT && !verifySameBindingSignal(iBS, oBS))
            return false;

        return super.linkingCheck(iBS, oBS);
    }

    private boolean verifySameBindingSignal(BindingSignal iBS, BindingSignal oBS) {
        BindingActivation iAct = (BindingActivation) iBS.getActivation();
        BindingActivation oAct = (BindingActivation) oBS.getActivation();
        BindingSignal sameSB = iAct.getBoundPatternBindingSignal();
        if(sameSB == null)
            return false;

        PrimaryInputSynapse primaryInputSyn = oAct.getNeuron().getPrimaryInputSynapse();
        if(primaryInputSyn == null)
            return false;

        Activation originAct = sameSB.getOriginActivation();
        return originAct.getReverseBindingSignals(primaryInputSyn.getInput())
                .findAny()
                .isPresent();
    }
}
