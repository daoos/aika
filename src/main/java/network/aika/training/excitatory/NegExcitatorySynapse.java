package network.aika.training.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.search.Option;
import network.aika.training.MetaModel;
import network.aika.training.Sign;
import network.aika.training.TNeuron;


public class NegExcitatorySynapse extends ExcitatorySynapse {


    public NegExcitatorySynapse(Neuron input, Neuron output, Integer id) {
        super(input, output, id);
    }

    public NegExcitatorySynapse(Neuron input, Neuron output, Integer id, int lastCount) {
        super(input, output, id, lastCount);
    }


    public void updateCountValue(Option io, Option oo) {
        double inputValue = io != null ? io.getState().value : 0.0;
        double outputValue = oo != null ? oo.getState().value : 0.0;

        if(!needsCountUpdate) {
            return;
        }
        needsCountUpdate = false;

        double optionProp = (io != null ? io.getP() : 1.0) * (oo != null ? oo.getP() : 1.0);

        if(TNeuron.checkSelfReferencing(oo, io)) {
            countValueIPosOPos += (Sign.POS.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        } else {
            countValueINegOPos += (Sign.NEG.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        }
        countValueIPosONeg += (Sign.POS.getX(inputValue) * Sign.NEG.getX(outputValue) * optionProp);
        countValueINegONeg += (Sign.NEG.getX(inputValue) * Sign.NEG.getX(outputValue) * optionProp);

        needsFrequencyUpdate = true;
    }


    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            NegExcitatorySynapse s = (NegExcitatorySynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new NegExcitatorySynapse(input, output, id, ((MetaModel) output.getModel()).charCounter);
        }
    }
}
