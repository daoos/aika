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

import network.aika.fields.Field;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.PositiveFeedbackLink;
import network.aika.neuron.axons.PatternAxon;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;

import static network.aika.neuron.bindingsignal.Transition.transition;

/**
 *
 * @author Lukas Molzberger
 */
public class PositiveFeedbackSynapse<I extends Neuron & PatternAxon, IA extends Activation> extends BindingNeuronSynapse<PositiveFeedbackSynapse, I, PositiveFeedbackLink<IA>, IA> {

    private static List<Transition> TRANSITIONS = List.of(
            transition(State.BRANCH, State.BRANCH, true, 1),
            transition(State.SAME, State.SAME, true, Integer.MAX_VALUE)
    );

    private Field feedbackWeight = new Field(this, "feedbackWeight");
    private Field feedbackBias = new Field(this, "feedbackBias");

    public PositiveFeedbackLink createLink(IA input, BindingActivation output, boolean isSelfRef) {
        return new PositiveFeedbackLink(this, input, output, isSelfRef);
    }

    protected void initFromTemplate(PositiveFeedbackSynapse s) {
        s.feedbackWeight.set(feedbackWeight.getCurrentValue());
        s.feedbackBias.set(feedbackBias.getCurrentValue());
        super.initFromTemplate(s);
    }

    public Field getFeedbackWeight() {
        return feedbackWeight;
    }

    public Field getFeedbackBias() {
        return feedbackBias;
    }

    @Override
    public boolean isRecurrent() {
        return true;
    }

    @Override
    public List<Transition> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public boolean linkingCheck(BindingSignal<IA> iBS, BindingSignal<BindingActivation> oBS) {
        // Skip BindingNeuronSynapse.checkLinkingPreConditions
        // --> Do not check Link.isForward(iAct, oAct) and
        // --> iAct.isFired() since the positive feedback synapse is initially assumed to be active.
        return commonLinkingCheck(iBS, oBS);
    }
}
