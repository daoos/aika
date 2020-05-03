package network.aika.neuron.activation.linker;

import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

public class LMatchingLink<S extends Synapse> extends LLink<S> {

    boolean dir;

    public LMatchingLink(LNode input, LNode output, PatternScope patternScope, Class<S> synapseClass, String label, boolean dir) {
        super(input, output, patternScope, synapseClass, label);

        this.dir = dir;
    }

    protected LNode getTo(LNode from) {
        if(from == input) {
            return output;
        }
        if(from == output) {
            return input;
        }
        return null;
    }

    protected Activation getToActivation(Link l, LNode from) {
        if(from == input) {
            return l.getOutput();
        }
        if(from == output) {
            return l.getInput();
        }
        return null;
    }

    public void follow(Mode m, Activation act, LNode from, Activation startAct) {
        Stream<Link> s = null;
        if(from == input) {
            if(!act.isFinal && act.lastRound != null) {
                act = act.lastRound;
            }
            s = act.outputLinks.values().stream();
        } else if(from == output) {
            s = act.inputLinks.values().stream();
        }

        s.forEach(l -> follow(m, l, from, startAct));
    }

    public void follow(Mode m, Link l, LNode from, Activation startAct) {
        LNode to = getTo(from);
        if(!checkLink(l)) {
            return;
        }

        to.follow(m, getToActivation(l, from), this, startAct);
    }

    private boolean checkLink(Link l) {
        if(patternScope != null && patternScope != l.getSynapse().getPatternScope()) {
            return false;
        }

        return true;
    }

    @Override
    public String getTypeStr() {
        return "M";
    }
}
