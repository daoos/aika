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
package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.steps.Step;

import static network.aika.neuron.steps.activation.TemplatePropagate.TemplatePropagateInput;
import static network.aika.neuron.steps.activation.TemplatePropagate.TemplateGradientPropagateOutput;
import static network.aika.neuron.steps.activation.TemplateCloseLoop.TemplateCloseLoopInput;
import static network.aika.neuron.steps.activation.TemplateCloseLoop.TemplateCloseLoopOutput;


/**
 *
 * @author Lukas Molzberger
 */
public interface ActivationStep extends Step<Activation> {
    ActivationStep INDUCTION = new Induction();
    Linking LINKING = new Linking();
    ActivationStep PROPAGATE = new Propagate();
    ActivationStep CHECK_IF_FIRED = new CheckIfFired();
    ActivationStep USE_FINAL_BIAS = new UseFinalBias();
    ActivationStep DETERMINE_BRANCH_PROBABILITY = new BranchProbability();
    ActivationStep ENTROPY_GRADIENT = new EntropyGradient();
    ActivationStep PROPAGATE_GRADIENTS_SUM = new PropagateGradientsSum();
    ActivationStep PROPAGATE_GRADIENTS_NET = new PropagateGradientsNet();
    ActivationStep UPDATE_SYNAPSE_INPUT_LINKS = new UpdateSynapseInputLinks();
    ActivationStep TEMPLATE_PROPAGATE_INPUT = new TemplatePropagateInput();
    TemplateCloseLoop TEMPLATE_CLOSE_LOOP_INPUT = new TemplateCloseLoopInput();
    TemplateCloseLoop TEMPLATE_CLOSE_LOOP_OUTPUT = new TemplateCloseLoopOutput();
    ActivationStep TEMPLATE_PROPAGATE_OUTPUT = new TemplateGradientPropagateOutput();
    ActivationStep COUNTING = new Counting();
}
