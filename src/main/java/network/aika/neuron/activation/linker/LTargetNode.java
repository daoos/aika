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
package network.aika.neuron.activation.linker;

import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;

import static network.aika.neuron.activation.linker.Mode.INDUCTION;


/**
 *
 * @author Lukas Molzberger
 */
public class LTargetNode<N extends INeuron> extends LNode<N> {

    public LTargetNode(Class<N> neuronClass, Boolean isMature, String label) {
        super(neuronClass, isMature, label);
    }

    public Activation follow(Mode m, INeuron n, Activation act, LLink from, Activation startAct) {
        if(n == null && m == INDUCTION) {
            try {
                Model model = startAct.getNeuron().getModel();
                n = neuronClass.getConstructor(Model.class, String.class)
                        .newInstance(model, "");
            } catch (Exception e) {
            }
        }

        if(act == null) {
            Document doc = startAct.getDocument();
            act = new Activation(doc.getNewActivationId(), doc, n);
        }

        return act;
    }
}
