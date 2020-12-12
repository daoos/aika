package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.activation.Visitor;

import static network.aika.neuron.activation.Visitor.Transition.ACT;

public class PatternSynapse<I extends Neuron<?>> extends ExcitatorySynapse<I, PatternNeuron> {

    public static byte type;

    public PatternSynapse() {
        super();
    }

    public PatternSynapse(I input, PatternNeuron output, Synapse template) {
        super(input, output, template);
    }


    @Override
    public void updateReference(Link l) {
        Reference or = l.getOutput().getReference();
        Reference ir = l.getInput().getReference();

        l.getOutput().propagateReference(or == null ? ir : or.add(ir));
    }

    @Override
    public boolean checkTemplate(Activation iAct, Activation oAct, Visitor v) {
        return true;
    }

    @Override
    public boolean checkInduction(Link l) {
        return true;
    }

    @Override
    protected boolean checkOnCreate(Activation fromAct, Activation toAct, Visitor v) {
        return true;
    }

    @Override
    public PatternSynapse instantiateTemplate(I input, PatternNeuron output) {
        if(!input.getTemplates().contains(getInput())) {
            return null;
        }
        return new PatternSynapse(input, output, this);
    }

    public Activation getOutputActivationToLink(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron()) {
            return null;
        }

        return oAct;
    }

    @Override
    public void transition(Visitor v, Activation fromAct, Activation toAct, boolean create) {
        Visitor nv = v.prepareNextStep(toAct, ACT);
        nv.incrementPathLength();

        follow(null, toAct, nv, create);
    }

    @Override
    public byte getType() {
        return type;
    }
}
