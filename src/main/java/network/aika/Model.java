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
package network.aika;


import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.Provider.SuspensionMode;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.pattern.NegativeRecurrentSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.pattern.PatternNeuron;
import network.aika.neuron.pattern.PatternPartNeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;



/**
 * The model consists of two layers. The first layer is the actual neural network consisting of neurons and synapses.
 * The second layer is a pattern lattice containing a boolean logic representation of all the neurons. Whenever the
 * synapse weights of a neuron are adjusted, then the underlying boolean logic representation of this neuron will be
 * updated too.
 * <p>
 * <p>The model supports parallel processing using a fixed number of threads.
 *
 * @author Lukas Molzberger
 */
public class Model {

    private static final Logger log = LoggerFactory.getLogger(Model.class);

    public static double ALPHA = 0.99;

    public int N = 0; // needs to be stored

    public static Map<Byte, Class> typeRegistry = new HashMap<>();

    static {
        register(PatternNeuron.class);
        register(PatternPartNeuron.class);
        register(ExcitatorySynapse.class);
        register(NegativeRecurrentSynapse.class);
        register(InhibitoryNeuron.class);
        register(InhibitorySynapse.class);
    }

    public SuspensionHook suspensionHook;

    public AtomicInteger docIdCounter = new AtomicInteger(0);
    public AtomicInteger currentId = new AtomicInteger(0);

    // Important: the id field needs to be referenced by the provider!
    public WeakHashMap<Integer, WeakReference<Provider<? extends AbstractNode>>> providers = new WeakHashMap<>();
    public Map<Integer, Provider<? extends AbstractNode>> activeProviders = new TreeMap<>();

    public static AtomicLong visitedCounter = new AtomicLong(1);

    public Model() {
        this(null);
    }

    public Model(SuspensionHook sh) {
        suspensionHook = sh;
    }

    public void applyMovingAverage() {
        N *= ALPHA;
    }

    private static void register(Class clazz) {
        byte type = (byte) typeRegistry.size();
        typeRegistry.put(type, clazz);
        try {
            clazz.getField("type").setByte(null, type);
        } catch (Exception e) {
            log.error("Initialization error: ", e);
        }
    }

    public SuspensionHook getSuspensionHook() {
        return suspensionHook;
    }

    public void setSuspensionHook(SuspensionHook suspensionHook) {
        this.suspensionHook = suspensionHook;
    }

    public INeuron readNeuron(DataInput in, Neuron p) throws Exception {
        Constructor c = typeRegistry.get(in.readByte()).getDeclaredConstructor(Neuron.class);
        INeuron n = (INeuron) c.newInstance(p);
        n.readFields(in, this);
        return n;
    }

    public Synapse readSynapse(DataInput in) throws Exception {
        Synapse s = (Synapse) typeRegistry.get(in.readByte()).getDeclaredConstructor().newInstance();
        s.readFields(in, this);
        return s;
    }

    public void writeSynapse(Synapse s, DataOutput out) throws IOException {
        s.write(out);
    }

    public int getNewDocumentId() {
        return docIdCounter.addAndGet(1);
    }

    public Collection<Neuron> getActiveNeurons() {
        List<Neuron> tmp = new ArrayList<>();
        for(Provider<?> p: activeProviders.values()) {
            if(p instanceof Neuron) {
                tmp.add((Neuron) p);
            }
        }

        return tmp;
    }

    public Neuron lookupNeuron(int id) {
        synchronized (providers) {
            WeakReference<Provider<? extends AbstractNode>> wr = providers.get(id);
            if(wr != null) {
                Neuron n = (Neuron) wr.get();
                if (n != null) {
                    return n;
                }
            }

            return new Neuron(this, id);
        }
    }

    public void register(Provider p) {
        synchronized (activeProviders) {
            activeProviders.put(p.id, p);
        }
    }

    public void unregister(Provider p) {
        synchronized (activeProviders) {
            activeProviders.remove(p.id);
        }
    }

    private boolean suspend(int docId, Provider<? extends AbstractNode> p, SuspensionMode sm) {
        AbstractNode an = p.getIfNotSuspended();
        if (an != null && an.lastUsedDocumentId < docId) {
            p.suspend(sm);
            return true;
        }
        return false;
    }

    public void removeProvider(Provider p) {
        synchronized (activeProviders) {
            activeProviders.remove(p.id);
        }
        synchronized (providers) {
            providers.remove(p.id);
        }
    }

    public void dumpStat() {
        for(Neuron n: getActiveNeurons()) {
            TNeuron tn = (TNeuron) n.get();
            tn.dumpStat();
        }
        System.out.println();
    }

    public void dumpModel() {
        System.out.println();
        System.out.println("Dump Model:");
        for(Neuron n: getActiveNeurons()) {
            TNeuron tn = (TNeuron) n.get();
            System.out.println(tn.toStringWithSynapses());
            System.out.println();
        }
    }
}
