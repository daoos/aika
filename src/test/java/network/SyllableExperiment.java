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
package network;

import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.*;
import network.aika.neuron.excitatory.pattern.PatternSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.PatternScope.INPUT_PATTERN;
import static network.aika.neuron.PatternScope.SAME_PATTERN;

/**
 *
 * @author Lukas Molzberger
 */
public class SyllableExperiment {

    Model model;

    Map<Character, PatternNeuron> inputLetters = new TreeMap<>();
    InhibitoryNeuron inputInhibN;
    ExcitatoryNeuron relN;


    @Before
    public void init() {
        model = new Model();

        model.setTrainingConfig(
                new Config()
                        .setLearnRate(0.025)
                        .setMetaThreshold(0.3)
                        .setMaturityThreshold(10)
        );

        inputInhibN = new InhibitoryNeuron(model, "INPUT INHIB", PatternNeuron.type);
        relN = new PatternPartNeuron(model, "Char-Relation");
    }


    public PatternNeuron lookupChar(Character c) {
        PatternNeuron n = inputLetters.get(c);
        if(n == null) {
            n = new PatternNeuron(model, "" + c);
            inputLetters.put(c, n);
        }
        return n;
    }


    private void train(String word) {
        Document doc = new Document(word);
        System.out.println("  " + word);

        Activation lastAct = null;
        Activation lastInInhibAct = null;
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);

            Activation currentAct = lookupChar(c).addInputActivation(doc,
                    new Activation.Builder()
                            .setInputTimestamp(i)
                            .setFired(0)
                            .setValue(1.0)
                            .setRangeCoverage(1.0)
            );

            Activation currentInInhibAct = inputInhibN.addInputActivation(doc,
                    new Activation.Builder()
                            .setInputTimestamp(i)
                            .setFired(0)
                            .setValue(1.0)
                            .setRangeCoverage(1.0)
                            .addInputLink(SAME_PATTERN, currentAct)
            );

            if(lastAct != null) {
                relN.addInputActivation(doc,
                        new Activation.Builder()
                            .setInputTimestamp(i)
                            .setFired(0)
                            .setValue(1.0)
                            .addInputLink(INPUT_PATTERN, lastInInhibAct)
                            .addInputLink(SAME_PATTERN, currentInInhibAct)
                );
            }

            lastAct = currentAct;
            lastInInhibAct = currentInInhibAct;
        }

        System.out.println(doc.activationsToString());

        doc.train(model);
    }

    @Test
    public void testTraining() throws IOException {
        for(String word: Util.loadExamplesAsWords(new File("/Users/lukas.molzberger/aika-ws/maerchen"))) {
            train( word + " ");
        }

        model.dumpModel();
        System.out.println();
    }

    @Test
    public void testTrainingDer() throws IOException {
        PatternNeuron derN = initDer();

        train("der ");

/*        for(String word: Util.loadExamplesAsWords(new File("/Users/lukas.molzberger/aika-ws/maerchen"))) {
            train( word + " ");
        }
*/
        model.dumpModel();
        System.out.println();
    }

    public PatternNeuron initDer() {
        PatternPartNeuron eD = new PatternPartNeuron(model, "TP-d");
        PatternPartNeuron eE = new PatternPartNeuron(model, "TP-e");
        PatternPartNeuron eR = new PatternPartNeuron(model, "TP-r");

        PatternNeuron derN = new PatternNeuron(model, "P-der");

        Neuron.init(eD, 5.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(lookupChar('d'))
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(derN)
                        .setWeight(10.0)
        );

        Neuron.init(eE, 5.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(lookupChar('e'))
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(eD)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(derN)
                        .setWeight(10.0)
        );

        Neuron.init(eR, 5.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(lookupChar('r'))
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(eE)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(derN)
                        .setWeight(10.0)
        );

        Neuron.init(derN, 1.0,
                new PatternSynapse.Builder()
                        .setNeuron(eD)
                        .setWeight(10.0),
                new PatternSynapse.Builder()
                        .setNeuron(eE)
                        .setWeight(10.0),
                new PatternSynapse.Builder()
                        .setNeuron(eR)
                        .setWeight(10.0)
        );

        return derN;
    }
}
