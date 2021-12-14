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
package network.aika.neuron.excitatory;

import network.aika.Model;
import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.fields.Field;
import network.aika.neuron.activation.PatternActivation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * @author Lukas Molzberger
 */
public class BindingNeuron extends ExcitatoryNeuron<BindingNeuronSynapse, BindingActivation> {

    private Field finalBias = new Field(u ->
        biasUpdate(u, true)
    );

    public BindingNeuron() {
        super();
    }

    public BindingNeuron(Model model, boolean addProvider) {
        super(model, addProvider);
    }

    @Override
    public BindingActivation createActivation(Thought t) {
        return new BindingActivation(t.createActivationId(), t, this);
    }

    @Override
    public boolean checkBindingSignalType(Activation bsAct) {
        return bsAct instanceof PatternActivation;
    }

    public Field getFinalBias() {
        return finalBias;
    }

    @Override
    public boolean allowTemplatePropagate(Activation act) {
        Neuron n = act.getNeuron();

        if(n.isInputNeuron())
            return false;

        if(act.getOutputGradient().updateAvailable())
            return false;

        return true;
    }

    @Override
    public BindingNeuron instantiateTemplate(boolean addProvider) {
        BindingNeuron n = new BindingNeuron(getModel(), addProvider);
        initFromTemplate(n);

        return n;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        finalBias.write(out);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        finalBias.readFields(in, m);
    }
}
