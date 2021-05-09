package network.aika;

import network.aika.debugger.AikaDebugger;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.activation.ActivationStep;
import network.aika.neuron.steps.link.LinkStep;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TextReference;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.platform.commons.util.StringUtils.isBlank;

public class PhraseTraining {


    private String trimPrefix(String l) {
        return l.substring(l.indexOf("-") + 1);
    }

    @Test
    public void trainPhrases() throws IOException {
        TextModel m = new TextModel();
        m.setConfig(
                new Config() {
                    public String getLabel(Activation act) {
                        Neuron n = act.getNeuron();
                        Activation iAct = act.getInputLinks()
                                .findFirst()
                                .map(l -> l.getInput())
                                .orElse(null);

                        if (n instanceof BindingNeuron) {
                            return "PP-" + trimPrefix(iAct.getLabel());
                        } else if (n instanceof PatternNeuron) {
                            return "P-" + ((Document) act.getThought()).getContent();
                        } else {
                            return "I-" + trimPrefix(iAct.getLabel());
                        }
                    }
                }
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
                        .setSurprisalInductionThreshold(0.0)
                        .setGradientInductionThreshold(0.0)
        );

        //    m.setN(912);

        for (int round = 0; round < 2; round++) {
            process(m, round);
        }
    }


    private void process(TextModel m, int round) throws IOException {
        Step[] countingOnlyFilters = new Step[]{
                ActivationStep.TEMPLATE_CLOSE_CYCLE_OUTPUT,
                ActivationStep.TEMPLATE_PROPAGATE_OUTPUT,
                ActivationStep.TEMPLATE_PROPAGATE_INPUT,
                ActivationStep.ENTROPY_GRADIENT,
                ActivationStep.PROPAGATE_GRADIENTS_NET,
                ActivationStep.PROPAGATE_GRADIENTS_SUM,
                ActivationStep.INDUCTION,
                LinkStep.TEMPLATE,
                LinkStep.INDUCTION,
                LinkStep.INFORMATION_GAIN_GRADIENT
        };
        Util.loadExamplePhrases("phrases.txt")
                .stream()
                .filter(p -> !isBlank(p))
                .forEach(p -> {
                            System.out.println(p);
                            Document doc = new Document(p);

                            int i = 0;
                            TextReference lastRef = null;
                            for (String t : doc.getContent().split(" ")) {
                                if (!isBlank(t)) {
                                    int j = i + t.length();
                                    lastRef = doc.processToken(m, lastRef, i, j, t).getReference();

                                    i = j + 1;
                                }
                            }

                            if (round == 0) {
                                doc.addFilters(countingOnlyFilters);
                            } else {
                                AikaDebugger.createAndShowGUI(doc, m);
                            }

                            doc.process(m);
                        }
                );
    }

}