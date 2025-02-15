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
package network.aika.text;

import network.aika.fields.Field;
import network.aika.fields.QueueField;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.steps.activation.Propagate;

/**
 *
 * @author Lukas Molzberger
 */
public class TokenActivation extends PatternActivation {

    private Range range;
    private TokenActivation previousToken;
    private TokenActivation nextToken;

    private CategoryActivation categoryActivation;
    private BindingActivation relPTBindingActivation;
    private BindingActivation relNTBindingActivation;


    public TokenActivation(int id, int begin, int end, Document doc, PatternNeuron patternNeuron) {
        super(id, doc, patternNeuron);
        range = new Range(begin, end);
    }

    @Override
    public void init(Synapse originSynapse, Activation originAct) {
        super.init(originSynapse, originAct);

        TextModel m = getModel();
        categoryActivation = (CategoryActivation) Propagate.propagate(
                this,
                getNeuron().getOutputSynapse(m.getTokenCategory().getProvider())
        );

        relPTBindingActivation = (BindingActivation) Propagate.propagate(
                categoryActivation,
                m.getRelPTFeedbackSyn()
        );

        relNTBindingActivation = (BindingActivation) Propagate.propagate(
                categoryActivation,
                m.getRelNTFeedbackSyn()
        );
    }

    protected Field initNet() {
        return new QueueField(this, "net", 10.0);
    }

    public boolean isInput() {
        return true;
    }

    public static void addRelation(TokenActivation prev, TokenActivation next) {
        if(prev == null || next == null)
            return;

        prev.nextToken = next;
        next.previousToken = prev;

        TextModel model = prev.getModel();

        next.linkPrimaryInput(model.getRelNTPrimaryInputSyn(), prev.relNTBindingActivation);
        prev.linkPrimaryInput(model.getRelPTPrimaryInputSyn(), next.relPTBindingActivation);
    }

    private void linkPrimaryInput(PrimaryInputSynapse<CategoryNeuron, CategoryActivation> model, BindingActivation toAct) {
        PrimaryInputSynapse relSynNext = model;
        relSynNext.createLink(
                categoryActivation,
                toAct,
                false
        );
    }

    public TokenActivation getPreviousToken() {
        return previousToken;
    }

    public TokenActivation getNextToken() {
        return nextToken;
    }

    @Override
    public Range getRange() {
        return range;
    }
}
