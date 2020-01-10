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
package network.aika.neuron.meta;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 * @author Lukas Molzberger
 */
public class MetaSynapse extends ConjunctiveSynapse<TNeuron, MetaNeuron> {

    public static final String TYPE_STR = Model.register("SM", MetaSynapse.class);


    public boolean isMetaVariable;

    Map<ExcitatoryNeuron, MappingLink> targetSynapses = new TreeMap<>();

    public MetaSynapse() {
        super();
    }

    public MetaSynapse(Neuron input, Neuron output, boolean recurrent, boolean propagate, int lastCount) {
        super(input, output, recurrent, propagate, lastCount);
    }


    @Override
    public String getType() {
        return TYPE_STR;
    }

/*
    public void updateWeight() {
        double sum = 0.0;
        double norm = 0.0;

        for(MappingLink sml: targetSynapses.values()) {
            MetaNeuron mn = (MetaNeuron) sml.metaSynapse.getOutput().get();
            ExcitatoryNeuron tn = (ExcitatoryNeuron) sml.targetSynapse.getOutput().get();
            MetaNeuron.MappingLink nml = mn.targetNeurons.get(tn);

            double nij = nml.nij;
            double wjl = sml.targetSynapse.getWeight();

            sum += nij * wjl;
            norm += nij;
        }

        update(null, sum / norm);
    }
*/

    public void transferTemplateSynapse(Document doc, TNeuron<?> inputNeuron, ExcitatoryNeuron targetNeuron, Link metaLink) {
        if(metaLink.getTargetSynapse() != null) {
            return;
        }

        ExcitatorySynapse targetSynapse = targetNeuron.createOrLookupSynapse(doc, this, inputNeuron.getProvider());

        new MappingLink(this, targetSynapse).link();

        targetSynapse.updateDelta(
                doc,
                getWeight()
        );

        System.out.println("  Transfer Template Synapse: IN:" +
                inputNeuron.getLabel() +
                " OUT:" + targetNeuron.getLabel() +
//                " M-SynId:" + getId() +
//                " T-SynId:" + targetSynapse.getId() +
                " W:" + targetSynapse.getNewWeight()
        );

        metaLink.setTargetSynapse(targetSynapse);
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(isMetaVariable);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        isMetaVariable = in.readBoolean();
    }


    /**
     *
     * @author Lukas Molzberger
     */
    public static class Builder extends Synapse.Builder {

        public boolean isMetaVariable;


        public Builder setIsMetaVariable(boolean isMetaVariable) {
            this.isMetaVariable = isMetaVariable;
            return this;
        }

        public Synapse getSynapse(Neuron outputNeuron) {
            MetaSynapse s = (MetaSynapse) super.getSynapse(outputNeuron);

            s.isMetaVariable = isMetaVariable;

            return s;
        }

        protected SynapseFactory getSynapseFactory() {
            return (input, output) -> new MetaSynapse(input, output, recurrent, propagate, output.getModel().charCounter);
        }
    }


    public static class MappingLink {
        public MetaSynapse metaSynapse;
        public ExcitatorySynapse targetSynapse;

        public MappingLink(MetaSynapse metaSynapse, ExcitatorySynapse targetSynapse) {
            this.metaSynapse = metaSynapse;
            this.targetSynapse = targetSynapse;
        }

        public void link() {
            metaSynapse.targetSynapses.put((ExcitatoryNeuron) targetSynapse.getOutput(), this);
            targetSynapse.metaSynapses.put(metaSynapse, this);
        }
    }
}