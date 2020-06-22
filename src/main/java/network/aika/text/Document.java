package network.aika.text;

import network.aika.Config;
import network.aika.Model;
import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.*;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * @author Lukas Molzberger
 */
public class Document extends Thought {

    private final StringBuilder content;

    private Cursor cursor = new Cursor();

    public Document(String content) {
        this(content, null);
    }

    public Document(String content, Config trainingConfig) {
        super(trainingConfig);
        this.content = new StringBuilder(content);
    }

    public void add(Activation act) {
        super.add(act);

        TextModel tm = (TextModel) act.getNeuron().getModel();
        if(act.getNeuron() == tm.nextTokenInhib) {
            cursor.previousNextTokenAct = act;
        }
    }

    public void append(String txt) {
        content.append(txt);
    }

    public char charAt(int i) {
        return content.charAt(i);
    }

    public String getContent() {
        return content.toString();
    }

    public int length() {
        return content.length();
    }

    public String toString() {
        return content.toString();
    }

    private String getText(Integer begin, Integer end) {
        if(begin != null && end != null) {
            return content.substring(
                    Math.max(0, Math.min(begin, length())),
                    Math.max(0, Math.min(end, length()))
            );
        } else {
            return "";
        }
    }

    public static int[] getRange(Activation act) {
        return null;
    }

    public static String getText(Activation act) {
        int[] range = getRange(act);
        return ((Document)act.getThought()).getText(range[0], range[1]);
    }

    public Activation addInput(Neuron n, int begin, int end) {
        Activation act = new Activation(this, n);
        act.setReference(new GroundReference(begin, end));

        act.setValue(1.0);

        /*
        act.setFired(new Fired(input.inputTimestamp, input.fired));
        act.setRangeCoverage(input.rangeCoverage);

        input.getInputLinks()
                .stream()
                .map(iAct -> new Link(
                                getNeuron().getInputSynapse(iAct.getNeuronProvider()),
                                iAct,
                                this
                        )
                )
                .forEach(l -> addLink(l));
*/
        act.propagateInput();
        return act;
    }

    public Activation addInput(NeuronProvider n, int begin, int end) {
        return addInput(n.get(), begin, end);
    }

    public Activation processToken(TextModel m, String tokenLabel) {
        Neuron tokenN = m.getNeuron(tokenLabel).get();

        Activation tokenPatternAct = new Activation(this, tokenN);
        tokenPatternAct.setValue(1.0);
        tokenPatternAct.setFired(new Fired(0, 0)); // TODO
        tokenPatternAct.propagateInput();

        processActivations();

        return tokenPatternAct;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public static class GroundReference implements Reference {
        private int begin;
        private int end;

        public GroundReference(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        public int getBegin() {
            return begin;
        }

        public int getEnd() {
            return end;
        }
    }
}
