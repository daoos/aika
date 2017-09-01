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


import org.aika.Activation;
import org.aika.Input;
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.AbstractNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

import java.util.TreeMap;

import static org.aika.corpus.Range.Operator.*;


/**
 *
 * @author Lukas Molzberger
 */
public class RecurrentPatternTest {

    @Test
    public void testRecurrentPattern() {
        String txt = "A B C D E F G H.";

        Model m = new Model();

        InputNeuron startSignal = m.createOrLookupInputNeuron("START-SIGNAL");
        InputNeuron spaceN = m.createOrLookupInputNeuron("SPACE");


        TreeMap<Character, InputNeuron> chars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            InputNeuron charSN = m.createOrLookupInputNeuron("" + c);
            chars.put(c, charSN);
        }

        Neuron ctNeuron = m.initCounterNeuron(m.createNeuron("CTN"), spaceN, false, startSignal, true, false);

        TreeMap<Character, AbstractNeuron> recChars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            InputNeuron charSN = chars.get(c);
            recChars.put(c, m.initRelationalNeuron(m.createNeuron("RN-" + c), ctNeuron, charSN, false));
        }

        Neuron patternN = m.initAndNeuron(m.createNeuron("PATTERN"),
                0.001,
                new Input()
                        .setNeuron(recChars.get('C'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0)
                        .setStartRangeMatch(EQUALS)
                        .setEndRangeMatch(GREATER_THAN)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(recChars.get('D'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(1)
                        .setRangeMatch(RangeRelation.CONTAINS),
                new Input()
                        .setNeuron(recChars.get('E'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(2)
                        .setStartRangeMatch(LESS_THAN)
                        .setEndRangeMatch(EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(ctNeuron)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0)
                        .setRangeMatch(RangeRelation.NONE)
        );

        Document doc = m.createDocument(txt, 0);

        startSignal.addInput(doc, 0, 1, 0);
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c == ' ') {
                spaceN.addInput(doc, i, i + 1);
            }
        }
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ' && c != '.') {
                chars.get(c).addInput(doc, i, i + 1);
            }
        }

        System.out.println("All activations:");
        System.out.println(doc.networkStateToString(true, true, false, true));
        System.out.println();

        doc.process();

        System.out.println("All activations:");
        System.out.println(doc.networkStateToString(true, true, false, true));
        System.out.println();

        Activation patAct = patternN.node.get().getFirstActivation(doc);
        Assert.assertEquals(4, patAct.key.r.begin.intValue());
        Assert.assertEquals(10, patAct.key.r.end.intValue());

        doc.clearActivations();

    }



    @Test
    public void testRecurrentPattern1() {
        String txt = "A B C D E F G H.";

        Model m = new Model();

        InputNeuron startSignal = m.createOrLookupInputNeuron("START-SIGNAL");
        InputNeuron spaceN = m.createOrLookupInputNeuron("SPACE");

        TreeMap<Character, InputNeuron> chars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            InputNeuron charSN = m.createOrLookupInputNeuron("" + c);
            chars.put(c, charSN);
        }

        Neuron ctNeuron = m.createNeuron("CTN");

        m.initCounterNeuron(ctNeuron, spaceN, false, startSignal, true, false);

        TreeMap<Character, AbstractNeuron> recChars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            InputNeuron charSN = chars.get(c);
            recChars.put(c, m.initRelationalNeuron(m.createNeuron("RN-" + c), ctNeuron, charSN, false));
        }

        Neuron patternN = m.initAndNeuron(m.createNeuron("PATTERN"),
                0.001,
                new Input()
                        .setNeuron(recChars.get('C'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0)
                        .setStartRangeMatch(EQUALS)
                        .setEndRangeMatch(GREATER_THAN)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(recChars.get('D'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(1)
                        .setRangeMatch(RangeRelation.CONTAINS),
                new Input()
                        .setNeuron(recChars.get('E'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(2)
                        .setStartRangeMatch(LESS_THAN)
                        .setEndRangeMatch(EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(ctNeuron)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0)
                        .setRangeMatch(RangeRelation.NONE)
        );


        Document doc = m.createDocument(txt, 0);

        startSignal.addInput(doc, 0, 1, 0);
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c == ' ') {
                spaceN.addInput(doc, i, i + 1);
            }
        }
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ' && c != '.') {
                chars.get(c).addInput(doc, i, i + 1);
            }
        }

        doc.process();

        System.out.println("All activations:");
        System.out.println(doc.networkStateToString(true, true, false, true));
        System.out.println();

        Activation patAct = patternN.node.get().getFirstActivation(doc);
        Assert.assertEquals(4, patAct.key.r.begin.intValue());
        Assert.assertEquals(10, patAct.key.r.end.intValue());

        doc.clearActivations();

    }


    @Test
    public void testAndWithRid() {
        Model m = new Model();

        InputNeuron start = m.createOrLookupInputNeuron("START");
        InputNeuron clock = m.createOrLookupInputNeuron("CLOCK");
        InputNeuron input = m.createOrLookupInputNeuron("INPUT");

        Neuron ctn = m.initCounterNeuron(m.createNeuron("CTN"), clock, false, start, true, false);
        Neuron rn = m.initRelationalNeuron(m.createNeuron("RN"), ctn, input, false);

        InputNeuron aN = m.createOrLookupInputNeuron("A");
        InputNeuron bN = m.createOrLookupInputNeuron("B");
        InputNeuron cN = m.createOrLookupInputNeuron("C");

        Neuron result = m.initAndNeuron(m.createNeuron("RESULT"),
                0.001,
                new Input()
                        .setNeuron(ctn)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setAbsoluteRid(1),
                new Input()
                        .setNeuron(aN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(bN)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
                        .setAbsoluteRid(0)
        );
    }


    @Test
    public void testCTNeuron() {
        Model m = new Model();

        InputNeuron start = m.createOrLookupInputNeuron("START");
        InputNeuron clock = m.createOrLookupInputNeuron("CLOCK");

        Neuron ctn = m.initCounterNeuron(m.createNeuron("CTN"), clock, false, start, true, false);


        Document doc = m.createDocument("                                                  ", 0);

        for(int i = 5; i < 30; i += 5) {
            clock.addInput(doc, i - 1, i);
        }

        System.out.println(doc.networkStateToString(true, false, false, true));

        start.addInput(doc, 0, 1, 0);

        System.out.println(doc.networkStateToString(true, false, false, true));

        Assert.assertEquals(2, Activation.get(doc, ctn.node.get(), 2, new Range(10, 15), EQUALS, EQUALS, null, null).key.o.primId);
    }
}
