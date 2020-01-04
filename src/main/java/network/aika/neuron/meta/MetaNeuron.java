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

import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.MetaInhibSynapse;

import java.util.*;
import java.util.function.Function;

import static network.aika.neuron.Synapse.State.CURRENT;


/**
 *
 * @author Lukas Molzberger
 */
public class MetaNeuron extends ConjunctiveNeuron<MetaSynapse> {

    public static final String TYPE_STR = Model.register("NM", MetaNeuron.class);

    public static double COVERED_THRESHOLD = 5.0;

    public InhibitoryNeuron inhibitoryNeuron;


    private MetaNeuron() {
        super();
    }


    public MetaNeuron(Neuron p) {
        super(p);
    }


    public MetaNeuron(Model model, String label) {
        super(model, label);
    }


    public String getType() {
        return TYPE_STR;
    }


    public String typeToString() {
        return "META";
    }



/*
    public static void induce(Model model, int threadId) {
        for(Neuron n: model.getActiveNeurons()) {
            if(n.get() instanceof ExcitatoryNeuron) {
                List<ExcitatorySynapse> candidateSynapses = n
                        .getActiveOutputSynapses()
                        .stream()
                        .map(s -> (ExcitatorySynapse) s)
                        .collect(Collectors.toList());

                double coveredScore = coveredSum(candidateSynapses);

                if (coveredScore > COVERED_THRESHOLD) {
                    createNewMetaNeuron(model, threadId, n, candidateSynapses);
                }
            }
        }
    }


    public static void createNewMetaNeuron(Model model, int threadId, Neuron inputNeuron, List<ExcitatorySynapse> candidateSynapses) {
        MetaNeuron mn = new MetaNeuron(model,"");

        MetaSynapse ms = new MetaSynapse(inputNeuron, mn.getProvider(), 0, model.charCounter);
        ms.link();

        for(ExcitatorySynapse ts: candidateSynapses) {
            new MetaNeuron.MappingLink(mn, (ExcitatoryNeuron) ts.getOutput().get(), ts.getUncovered()).link();
            new MetaSynapse.MappingLink(ms, ts).link();
        }

        mn.train(threadId);

        InhibitoryNeuron.induceOutgoing(threadId, mn);
    }


    public static double coveredSum(List<ExcitatorySynapse> syns) {
        double sum = 0.0;
        for (ExcitatorySynapse s : syns) {
            sum += s.getUncovered();
        }
        return sum;
    }



    public void train(int threadId) {
        do {
            double diff;
            do {
                for (Synapse s : getProvider().getActiveInputSynapses()) {
                    MetaSynapse ms = (MetaSynapse) s;

                    ms.updateWeight();
                }
                updateBias();

                commit(getInputSynapses());

                diff = 0.0;
                for (MappingLink ml : targetNeurons.values()) {
                    diff += ml.computeNij();
                }
            } while (diff > 1.0);

            propagateToOutgoingInhibNeurons(threadId);

        } while(induceInputInhibNeurons() || expand(threadId));
    }


    private void propagateToOutgoingInhibNeurons() {
        for (Synapse s : getProvider().getActiveOutputSynapses()) {
            if (s instanceof MetaInhibSynapse) {
                MetaInhibSynapse ms = (MetaInhibSynapse) s;

                InhibitoryNeuron in = (InhibitoryNeuron) ms.getOutput().get();

                in.train(this);
            }
        }
    }



    private boolean expand(int threadId) {
        boolean changed = false;
        double sumNij = getNijSum();

        for(Synapse s: getInputSynapses()) {
            MetaSynapse ms = (MetaSynapse) s;

            if(ms.getWeight() > 0.5) {
                for (MetaSynapse.MappingLink ml : ms.targetSynapses.values()) {
                    ExcitatorySynapse ts = ml.targetSynapse;

                    for (Relation.Key trk : ts.getRelations()) {
                        Integer relSynId = trk.getRelatedId();

                        if (relSynId != OUTPUT) {
                            ExcitatorySynapse relTargetSyn = (ExcitatorySynapse) ts.getOutput().getSynapseById(relSynId);

                            List<Neuron> candidates = new ArrayList<>();
                            collectCandidates(candidates, relTargetSyn.getInput());

                            for (Neuron cand : candidates) {
                                MetaSynapse relMetaSyn = lookupMetaSynapse(relTargetSyn, cand);

                                Relation.Key mrk = ms.getRelation(new Relation.Key(relMetaSyn.getId(), trk.getRelation(), trk.getDirection()));
                                WeightedRelation wtr = (WeightedRelation) trk.getRelation();

                                if (mrk == null) {
                                    Relation mr = wtr.copy();

                                    mr.link(getProvider(), ms.getId(), relMetaSyn.getId());
                                }
                            }
                        }
                    }
                }
            }
        }

        return changed;
    }


    private boolean induceInputInhibNeurons() {
        boolean changed = false;
        for(Synapse s: getInputSynapses()) {
            MetaSynapse ms = (MetaSynapse) s;

            if (ms.getWeight() > 0.5) {
                Map<InduceKey, List<MetaSynapse>> tmp = new TreeMap<>();
                for(Relation.Key rk : ms.getRelations()) {
                    MetaSynapse relMS = (MetaSynapse) getProvider().getSynapseById(rk.getRelatedId());
                    WeightedRelation wr = (WeightedRelation) rk.getRelation();

                    Relation keyRel = wr.getKeyRelation();
                    if (keyRel instanceof PositionRelation) {
                        PositionRelation pr = (PositionRelation) keyRel;
                        InduceKey ik = new InduceKey(pr.fromSlot, keyRel, rk.getDirection());

                        List<MetaSynapse> l = tmp.get(ik);
                        if (l == null) {
                            l = new ArrayList<>();
                            tmp.put(ik, l);
                        }

                        l.add(relMS);
                    }
                }
            }
        }
        return changed;
    }


    private MetaSynapse lookupMetaSynapse(ExcitatorySynapse targetSyn, Neuron cand) {
        MetaSynapse.MappingLink ml = targetSyn.getMetaSynapse(cand, getProvider());

        if (ml != null) {
            return ml.metaSynapse;
        } else {
            int newSynId = getNewSynapseId();

            MetaSynapse metaSyn = new MetaSynapse(cand, getProvider(), newSynId, getModel().charCounter);
            ml = new MetaSynapse.MappingLink(metaSyn, targetSyn);

            metaSyn.targetSynapses.put((ExcitatoryNeuron) targetSyn.getOutput().get(), ml);
            targetSyn.metaSynapses.put(metaSyn, ml);

            metaSyn.link();

            return metaSyn;
        }
    }


    private void applyRefinement(int threadId, Refinement ref, List<ExcitatorySynapse> targetSyns) {
        int newSynId = getNewSynapseId();

        if(ref.target == null) {
            ref.target = InhibitoryNeuron.induceIncoming(getModel(), threadId, targetSyns);
        }

        MetaSynapse nms = new MetaSynapse(ref.target.getProvider(), getProvider(), newSynId, getModel().charCounter);

        if(nms != null) {
            nms.link();

            for(ExcitatorySynapse ts: targetSyns) {
                new MetaSynapse.MappingLink(nms, ts).link();
            }

            Relation newRelation = null; //ref.keyRelation.newInstance();
            newRelation.link(getProvider(), ref.anchor.getId(), nms.getId());
        }
    }


    public static void collectCandidates(List<Neuron> results, Neuron n) {
        results.add(n);

        for(Synapse s: n.getActiveOutputSynapses()) {
            if(s.getOutput().getType() == INeuron.Type.INHIBITORY) {
                collectCandidates(results, n);
            }
        }
    }
*/

    public void updateBias() {
        double sum = 0.0;
        double norm = 0.0;

        for (Synapse ts: inhibitoryNeuron.getOutputSynapses()) {
            INeuron tn = ts.getOutput();

            if(tn instanceof ExcitatoryNeuron) {
                ExcitatoryNeuron targetNeuron = (ExcitatoryNeuron) tn;
                double bj = targetNeuron.getBias();

                sum += bj;
                norm += 1.0;
            }
        }

        setBias(sum / norm);
    }


    public InhibitoryNeuron getInhibitoryNeuron() {
        return inhibitoryNeuron;
    }


    public void setInhibitoryNeuron(InhibitoryNeuron inhibitoryNeuron) {
        this.inhibitoryNeuron = inhibitoryNeuron;
    }


    public void train(Config c, Activation o) {
//        transferMetaSynapses(c, o);

        super.train(c, o);
    }


    @Override
    public boolean isMature(Config c) {
        return false;
    }

    /*
        public TNeuron getInputTargets(TDocument doc, Activation in) {
            return doc.metaActivations.get(in);
        }
    */
    public ExcitatoryNeuron getTargetNeuron(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        ExcitatoryNeuron targetNeuron = createMetaNeuronTarget(metaAct, callback);

        return targetNeuron;
    }


    public ExcitatoryNeuron createMetaNeuronTarget(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        return createMetaNeuronTarget(metaAct.getDocument(), callback.apply(metaAct));
    }


    public ExcitatoryNeuron createMetaNeuronTargetFromLabel(Document doc, String label, ExcitatoryNeuron targetNeuron) {
        return createMetaNeuronTarget(doc, new ExcitatoryNeuron(getModel(), getInhibitoryNeuron().getLabel().substring(2) + "-" + label));
    }


    public ExcitatoryNeuron createMetaNeuronTarget(Document doc, ExcitatoryNeuron targetNeuron) {
        System.out.println("New Meta Neuron Instance: " + targetNeuron.getLabel() + " Bias:" + getBias());

        initMetaNeuronTarget(doc, targetNeuron);

        return targetNeuron;
    }


    private void initMetaNeuronTarget(Document doc, ExcitatoryNeuron tn) {
        tn.setInhibitoryNeuron(getInhibitoryNeuron());

        transferNegativeMetaInputSynapses(doc, tn);
        transferMetaOutputSynapses(doc, getInhibitoryNeuron(), tn);

        tn.trainingBias = trainingBias;
        tn.setBias(getBias() - trainingBias);

        tn.computeOutputRelations();

        tn.commit(tn.getProvider().getActiveInputSynapses());
    }


    private void transferNegativeMetaInputSynapses(Document doc, ExcitatoryNeuron targetNeuron) {
        for (Synapse templateSynapse : getProvider().getActiveInputSynapses()) {
            MetaSynapse ms = (MetaSynapse) templateSynapse;

            if (ms != null && templateSynapse.isNegative(CURRENT)) {
                ms.transferTemplateSynapse(doc, ms.getInput(), targetNeuron, null);
            }
        }
    }


    private void transferMetaOutputSynapses(Document doc, InhibitoryNeuron inhibNeuron, ExcitatoryNeuron targetNeuron) {
        for (Synapse templateSynapse : getProvider().getActiveOutputSynapses()) {
            if (templateSynapse.getOutput().getId() == inhibNeuron.getId()) {
                MetaInhibSynapse mis = (MetaInhibSynapse) templateSynapse;

                InhibitorySynapse targetSynapse = mis.transferMetaSynapse(doc, targetNeuron);

                List<Synapse> modifiedSynapses = Collections.singletonList(targetSynapse);
                targetSynapse.getOutput().commit(modifiedSynapses);
            }
        }
    }

/*
    public void transferMetaSynapses(Config config, Activation metaActActivation) {
        if(metaActActivation.targetNeuron == null) {
            return;
        }

        Document doc = metaActActivation.getAct().getDocument();

        metaActActivation.inputOptions.entrySet().stream()
                .filter(me -> me.getValue() != null && me.getValue().getP() >= config.getMetaThreshold())
                .forEach(me -> {
                    Link l = me.getKey();
                    Activation inState = me.getValue();

                    MetaSynapse templateSynapse = (MetaSynapse) l.getSynapse();

                    if (templateSynapse != null) {
                        TNeuron inputNeuron = inState.targetNeuron;

                        templateSynapse.transferTemplateSynapse(doc, inputNeuron, metaActActivation.targetNeuron, l);
                    }
                });
    }
*/
}