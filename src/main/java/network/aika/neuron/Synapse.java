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
package network.aika.neuron;

import network.aika.Model;
import network.aika.direction.Direction;
import network.aika.fields.ThresholdOperator;
import network.aika.neuron.activation.*;
import network.aika.fields.Field;
import network.aika.neuron.axons.Axon;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.sign.Sign;
import network.aika.steps.activation.PostTraining;
import network.aika.utils.Bound;
import network.aika.utils.Utils;
import network.aika.utils.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import static network.aika.sign.Sign.NEG;
import static network.aika.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<S extends Synapse, I extends Neuron & Axon, O extends Neuron<?, OA>, L extends Link<S, IA, OA>, IA extends Activation, OA extends Activation> implements Writable {

    private static final Logger log = LoggerFactory.getLogger(Synapse.class);

    protected NeuronProvider input;
    protected NeuronProvider output;

    private boolean isInputLinked;
    private boolean isOutputLinked;

    protected S template;
    private TemplateSynapse templateInfo;

    protected Field weight = new Field(this, "weight", () -> {
        PostTraining.add(getOutput());
        setModified();
    });

    protected SampleSpace sampleSpace = new SampleSpace();

    protected double frequencyIPosOPos;
    protected double frequencyIPosONeg;
    protected double frequencyINegOPos;

    protected boolean allowTraining = true;

    public Integer getLooseLinkingRange() {
        return null;
    }

    public boolean allowLooseLinking() {
        return false;
    }

    public boolean isRecurrent() {
        return false;
    }

    public boolean linkingCheck(BindingSignal<IA> iBS, BindingSignal<OA> oBS) {
        if(!iBS.getActivation().isFired())
            return false;

        return commonLinkingCheck(iBS, oBS);
    }

    protected boolean commonLinkingCheck(BindingSignal<IA> iBS, BindingSignal<OA> oBS) {
        Transition t = getTransition(iBS, Direction.OUTPUT, false);
        if(t == null)
            return false;

        if(!t.next(iBS).match(oBS))
            return false;

        if(linkExists(iBS.getActivation(), oBS.getActivation()))
            return false;

        if(isTemplate() && !checkTemplateLinkingPreConditions(iBS, oBS))
            return false;

        return true;
    }

    public boolean linkExists(IA iAct, OA oAct) {
        Link existingLink = oAct.getInputLink(iAct.getNeuron());
        return existingLink != null && existingLink.getInput() == iAct;
    }

    public boolean checkTemplateLinkingPreConditions(BindingSignal<IA> iBS, BindingSignal<OA> oBS) {
        if(oBS.getActivation().getNeuron().isNetworkInput())
            return false;

        if(Link.templateLinkExists(this, iBS.getActivation(), oBS.getActivation()))
            return false;

        if(!ThresholdOperator.isTrue(oBS.getActivation().getInductionThreshold()))
            return false;

        return true;
    }

    public Transition getTransition(BindingSignal from, Direction dir, boolean propagate) {
        return getTransitions().stream()
                .filter(t -> t.check(from.getState(), dir, propagate))
                .findFirst()
                .orElse(null);
    }

    public abstract List<Transition> getTransitions();

    public abstract void setModified();

    public boolean allowPropagate(Activation act) {
        return true;
    }

    public void setInput(I input) {
        this.input = input.getProvider();
    }

    public void setOutput(O output) {
        this.output = output.getProvider();
    }

    public S instantiateTemplate(I input, O output) {
        S s = instantiateTemplate();

        s.input = input.getProvider();
        s.output = output.getProvider();

        initFromTemplate(s);
        return s;
    }

    private S instantiateTemplate() {
        S s;
        try {
            s = (S) getClass().getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    public abstract L createLink(IA input, OA output, boolean isSelfRef);

    protected void initFromTemplate(S s) {
        s.weight.set(weight.getCurrentValue());
        s.template = this;
    }

    public void setWeight(double w) {
        weight.set(w);
    }

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public boolean isTemplate() {
        return template == null;
    }

    public Synapse getTemplate() {
        if(isTemplate())
            return this;
        return template;
    }

    public TemplateSynapse getTemplateInfo() {
        assert isTemplate();
        if (templateInfo == null) {
            templateInfo = new TemplateSynapse();
        }

        return templateInfo;
    }

    public boolean isOfTemplate(Synapse templateSynapse) {
        return getTemplateSynapseId() == templateSynapse.getTemplateSynapseId();
    }

    public byte getTemplateSynapseId() {
        return getTemplate().getTemplateInfo().getTemplateSynapseId();
    }

    public boolean isInputLinked() {
        return isInputLinked;
    }

    public void linkInput() {
        Neuron in = getInput();
        in.getLock().acquireWriteLock();
        isInputLinked = true;
        in.addOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }

    public void unlinkInput() {
        Neuron in = getInput();
        in.getLock().acquireWriteLock();
        isInputLinked = false;
        in.removeOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }

    public boolean isOutputLinked() {
        return isOutputLinked;
    }

    public void linkOutput() {
        Neuron out = output.getNeuron();

        out.getLock().acquireWriteLock();
        isOutputLinked = true;
        out.addInputSynapse(this);
        out.getLock().releaseWriteLock();
    }

    public void unlinkOutput() {
        Neuron out = output.getNeuron();

        out.getLock().acquireWriteLock();
        isOutputLinked = false;
        out.removeInputSynapse(this);
        out.getLock().releaseWriteLock();
    }

    public NeuronProvider getPInput() {
        return input;
    }

    public NeuronProvider getPOutput() {
        return output;
    }

    public I getInput() {
        return (I) input.getNeuron();
    }

    public O getOutput() {
        return (O) output.getNeuron();
    }

    public SampleSpace getSampleSpace() {
        return sampleSpace;
    }

    public double getFrequency(Sign is, Sign os, double n) {
        if(is == POS && os == POS) {
            return frequencyIPosOPos;
        } else if(is == POS && os == NEG) {
            return frequencyIPosONeg;
        } else if(is == NEG && os == POS) {
            return frequencyINegOPos;
        }

        //TODO:
        return Math.max(n - (frequencyIPosOPos + frequencyIPosONeg + frequencyINegOPos), 0);
    }

    public void setFrequency(Sign is, Sign os, double f) {
        if(is == POS && os == POS) {
            frequencyIPosOPos = f;
        } else if(is == POS && os == NEG) {
            frequencyIPosONeg = f;
        } else if(is == NEG && os == POS) {
            frequencyINegOPos = f;
        } else {
            throw new UnsupportedOperationException();
        }
        setModified();
    }

    public void applyMovingAverage(double alpha) {
        sampleSpace.applyMovingAverage(alpha);
        frequencyIPosOPos *= alpha;
        frequencyIPosONeg *= alpha;
        frequencyINegOPos *= alpha;
        setModified();
    }

    public void count(Link l) {
        double oldN = sampleSpace.getN();

        boolean iActive = l.getInput().isFired();
        boolean oActive = l.getOutput().isFired();

        Range absoluteRange = l.getInput().getAbsoluteRange();

        sampleSpace.countSkippedInstances(absoluteRange);

        sampleSpace.count();

        if(oActive) {
            Double alpha = l.getConfig().getAlpha();
            if (alpha != null)
                applyMovingAverage(
                        Math.pow(alpha, sampleSpace.getN() - oldN)
                );
        }

        if(iActive && oActive) {
            frequencyIPosOPos += 1.0;
            setModified();
        } else if(iActive) {
            frequencyIPosONeg += 1.0;
            setModified();
        } else if(oActive) {
            frequencyINegOPos += 1.0;
            setModified();
        }

        sampleSpace.updateLastPosition(absoluteRange);
    }

    public Model getModel() {
        return getPOutput().getModel();
    }

    public double getSurprisal(Sign si, Sign so, Range range, boolean addCurrentInstance) {
        double n = sampleSpace.getN(range);
        double p = getProbability(si, so, n, addCurrentInstance);
        return Utils.surprisal(p);
    }

    public double getProbability(Sign si, Sign so, double n, boolean addCurrentInstance) {
        double f = getFrequency(si, so, n);

        // Add the current instance
        if(addCurrentInstance) {
            f += 1.0;
            n += 1.0;
        }

        return Bound.UPPER.probability(f, n);
    }

    public Field getWeight() {
        return weight;
    }

    public boolean isZero() {
        return Utils.belowTolerance(weight.getCurrentValue());
    }

    public boolean isNegative() {
        return weight.getCurrentValue() < 0.0;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getTemplate().getTemplateInfo().getTemplateSynapseId());

        out.writeLong(input.getId());
        out.writeLong(output.getId());

        out.writeBoolean(isInputLinked);
        out.writeBoolean(isOutputLinked);

        weight.write(out);

        out.writeDouble(frequencyIPosOPos);
        out.writeDouble(frequencyIPosONeg);
        out.writeDouble(frequencyINegOPos);

        sampleSpace.write(out);
    }

    public static Synapse read(DataInput in, Model m) throws IOException {
        byte templateSynapseId = in.readByte();
        Synapse templateSynapse = m.getTemplates().getTemplateSynapse(templateSynapseId);
        Synapse s = templateSynapse.instantiateTemplate();
        s.readFields(in, m);
        return s;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        input = m.lookupNeuron(in.readLong());
        output = m.lookupNeuron(in.readLong());

        isInputLinked = in.readBoolean();
        isOutputLinked = in.readBoolean();

        weight.readFields(in, m);

        frequencyIPosOPos = in.readDouble();
        frequencyIPosONeg = in.readDouble();
        frequencyINegOPos = in.readDouble();

        sampleSpace = SampleSpace.read(in, m);
    }

    public String toString() {
        return (isTemplate() ? "Template-" : "") +
                getClass().getSimpleName() +
                " in:[" + input.getNeuron().toKeyString()  + "](" + (isInputLinked ? "+" : "-") + ") " +
                (allowPropagate(null) ? "==>" : "-->") +
                " out:[" + output.getNeuron().toKeyString() + "](" + (isOutputLinked ? "+" : "-") + ")";
    }
}
