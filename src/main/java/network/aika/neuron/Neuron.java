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

import network.aika.*;
import network.aika.neuron.activation.*;
import network.aika.neuron.phase.Phase;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.Sign.NEG;
import static network.aika.neuron.Sign.POS;
import static network.aika.neuron.activation.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Neuron<S extends Synapse> implements Writable {

    public static int debugId = 0;
    public static boolean debugOutput = false;

    private static final Logger log = LoggerFactory.getLogger(Neuron.class);

    volatile long retrievalCount = 0;

    private volatile boolean modified;

    private NeuronProvider provider;

    private String descriptionLabel;

    private volatile double bias;

    protected TreeMap<NeuronProvider, Synapse> outputSynapses = new TreeMap<>();

    protected final ReadWriteLock lock = new ReadWriteLock();

    protected double frequency;
    protected Instances instances;

    protected boolean isInputNeuron; // Input Neurons won't be trained!

    protected Neuron() {
    }

    public Neuron(NeuronProvider p) {
        provider = p;
    }

    public Neuron(Model m, Boolean isInputNeuron) {
        this.isInputNeuron = isInputNeuron;
        provider = new NeuronProvider(m, this);
        modified = true;
    }

    public abstract ActivationFunction getActivationFunction();

    public abstract Fired incrementFired(Fired f);

    public abstract Synapse getInputSynapse(NeuronProvider n);

    public abstract Visitor transition(Visitor v);

    public Synapse getOutputSynapse(NeuronProvider n) {
        lock.acquireReadLock();
        Synapse s = outputSynapses.get(n);
        lock.releaseReadLock();
        return s;
    }

    public void initInstance(Reference ref) {
        this.instances = new Instances(getModel(), ref);
    }

    public Instances getInstances() {
        return instances;
    }

    public NeuronProvider getProvider() {
        return provider;
    }

    public Stream<? extends Synapse> getInputSynapses() {
        throw new UnsupportedOperationException();
    }

    public Stream<Synapse> getOutputSynapses() {
        return outputSynapses.values().stream();
    }

    public boolean isInputNeuron() {
        return isInputNeuron;
    }

    public abstract boolean isInitialized();

    public abstract boolean containsInputSynapse(Synapse s);

    public abstract void addInputSynapse(S s);

    public abstract boolean containsOutputSynapse(Synapse synapse);

    public abstract void addOutputSynapse(Synapse synapse);

    public abstract void removeInputSynapse(S s);

    public abstract void removeOutputSynapse(Synapse s);

    public abstract byte getType();

    public abstract void updateReference(Link nl);

    public Long getId() {
        return provider.getId();
    }

    public String getDescriptionLabel() {
        return descriptionLabel;
    }

    public void setDescriptionLabel(String label) {
        this.descriptionLabel = label;
    }

    public Model getModel() {
        return provider.getModel();
    }

    public long getRetrievalCount() {
        return retrievalCount;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void addConjunctiveBias(double b, boolean recurrent) {
    }

    public void setBias(double b) {
        bias += b;
        modified = true;
    }

    public void addBias(double biasDelta) {
        bias += biasDelta;
        modified = true;
    }

    public double getBias(Phase p) {
        return bias;
    }

    public double getRawBias() {
        return bias;
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public void count(Activation act) {
        if(act.isActive()) {
            instances.update(getModel(), act.getReference());
            frequency += 1.0;
            modified = true;
        }
    }

    public void applyMovingAverage(Config trainingConfig) {
        Double alpha = trainingConfig.getAlpha();
        if(alpha != null) {
            frequency *= alpha;
            modified = true;
        }
    }

    public abstract void induceNeuron(Activation act);

    public Activation initInducedNeuron(Activation iAct) {
//        System.out.println("N  " + "dbg:" + (Neuron.debugId++) + " " + act.getNeuron().getDescriptionLabel() + "  " + Utils.round(s) + "  --> " + n.getDescriptionLabel() + "               INDUCED!");
        setDescriptionLabel(iAct.getConfig().getLabel(iAct, this));

        Activation act = iAct.createActivation(this);

        act.initSelfGradient();

        induceSynapse(iAct, act, new Visitor(iAct, INPUT, false));

        getInstances().update(getModel(), iAct.getReference());

        act.process();

        act.propagate();

        return act;
    }

    public abstract Link induceSynapse(Activation iAct, Activation oAct, Visitor c);

    public static boolean ADJUST_GRADIENT = false;

    public void train(Activation act) {
        act.propagate();
        act.getThought().processLinks();

        if(isInputNeuron) {
            return;
        }

        if(!ADJUST_GRADIENT) return;

        act.initSelfGradient();
        act.computeInitialLinkGradients();
        act.updateSelfGradient();
    }

    public double getSurprisal(Sign s) {
        double p = getP(s, instances.getN());
        return -Math.log(p);
    }

    public double getP(Sign s, double n) {
        BetaDistribution dist = new BetaDistribution(
                getFrequency(s, n) + 1,
                getFrequency(s.invert(), n) + 1
        );

        double p = dist.inverseCumulativeProbability(
                getModel().getBetaThreshold()
        );

        return p;
    }

    public double getFrequency() {
        return frequency;
    }

    public double getFrequency(Sign s, double n) {
        return (s == POS ? frequency : n - frequency);
    }

    public void setFrequency(double f) {
        frequency = f;
        modified = true;
    }

    public void reactivate() {
    }

    public void suspend() {
    }

    public void updatePropagateFlag() {
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getType());

        out.writeBoolean(descriptionLabel != null);
        if(descriptionLabel != null) {
            out.writeUTF(descriptionLabel);
        }

        out.writeDouble(bias);

        for (Synapse s : outputSynapses.values()) {
            if (s.getOutput() != null) {
                out.writeBoolean(true);
                getModel().writeSynapse(s, out);
            }
        }
        out.writeBoolean(false);

        out.writeDouble(frequency);
        instances.write(out);

        out.writeBoolean(isInputNeuron);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        if(in.readBoolean()) {
            descriptionLabel = in.readUTF();
        }

        bias = in.readDouble();

        while (in.readBoolean()) {
            Synapse syn = m.readSynapse(in);
            outputSynapses.put(syn.getPOutput(), syn);
        }

        frequency = in.readDouble();
        instances = Instances.read(in, m);

        isInputNeuron = in.readBoolean();
    }

    public String toString() {
        return getId() + ":" + getDescriptionLabel();
    }

    public String toDetailedString() {
        return "n " + getClass().getSimpleName() + " " + toString() + " b:" + Utils.round(bias);
    }

    public String statToString() {
        return getClass().getSimpleName() + " " +
                getId() + ":" + getDescriptionLabel() + " " +
                "f:" + Utils.round(frequency) + " " +
                "N:" + Utils.round(instances.getN()) + " " +
                "p:" + Utils.round(getP(POS, instances.getN())) + " " +
                "s(p):" + Utils.round(getSurprisal(POS)) + " " +
                "s(n):" + Utils.round(getSurprisal(NEG)) + " " +
                "\n";
    }
}
