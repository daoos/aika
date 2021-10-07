package network.aika.neuron.steps;

import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.InhibitoryActivation;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.linker.AbstractLinker;

import java.util.List;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public abstract class LinkerStep<E extends Element, T extends AbstractLinker> extends TaskStep<E, T> {

    protected final PatternActivation bindingSignal;
    protected final Byte scope;

    public LinkerStep(E element, PatternActivation bindingSignal, Byte scope, T task) {
        super(element, task);
        this.bindingSignal = bindingSignal;
        this.scope = scope;
    }

    protected List<Direction> getDirections() {
        if(getElement() instanceof InhibitoryActivation)
            return List.of(OUTPUT);

        return List.of(INPUT, OUTPUT);
    }
}
