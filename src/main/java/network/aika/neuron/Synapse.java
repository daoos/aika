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
import network.aika.Document;
import network.aika.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

import static network.aika.neuron.Synapse.State.CURRENT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<I extends INeuron, O extends INeuron> implements Writable {

    public static double TOLERANCE = 0.0000001;

    protected Neuron input;
    protected Neuron output;

    private boolean recurrent;
    private boolean propagate;

    private double weight;
    private double weightDelta;

    public Synapse() {
    }


    public Synapse(Neuron input, Neuron output, boolean recurrent, boolean propagate) {
        this.input = input;
        this.output = output;
        this.recurrent = recurrent;
        setPropagate(propagate);
    }


    public abstract byte getType();

    public Neuron getPInput() {
        return input;
    }

    public Neuron getPOutput() {
        return output;
    }


    public I getInput() {
        return (I) input.get();
    }


    public O getOutput() {
        return (O) output.get();
    }

    public double getWeight() {
        return weight;
    }

    public double getNewWeight() {
        return weight + weightDelta;
    }

    public double getWeight(State s) {
        return s == CURRENT ? weight : getNewWeight();
    }

    public double getWeightDelta() {
        return weightDelta;
    }

    public void link() {
        INeuron in = input.get();
        INeuron out = output.get();

        boolean dir = in.getId() < out.getId();

        (dir ? in : out).lock.acquireWriteLock();
        (dir ? out : in).lock.acquireWriteLock();

        input.lock.acquireWriteLock();
        input.activeOutputSynapses.put(output, this);
        input.lock.releaseWriteLock();

        output.lock.acquireWriteLock();
        output.activeInputSynapses.put(input, this);
        output.lock.releaseWriteLock();

        addLinkInternal(in, out);

        (dir ? in : out).lock.releaseWriteLock();
        (dir ? out : in).lock.releaseWriteLock();
    }


    public void unlink() {
        INeuron in = input.get();
        INeuron out = output.get();

        boolean dir = input.getId() < out.getId();

        (dir ? in : out).lock.acquireWriteLock();
        (dir ? out : in).lock.acquireWriteLock();

        input.lock.acquireWriteLock();
        input.activeOutputSynapses.remove(this);
        input.lock.releaseWriteLock();

        output.lock.acquireWriteLock();
        output.activeInputSynapses.remove(this);
        output.lock.releaseWriteLock();

        removeLinkInternal(in, out);

        (dir ? in : out).lock.releaseWriteLock();
        (dir ? out : in).lock.releaseWriteLock();
    }


    protected abstract void addLinkInternal(INeuron in, INeuron out);


    protected abstract void removeLinkInternal(INeuron in, INeuron out);


    public void commit() {
        weight += weightDelta;
        weightDelta = 0.0;
    }


    public boolean isZero() {
        return Math.abs(weight) < TOLERANCE;
    }


    public enum State {
        NEXT,
        CURRENT
    }


    public boolean isWeak(State state) {
        return output.get().isWeak(this, state);
    }


    public void updateDelta(Document doc, double weightDelta) {
        this.weightDelta += weightDelta;

        if(doc != null) {
            doc.notifyWeightModified(this);
        }
    }


    public void update(Document doc, double weight) {
        this.weightDelta = weight - this.weight;

        if(doc != null) {
            doc.notifyWeightModified(this);
        }
    }


    public boolean isNegative(State s) {
        return getWeight(s) < 0.0;
    }


    public boolean isRecurrent() {
        return recurrent;
    }


    public boolean isPropagate() {
        return propagate;
    }


    public void setPropagate(boolean propagate) {
        this.propagate = propagate;

        if(propagate) {
            input.get().addPropagateTarget(output);
        } else {
            input.get().removePropagateTarget(output);
        }
    }


    public String toString() {
        return "S W:" + Utils.round(getNewWeight()) + " " + input + "->" + output;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getType());

        out.writeInt(input.getId());
        out.writeInt(output.getId());

        out.writeDouble(weight);

        out.writeBoolean(recurrent);
        out.writeBoolean(propagate);
    }



    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        input = m.lookupNeuron(in.readInt());
        output = m.lookupNeuron(in.readInt());

        weight = in.readDouble();

        recurrent = in.readBoolean();
        propagate = in.readBoolean();

        output.addActiveInputSynapse(this);
        input.addActiveOutputSynapse(this);
    }



    public static Synapse createOrReplace(Neuron inputNeuron, Neuron outputNeuron, SynapseFactory synapseFactory) {
        Synapse s = synapseFactory.createSynapse(inputNeuron, outputNeuron);
        s.link();

        return s;
    }



    /**
     * The {@code Builder} class is just a helper class which is used to initialize a neuron. Most of the parameters of this class
     * will be mapped to a input synapse for this neuron.
     *
     * @author Lukas Molzberger
     */
    public static abstract class Builder implements Neuron.Builder {

        private Neuron neuron;

        protected boolean recurrent;
        protected boolean propagate;
        double weight;


        /**
         * Determines the input neuron.
         *
         * @param neuron
         * @return
         */
        public Builder setNeuron(Neuron neuron) {
            assert neuron != null;
            this.neuron = neuron;
            return this;
        }

        /**
         * Determines the input neuron.
         *
         * @param neuron
         * @return
         */
        public Builder setNeuron(INeuron<?> neuron) {
            assert neuron != null;
            this.neuron = neuron.getProvider();
            return this;
        }

        public Builder setRecurrent(boolean recurrent) {
            this.recurrent = recurrent;
            return this;
        }

        /**
         * The synapse weight of this input.
         *
         * @param weight
         * @return
         */
        public Builder setWeight(double weight) {
            this.weight = weight;
            return this;
        }

        public Synapse getSynapse(Neuron outputNeuron) {
            return createOrReplace(neuron, outputNeuron, getSynapseFactory());
        }

        protected abstract SynapseFactory getSynapseFactory();
    }

    public interface SynapseFactory {
        Synapse createSynapse(Neuron input, Neuron output);
    }
}
