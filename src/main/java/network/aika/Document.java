package network.aika;

import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.Activation;

/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * @author Lukas Molzberger
 */
public class Document extends Thought {

    private final StringBuilder content;

    public Document(String content) {
        super();
        this.content = new StringBuilder(content);
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
        return ""; // TODO!
    }

    public Activation addInput(Neuron n, int begin, int end) {
        return null;
    }

    public Activation addInput(NeuronProvider n, int begin, int end) {
        return null;
    }
}
