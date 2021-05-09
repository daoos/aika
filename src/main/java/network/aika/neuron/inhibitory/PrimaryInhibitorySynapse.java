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
package network.aika.neuron.inhibitory;


import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.Visitor;

/**
 *
 * @author Lukas Molzberger
 */
public class PrimaryInhibitorySynapse extends InhibitorySynapse {


    public PrimaryInhibitorySynapse() {
        super();
    }

    public PrimaryInhibitorySynapse(Neuron<?> input, InhibitoryNeuron output, Synapse template) {
        super(input, output, template);
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return v.startDir != Direction.INPUT;
    }

    @Override
    public PrimaryInhibitorySynapse instantiateTemplate(Neuron<?> input, InhibitoryNeuron output) {
        assert input.getTemplates().contains(getInput());

        return new PrimaryInhibitorySynapse(input, output, this);
    }

    /*
    @Override
    public Set<ScopeEntry> transition(ScopeEntry s, Direction dir, Direction startDir, boolean checkFinalRequirement) {
        if (dir == INPUT) {
            switch (s.getScope()) {
                case I_SAME:
                    return s.nextSet(I_INPUT);
            }
        } else {
            switch (s.getScope()) {
                case I_INPUT:
                    return s.nextSet(I_SAME);
            }
        }
        return Collections.emptySet();
    }
     */
}