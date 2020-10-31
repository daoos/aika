package network.aika.neuron.inhibitory;


import network.aika.neuron.Neuron;

public class PrimaryInhibitorySynapse extends InhibitorySynapse {


    public PrimaryInhibitorySynapse() {
        super();
    }

    public PrimaryInhibitorySynapse(Neuron<?> input, InhibitoryNeuron output) {
        super(input, output);
    }

    @Override
    public boolean inductionRequired(Class<? extends Neuron> type) {
        return type == InhibitoryNeuron.class;
    }
}
