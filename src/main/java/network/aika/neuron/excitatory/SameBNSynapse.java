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
package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.direction.Direction;

/**
 *
 * @author Lukas Molzberger
 */
public class SameBNSynapse<I extends Neuron> extends BindingNeuronSynapse<I> {

    @Override
    public Byte transitionScope(Byte fromScope, Direction dir) {
        if(fromScope == 0)
            return fromScope;

        return dir.transitionScope(fromScope);
    }

    public boolean checkScope(Byte fromScope, Byte toScope, Direction dir) {
        if(fromScope == 0 && toScope == 0)
            return true;

        if(dir.getInputScope(fromScope, toScope) == 1 && dir.getOutputScope(fromScope, toScope) == 2)
            return true;

        return false;
    }
}
