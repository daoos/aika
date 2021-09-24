package network.aika.neuron.steps;

import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.linker.AbstractLinker;

import java.util.List;

public abstract class TaskStep<E extends Element, L extends AbstractLinker> extends Step<E> {
    protected final L task;

    protected final List<Direction> directions;


    public TaskStep(E element, L task, List<Direction> dirs) {
        super(element);
        this.task = task;
        this.directions = dirs;
    }

}
