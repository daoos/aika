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
package org.aika.network;


import org.aika.Neuron;
import org.aika.neuron.Synapse;
import org.aika.corpus.Range.Relation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.InterpretationNode;
import org.aika.corpus.Range;
import org.aika.lattice.Node;
import org.aika.lattice.OrNode;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.corpus.Range.Mapping.BEGIN;
import static org.aika.corpus.Range.Mapping.END;
import static org.aika.corpus.Range.Relation.NONE;
import static org.aika.corpus.Range.Operator.EQUALS;

/**
 *
 * @author Lukas Molzberger
 */
public class NegationTest {


    @Test
    public void testTwoNegativeInputs1() {
        Model m = new Model();
        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");

        Neuron abcN = m.createNeuron("ABC");

        Neuron.init(abcN,
                5.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-9.5)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(-10.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inC)
                        .setWeight(-10.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        inA.addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));
        Assert.assertNotNull(Activation.get(doc, abcN.get(), null, new Range(0, 11), Relation.EQUALS, null, null));

        InterpretationNode o1 = InterpretationNode.addPrimitive(doc);

        inB.addInput(doc,
                new Activation.Builder()
                        .setRange(2, 7)
                        .setInterpretation(o1)
        );

        System.out.println(doc.neuronActivationsToString(true, false, true));

        InterpretationNode o2 = InterpretationNode.addPrimitive(doc);

        inC.addInput(doc,
                new Activation.Builder()
                        .setRange(4, 9)
                        .setInterpretation(o2)
        );

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(Activation.get(doc, abcN.get(), null, new Range(0, 11), Relation.EQUALS, null, null));
    }


    @Test
    public void testTwoNegativeInputs2() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");

        Neuron inB = m.createNeuron("B");

        Neuron inC = m.createNeuron("C");

        Neuron abcN = m.createNeuron("ABC");

        Neuron outN = Neuron.init(m.createNeuron("OUT"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(abcN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(0.0)
        );

        Neuron.init(abcN,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-1.0),
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setBias(0.0),
                new Synapse.Builder()
                        .setNeuron(inC)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setBias(0.0)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        inA.addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        InterpretationNode ob = InterpretationNode.addPrimitive(doc);
        inB.addInput(doc,
                new Activation.Builder()
                        .setRange(2, 7)
                        .setInterpretation(ob)
        );

        System.out.println(doc.neuronActivationsToString(true, false, true));

        InterpretationNode oc = InterpretationNode.addPrimitive(doc);
        inC.addInput(doc,
                new Activation.Builder()
                        .setRange(4, 9)
                        .setInterpretation(oc)
        );

        System.out.println(doc.neuronActivationsToString(true, false, true));

//        Assert.assertNull(Activation.get(t, outN.node, 0, new Range(0, 11), Range.Relation.EQUALS, null, null, null));
    }


    @Test
    public void testSimpleNegation1() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");

        Neuron asN = m.createNeuron("AS");

        Neuron inS = m.createNeuron("S");

        Neuron outN = Neuron.init(m.createNeuron("OUT"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeOutput(true)
        );

        Neuron.init(asN,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeMatch(NONE)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        InterpretationNode o = InterpretationNode.addPrimitive(doc);

        inS.addInput(doc,
                new Activation.Builder()
                        .setRange(3, 8)
                        .setInterpretation(o)
        );

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inA.addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(Activation.get(doc, outN.get(), null, new Range(0, 11), Relation.EQUALS, null, null));

        doc.clearActivations();
    }


    @Test
    public void testSimpleNegation2() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");

        Neuron asN = m.createNeuron("AS");

        Neuron inS = m.createNeuron("S");

        Neuron outN = Neuron.init(m.createNeuron("OUT"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeOutput(true)
        );

        Neuron.init(asN,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.CONTAINS)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        InterpretationNode o = InterpretationNode.addPrimitive(doc);

        inS.addInput(doc,
                new Activation.Builder()
                        .setRange(3, 8)
                        .setInterpretation(o)
        );

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inA.addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(Activation.get(doc, outN.get(), null, new Range(0, 11), Relation.EQUALS, null, null));

        doc.clearActivations();
    }


    @Test
    public void testSimpleNegation3() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");

        Neuron asN = m.createNeuron("AS");

        Neuron inS = m.createNeuron("S");

        Neuron outN = Neuron.init(m.createNeuron("OUT"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeOutput(true)
        );

        Neuron.init(asN,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.CONTAINS)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        InterpretationNode o = InterpretationNode.addPrimitive(doc);

        inA.addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inS.addInput(doc,
                new Activation.Builder()
                        .setRange(3, 8)
                        .setInterpretation(o)
        );

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(Activation.get(doc, outN.get(), null, new Range(0, 11), Relation.EQUALS, null, null));

        doc.clearActivations();
    }


    @Test
    public void testNegation1() {
        Model m = new Model();
        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron asN = m.createNeuron("AS");
        Neuron absN = m.createNeuron("ABS");
        Neuron bsN = m.createNeuron("BS");

        Neuron inS = Neuron.init(m.createNeuron("S"),
                0.0,
                INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(absN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );

        Neuron.init(asN,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.CONTAINS)
        );
        Neuron.init(absN,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(EQUALS, Range.Operator.NONE)
                        .setRangeOutput(BEGIN, Range.Mapping.NONE),
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(Range.Operator.NONE, EQUALS)
                        .setRangeOutput(Range.Mapping.NONE, END),
                new Synapse.Builder()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.CONTAINS)
        );

        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.addInput(doc, 0, 6);
            System.out.println(doc.neuronActivationsToString(true, false, true));

            inB.addInput(doc, 0, 6);

            System.out.println(doc.neuronActivationsToString(true, false, true));

            Assert.assertNotNull(Activation.get(doc, inS.get(), null, new Range(0, 6), Relation.EQUALS, null, null));
            Assert.assertEquals(2, Activation.get(doc, inS.get(), null, new Range(0, 6), Relation.EQUALS, null, null).key.interpretation.orInterpretationNodes.size());

            doc.clearActivations();
        }

        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.addInput(doc, 0, 6);
            System.out.println(doc.neuronActivationsToString(true, false, true));

            inB.addInput(doc, 3, 9);

            System.out.println(doc.neuronActivationsToString(true, false, true));

//            Assert.assertNotNull(Activation.get(t, inS.node, 0, new Range(0, 6), EQUALS, EQUALS, null, null, null));
            Assert.assertNotNull(Activation.get(doc, inS.get(), null, new Range(0, 9), Relation.EQUALS, null, null));
//            Assert.assertEquals(1, Activation.get(t, inS.node, 0, new Range(0, 6), EQUALS, EQUALS, null, null, null).key.interpretation.orInterprNodes.size());
            Assert.assertEquals(1, Activation.get(doc, inS.get(), null, new Range(0, 6), Relation.EQUALS, null, null).key.interpretation.orInterpretationNodes.size());
            Assert.assertEquals(1, Activation.get(doc, inS.get(), null, new Range(0, 9), Relation.EQUALS, null, null).key.interpretation.orInterpretationNodes.size());

            doc.clearActivations();
        }
    }


    @Test
    public void testNegation2() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");

        Neuron asN = m.createNeuron("AS");
        Neuron ascN = m.createNeuron("ASC");
        Neuron bsN = m.createNeuron("BS");

        Neuron inS = Neuron.init(m.createNeuron("S"),
                0.0,
                INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(ascN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(bsN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );

        Neuron.init(asN,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );
        Neuron.init(ascN,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );

        Neuron.init(bsN,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );

        Neuron outA = Neuron.init(m.createNeuron("OUT A"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );
        Neuron outAC = Neuron.init(m.createNeuron("OUT AC"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(ascN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );
        Neuron outB = Neuron.init(m.createNeuron("OUT B"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(bsN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);


        inA.addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inB.addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inC.addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        doc.process();

        System.out.println(doc.neuronActivationsToString( true, false, true));
    }




    /**
     *
     *       -----
     *  A ---| &  |------
     *     -*| C  |     |       ------
     *     | ------     |   G---| &  |
     *      \           |       | H  |-----
     *       \/-----------------|    |
     *       /\-----------------|    |
     *      /           |       ------
     *     | ------     |
     *     -*| &  |------
     *  B ---| D  |
     *       ------
     *
     */

    @Test
    public void testOptions() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Node inANode = inA.get().node.get();

        Neuron inB = m.createNeuron("B");
        Node inBNode = inB.get().node.get();


        Neuron pC = m.createNeuron("C");
        Neuron pD = m.createNeuron("D");

        Neuron.init(pC,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pD)
                        .setWeight(-2.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
        );

        Neuron.init(pD,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pC)
                        .setWeight(-2.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
        );


        Neuron inG = m.createNeuron("G");
        OrNode inGNode = inG.get().node.get();

        Neuron pH = Neuron.init(m.createNeuron("H"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(pC)
                        .setBias(-2.0)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pD)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inG)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);
        inG.addInput(doc, 0, 1);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(pC.get().getFirstActivation(doc));
        Assert.assertNotNull(pD.get().getFirstActivation(doc));

        // Die Optionen 0 und 2 stehen in Konflikt. Da sie aber jetzt in Oder Optionen eingebettet sind, werden sie nicht mehr ausgefiltert.
//        Assert.assertNull(pH.node.getFirstActivation(t));
    }


}
