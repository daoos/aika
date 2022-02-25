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

import network.aika.Config;
import network.aika.Model;
import network.aika.Thought;
import network.aika.fields.*;
import network.aika.neuron.*;
import network.aika.direction.Direction;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.BranchBindingSignal;
import network.aika.neuron.bindingsignal.PatternBindingSignal;
import network.aika.sign.Sign;
import network.aika.steps.activation.*;
import network.aika.steps.link.InformationGainGradient;
import network.aika.steps.link.LinkCounting;
import network.aika.utils.Utils;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.FieldUtils.func;
import static network.aika.fields.FieldUtils.mul;
import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.steps.LinkingOrder.POST_FIRED;
import static network.aika.steps.LinkingOrder.PRE_FIRED;

/**
 * @author Lukas Molzberger
 */
public abstract class Activation<N extends Neuron> extends Element<Activation> {

    public static final Comparator<Activation> NEURON_COMPARATOR = Comparator.
            <Activation>comparingLong(act -> act.getNeuron().getId())
            .thenComparingInt(Activation::getId);

    public static final Comparator<Activation> ID_COMPARATOR = Comparator.comparingInt(Activation::getId);

    protected final int id;
    protected N neuron;
    protected Thought thought;

    protected Timestamp creationTimestamp = NOT_SET;
    protected Timestamp fired = NOT_SET;

    protected boolean isInput;
    private boolean finalMode = false;

    protected Field value = new Field("value");
    protected Field net = new QueueField(this, "net");

    protected Map<NeuronProvider, Link> inputLinks;
    protected NavigableMap<OutputKey, Link> outputLinks;

    protected SortedMap<Activation<?>, PatternBindingSignal> patternBindingSignals = new TreeMap<>(
            Comparator.comparing(Activation::getId)
    );
    protected SortedMap<Activation<?>, BranchBindingSignal> branchBindingSignals = new TreeMap<>(
            Comparator.comparing(Activation::getId)
    );

    private Field entropy = new Field("Entropy");
    protected Field inputGradient = new QueueField(this, "Input-Gradient");
    protected Field outputGradient = new QueueField(this, "Output-Gradient");

    protected Activation(int id, N n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(int id, Thought t, N n) {
        this(id, n);
        this.thought = t;

        entropy.addFieldListener("receiveOwnGradientUpdate", (l, u) ->
                receiveOwnGradientUpdate(u)
        );

        net.setPropagatePreCondition((cv, nv, u) ->
                !Utils.belowTolerance(u) && (cv >= 0.0 || nv >= 0.0)
        );
        net.add(getNeuron().getBias().getCurrentValue());

        net.addFieldListener("checkIfFired", (l, u) -> {
            if (net.getNewValue() > 0.0)
                setFired();
        });

        initFields();

        value.addFieldListener("l.propagateValue", (label, u) ->
                getOutputLinks()
                        .forEach(l -> l.propagateValue())
        );

        mul(
                "ig * f'(net)",
                inputGradient,
                func("f'(net)", net, x ->
                        getNeuron().getActivationFunction().outerGrad(x)
                ),
                outputGradient
        );

        outputGradient.addFieldListener("update-bias", (l, g) ->
                getNeuron().getBias().addAndTriggerUpdate(getConfig().getLearnRate() * g)
        );

        thought.register(this);
        neuron.register(this);

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(OutputKey.COMPARATOR);
    }

    protected void initFields() {
        if (!isInput)
            func(
                    "f(net)",
                    net,
                    x -> getActivationFunction().f(x),
                    value
            );

        outputGradient.addFieldListener("updateWeights", (l, u) ->
                updateWeights(u)
        );
        outputGradient.addFieldListener("propagateGradient", (l, u) ->
                propagateGradient()
        );
    }


    public abstract boolean isBoundToConflictingBS(BindingSignal bs);

    public boolean checkPropagatePatternBindingSignal(PatternBindingSignal bs) {
        return true;
    }

    public boolean checkPropagateBranchBindingSignal(BranchBindingSignal bs) {
        return true;
    }

    protected void propagateGradient() {
        inputLinks.values().stream()
                .filter(l -> l.getSynapse().isAllowTraining())
                .forEach(l -> l.backPropagate());
    }

    protected void updateWeights(double g) {
        inputLinks.values().stream()
                .filter(l -> l.getSynapse().isAllowTraining())
                .forEach(l -> l.updateWeight(g));
    }

    public void init(Synapse originSynapse, Activation originAct) {
        setCreationTimestamp();
        thought.onActivationCreationEvent(this, originSynapse, originAct);
    }

    public Field getEntropy() {
        return entropy;
    }

    public Field getInputGradient() {
        return inputGradient;
    }

    public void updateBias(double u) {
        getNet().addAndTriggerUpdate(u);
    }

    public int getId() {
        return id;
    }

    public Field getValue() {
        return value;
    }

    public boolean isInput() {
        return isInput;
    }

    public void setInput(boolean input) {
        isInput = input;
    }

    public Field getNet() {
        return net;
    }

    public Timestamp getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp() {
        this.creationTimestamp = thought.getCurrentTimestamp();
    }

    public boolean isFinal() {
        return finalMode;
    }

    public void setFinal() {
        if(finalMode)
            return;
        finalMode = true;

        onFinal();
    }

    public Timestamp getFired() {
        return fired;
    }

    public boolean isFired() {
        return fired != NOT_SET;
    }

    public void setFired() {
        if(isFired())
            return;

        fired = thought.getCurrentTimestamp();

        onFired();
    }

    protected void onFired() {
        if(isTemplate())
            Induction.add(this);

        Propagate.add(this, false, "", s -> true);

        if(isFinal())
            onFinalFired();

        getBindingSignals()
                .forEach(bs ->
                        onBindingSignalArrivedFired(bs)
                );
    }

    protected void onFinal() {
        if(isFired())
            onFinalFired();

        getBindingSignals()
                .forEach(bs ->
                        onBindingSignalArrivedFinal(bs)
                );
    }

    protected void onFinalFired() {
        Propagate.add(this, true, "", s -> true);

        InactiveLinks.add(this);
        addEntropySteps();
        addCountingSteps();

        getBindingSignals()
                .forEach(bs ->
                        onBindingSignalArrivedFinalFired(bs)
                );
    }

    protected void onBindingSignalArrived(BindingSignal bs) {
        if(!getNeuron().isNetworkInput()) {
            Linking.add(this, bs, INPUT, PRE_FIRED, false, "", s -> true);
        }

        if(isFired())
            onBindingSignalArrivedFired(bs);

        if(isFinal())
            onBindingSignalArrivedFinal(bs);
    }

    protected void onBindingSignalArrivedFinal(BindingSignal bs) {
        if(!getNeuron().isNetworkInput()) {
            Linking.add(this, bs, INPUT, PRE_FIRED, true, "", s -> true);
        }
    }

    protected void onBindingSignalArrivedFired(BindingSignal bs) {
        Linking.add(this, bs, OUTPUT, POST_FIRED, false, "", s -> true);

        if(isFinal())
            onBindingSignalArrivedFinalFired(bs);
    }

    protected void onBindingSignalArrivedFinalFired(BindingSignal bs) {
        Linking.add(this, bs, OUTPUT, POST_FIRED, true, "", s -> true);
    }

    private void addEntropySteps() {
        EntropyGradient.add(this);

        inputLinks.values()
                .forEach(l ->
                        InformationGainGradient.add(l)
                );
    }

    private void addCountingSteps() {
        Counting.add(this);
        getInputLinks().forEach(l ->
                LinkCounting.add(l)
        );
    }

    public Thought getThought() {
        return thought;
    }

    public abstract boolean isSelfRef(Activation iAct);

    public boolean isNetworkInput() {
        return getNeuron().isNetworkInput();
    }

    public boolean isTemplate() {
        return getNeuron().isTemplate();
    }

    public boolean checkAllowPropagate() {
        return isFired();
    }

    public abstract Range getRange();

    public Range getAbsoluteRange() {
        Range r = getRange();
        if(r == null) return null;
        return r.getAbsoluteRange(thought.getRange());
    }

    public Stream<? extends BindingSignal> getBindingSignals() {
        return Stream.concat(
                getPatternBindingSignals().values().stream(),
                getBranchBindingSignals().values().stream()
        );
    }

    public BindingSignal addBindingSignal(BindingSignal bindingSignal) {
        if (bindingSignal.exists())
            return null;

        bindingSignal.link();
        return bindingSignal;
    }

    public void registerPatternBindingSignal(PatternBindingSignal bs) {
        onBindingSignalArrived(bs);

        patternBindingSignals.put(bs.getOriginActivation(), bs);
    }

    public void registerBranchBindingSignal(BranchBindingSignal bs) {
        onBindingSignalArrived(bs);

        branchBindingSignals.put(bs.getOriginActivation(), bs);
    }

    public Map<Activation<?>, PatternBindingSignal> getPatternBindingSignals() {
        return patternBindingSignals;
    }

    public Map<Activation<?>, BranchBindingSignal> getBranchBindingSignals() {
        return branchBindingSignals;
    }

    public abstract Stream<? extends BindingSignal<?>> getReverseBindingSignals(Neuron toNeuron);

    @Override
    public int compareTo(Activation act) {
        return ID_COMPARATOR.compare(this, act);
    }

    public OutputKey getOutputKey() {
        return new OutputKey(getNeuronProvider(), getId());
    }

    public String getLabel() {
        return getNeuron().getLabel();
    }

    public N getNeuron() {
        return neuron;
    }

    public void setNeuron(N n) {
        this.neuron = n;
    }

    public ActivationFunction getActivationFunction() {
        return neuron.getActivationFunction();
    }

    public Model getModel() {
        return neuron.getModel();
    }

    public Config getConfig() {
        return getThought().getConfig();
    }

    public NeuronProvider getNeuronProvider() {
        return neuron.getProvider();
    }

    public void setInputValue(double v) {
        value.setAndTriggerUpdate(v);
        isInput = true;
    }

    public Link getInputLink(Neuron n) {
        return inputLinks.get(n.getProvider());
    }

    public Link getInputLink(Synapse s) {
        return inputLinks.get(s.getPInput());
    }

    public boolean inputLinkExists(Synapse s) {
        return inputLinks.containsKey(s.getPInput());
    }

    public boolean linkExists(Direction dir, Synapse ts, boolean template) {
        return template ?
                dir.getLinks(this)
                        .map(Link::getSynapse)
                        .anyMatch(s -> s.isOfTemplate(ts)) :
                !getOutputLinks(ts).isEmpty();
    }

    public SortedMap<OutputKey, Link> getOutputLinks(Synapse s) {
        return outputLinks
                .subMap(
                        new OutputKey(s.getOutput().getProvider(), Integer.MIN_VALUE),
                        true,
                        new OutputKey(s.getOutput().getProvider(), MAX_VALUE),
                        true
                );
    }

    public FieldOutput getOutputGradient() {
        return outputGradient;
    }

    public void updateEntropyGradient() {
        Range range = getAbsoluteRange();
        assert range != null;

        entropy.setAndTriggerUpdate(
                getNeuron().getSurprisal(
                        this,
                        Sign.getSign(this),
                        range
                )
        );
    }

    public void receiveOwnGradientUpdate(double u) {
        inputGradient.addAndTriggerUpdate(u);
    }

    public void linkInputs() {
        inputLinks
                .values()
                .forEach(Link::linkInput);
    }

    public void unlinkInputs() {
        inputLinks
                .values()
                .forEach(Link::unlinkInput);
    }

    public void linkOutputs() {
        outputLinks
                .values()
                .forEach(Link::linkOutput);
    }

    public void unlinkOutputs() {
        outputLinks
                .values()
                .forEach(Link::unlinkOutput);
    }

    public void link() {
        linkInputs();
        linkOutputs();
    }

    public void unlink() {
        unlinkInputs();
        unlinkOutputs();
    }

    public Stream<Link> getInputLinks() {
        return inputLinks.values().stream();
    }

    public Stream<Link> getOutputLinks() {
        return outputLinks.values().stream();
    }

    public String toString() {
        return (isTemplate() ? "Template-" : "") + getClass().getSimpleName() + " " + toKeyString();
    }

    public String toKeyString() {
        return "id:" + getId() + " n:[" + getNeuron().toKeyString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Activation)) return false;
        Activation<?> that = (Activation<?>) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
