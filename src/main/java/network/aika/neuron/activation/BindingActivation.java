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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.fields.Field;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.steps.activation.BranchProbability;
import network.aika.neuron.steps.activation.SetFinalMode;
import network.aika.neuron.steps.activation.UpdateBias;
import network.aika.neuron.steps.link.PropagateGradientAndUpdateWeight;
import network.aika.utils.Utils;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Timestamp.NOT_SET;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends Activation<BindingNeuron> {

    public static BindingActivation MIN_BINDING_ACT = new BindingActivation(0, null);
    public static BindingActivation MAX_BINDING_ACT = new BindingActivation(Integer.MAX_VALUE, null);


    private boolean finalMode = false;
    private Timestamp finalTimestamp = NOT_SET;

    private final Set<BindingActivation> branches = new TreeSet<>();
    private BindingActivation mainBranch;

    private double branchProbability = 1.0;

    private Field ownInputGradient = new Field();


    protected BindingActivation(int id, BindingNeuron n) {
        super(id, n);
    }

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);
    }

    @Override
    public void init(Synapse originSynapse, Activation originAct) {
        super.init(originSynapse, originAct);
        addSelfBindingSignal((byte) 0);
    }

    @Override
    public byte getType() {
        return 1;
    }

    public BindingActivation createBranch(Synapse excludedSyn) {
        BindingActivation clonedAct = getNeuron().createActivation(getThought());

        copySteps(clonedAct);
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct, excludedSyn);

        clonedAct.init(null, this);
        return clonedAct;
    }

    public void addFeedbackSteps() {
        SetFinalMode.add(this);
        BranchProbability.add(this);
    }

    @Override
    public Range getRange() {
        BindingSignal bs = getPrimaryPatternBindingSignal();
        if(bs == null)
            return null;

        return bs.getBindingSignalAct()
                .getRange();
    }

    private BindingSignal getPrimaryPatternBindingSignal() {
        return getPatternBindingSignals().values().stream()
                .min(Comparator.comparing(bs -> bs.getBindingSignalAct().getFired()))
                .orElse(null);
    }

    public boolean isFinalMode() {
        return finalMode;
    }

    public void setFinalMode(boolean finalMode) {
        this.finalMode = finalMode;
    }

    public Timestamp getFinalTimestamp() {
        return finalTimestamp;
    }

    public void setFinalTimestamp() {
        this.finalTimestamp = getThought().getCurrentTimestamp();
    }

    public BindingActivation getMainBranch() {
        return mainBranch;
    }

    public boolean hasBranches() {
        return !branches.isEmpty();
    }

    public Set<BindingActivation> getBranches() {
        return branches;
    }

    public Stream<BindingActivation> getAllBranches() {
        if (mainBranch != null)
            return Stream.concat(Stream.of(mainBranch), branches.stream());
        else
            return branches.stream();
    }

    public double getBranchProbability() {
        return branchProbability;
    }

    public void setBranchProbability(double p) {
        branchProbability = p;
    }


    public void receiveOwnGradientUpdate(double u) {
        super.receiveOwnGradientUpdate(u);
        ownInputGradient.add(u);
    }

    // should ownGrad only used for backpropagation or also for the weight update?
    public void updateOutputGradient() {
        double grad = outputGradient.getUpdate();
        double ownGrad = ownInputGradient.getUpdate();
        UpdateBias.add(this, getConfig().getLearnRate() * grad);

        inputLinks.values().forEach(l ->
                PropagateGradientAndUpdateWeight.add(l, l.getSynapse().isRecurrent() ? ownGrad : grad)
        );
    }

    @Override
    protected double computeValue() {
        return branchProbability * getActivationFunction().f(net);
    }

    public String toString(boolean includeLink) {
        return super.toString(includeLink) + " bp:" + Utils.round(branchProbability);
    }
}
