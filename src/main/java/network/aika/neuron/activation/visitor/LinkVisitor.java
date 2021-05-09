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
package network.aika.neuron.activation.visitor;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.scopes.Scope;
import network.aika.neuron.activation.scopes.Transition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;


/**
 *
 * @author Lukas Molzberger
 */
public class LinkVisitor extends Visitor {

    Link link;
    Collection<Transition> transitions;

    public ActVisitor prepareNextStep(Activation act) {
        ActVisitor nv = new ActVisitor();
        prepareNextStep(nv);
        nv.act = act;
        nv.scopes = new ArrayList<>();
        nv.visitedScopes = new TreeSet<>(visitedScopes);

        getTransitions()
                .forEach(t -> {
                    Scope fromScope = downUpDir.getFromScope(t.getTemplate());
                    Scope toScope = downUpDir.getToScope(t.getTemplate());
                    if(fromScope == toScope || !visitedScopes.contains(toScope))
                        nv.scopes.add(toScope.getInstance(downUpDir, t));
                });

        if(nv.scopes.isEmpty())
            return null;

        return nv;
    }

    public boolean isClosedCycle() {
        return transitions.stream()
                .anyMatch(t ->
                        origin.scopes.contains(t.getInput().getOrigin())
                );
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public Link getLink() {
        return link;
    }

    public Collection<Transition> getTransitions() {
        return transitions;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Origin:" + origin.act.toShortString() + ", ");
        sb.append("Current:" + link.toString() + ", ");

        sb.append("Transitions:" + transitions + ", ");

        sb.append(super.toString());

        return sb.toString();
    }
}