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

import network.aika.Model;
import network.aika.callbacks.SuspensionCallback;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.CategoryActivation;
import network.aika.neuron.conjunctive.*;
import network.aika.neuron.disjunctive.CategoryNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
* @author Lukas Molzberger
*/
public class TextModel extends Model {

    public static String REL_PREVIOUS_TOKEN_LABEL = " Rel Prev. Token";
    public static String REL_NEXT_TOKEN_LABEL = " Rel Next Token";
    public static String TOKEN_LABEL = "Token Category";


    private NeuronProvider tokenCategory;
    private NeuronProvider relPreviousToken;
    private NeuronProvider relNextToken;

    private PrimaryInputSynapse<CategoryNeuron, CategoryActivation> relPTPrimaryInputSyn;
    private PositiveFeedbackSynapse<CategoryNeuron, CategoryActivation> relPTFeedbackSyn;

    private PrimaryInputSynapse<CategoryNeuron, CategoryActivation> relNTPrimaryInputSyn;
    private PositiveFeedbackSynapse<CategoryNeuron, CategoryActivation> relNTFeedbackSyn;

    public TextModel() {
        super();
    }

    public TextModel(SuspensionCallback sc) {
        super(sc);
    }

    public void init() {
        if(tokenCategory == null)
            tokenCategory = initCategoryNeuron(TOKEN_LABEL);

        BindingNeuron relPT = getTemplates().INPUT_BINDING_TEMPLATE.instantiateTemplate(true);
        relPreviousToken = relPT.getProvider();

        BindingNeuron relNT = getTemplates().INPUT_BINDING_TEMPLATE.instantiateTemplate(true);
        relNextToken = relNT.getProvider();

        relPTFeedbackSyn = initFeedbackSamePatternSynapse(getTokenCategory(), relPT);
        relPTPrimaryInputSyn = initRelatedInputSynapse(getTokenCategory(), relPT);
        initRelationNeuron(REL_PREVIOUS_TOKEN_LABEL, relPT);

        relNTFeedbackSyn = initFeedbackSamePatternSynapse(getTokenCategory(), relNT);
        relNTPrimaryInputSyn = initRelatedInputSynapse(getTokenCategory(), relNT);
        initRelationNeuron(REL_NEXT_TOKEN_LABEL, relNT);

        relPT.getProvider().save();
        relNT.getProvider().save();
    }

    private NeuronProvider initCategoryNeuron(String label) {
        CategoryNeuron n = getTemplates().CATEGORY_TEMPLATE.instantiateTemplate(true);
        n.setNetworkInput(true);
        n.setLabel(label);
        NeuronProvider np = n.getProvider();
        np.save();
        return np;
    }

    public PatternNeuron lookupToken(String tokenLabel) {
        Neuron inProv = getNeuron(tokenLabel);
        if(inProv != null) {
            return (PatternNeuron) inProv;
        }

        PatternNeuron in = getTemplates().INPUT_PATTERN_TEMPLATE.instantiateTemplate(true);

        in.setTokenLabel(tokenLabel);
        in.setNetworkInput(true);
        in.setLabel(tokenLabel);
        in.setAllowTraining(false);
        putLabel(tokenLabel, in.getId());

        initCategorySynapse(in, getTokenCategory());

        in.getProvider().save();

        return in;
    }

    private void initRelationNeuron(String label, BindingNeuron inRel) {
        inRel.setNetworkInput(true);
        inRel.setLabel(label);
        inRel.getBias().receiveUpdate(0, 4.0);
        inRel.setAllowTraining(false);
        inRel.updateAllowPropagate();
    }

    private PrimaryInputSynapse initRelatedInputSynapse(CategoryNeuron relTokenCat, BindingNeuron relBN) {
        PrimaryInputSynapse s = (PrimaryInputSynapse) getTemplates().PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE
                .instantiateTemplate(relTokenCat, relBN);

        double w = 10.0;

        s.linkOutput();
        s.getWeight().set(w);
        s.setAllowTraining(false);

        relBN.getBias().receiveUpdate(0, -w);
        return s;
    }

    private PositiveFeedbackSynapse initFeedbackSamePatternSynapse(CategoryNeuron in, BindingNeuron inRel) {
        PositiveFeedbackSynapse s = (PositiveFeedbackSynapse) getTemplates().POSITIVE_FEEDBACK_SYNAPSE_FROM_CATEGORY_TEMPLATE
                .instantiateTemplate(in, inRel);

        double w = 11.0;

        s.linkInput();
        s.linkOutput();
        s.getWeight().set(w);
        s.setAllowTraining(false);
        inRel.getBias().receiveUpdate(0, -w);

        return s;
    }

    private void initCategorySynapse(PatternNeuron tokenNeuron, CategoryNeuron tokenCat) {
        Synapse s = getTemplates().CATEGORY_SYNAPSE_TEMPLATE
                .instantiateTemplate(tokenNeuron, tokenCat);

        s.linkInput();
        s.getWeight().set(2.0);
        s.setAllowTraining(false);
    }

    public CategoryNeuron getTokenCategory() {
        return (CategoryNeuron) tokenCategory.getNeuron();
    }

    public BindingNeuron getPreviousTokenRelationBindingNeuron() {
        return (BindingNeuron) relPreviousToken.getNeuron();
    }

    public BindingNeuron getNextTokenRelationBindingNeuron() {
        return (BindingNeuron) relNextToken.getNeuron();
    }

    public PrimaryInputSynapse<CategoryNeuron, CategoryActivation> getRelPTPrimaryInputSyn() {
        return relPTPrimaryInputSyn;
    }

    public PositiveFeedbackSynapse<CategoryNeuron, CategoryActivation> getRelPTFeedbackSyn() {
        return relPTFeedbackSyn;
    }

    public PrimaryInputSynapse<CategoryNeuron, CategoryActivation> getRelNTPrimaryInputSyn() {
        return relNTPrimaryInputSyn;
    }

    public PositiveFeedbackSynapse<CategoryNeuron, CategoryActivation> getRelNTFeedbackSyn() {
        return relNTFeedbackSyn;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeLong(tokenCategory.getId());
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        tokenCategory = lookupNeuron(in.readLong());
    }
}
