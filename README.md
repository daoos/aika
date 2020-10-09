# About the Aika Neural Network

Aika is a new type of artificial neural network designed to more closely mimic the behavior of a biological brain and to bridge the gap to classical AI. A key design decision in the Aika network is to conceptually separate the activations from their neurons, meaning that there are two separate graphs. One graph consisting of neurons and synapses representing the knowledge the network has already acquired and another graph consisting of activations and links describing the information the network was able to infer about a concrete input data set. There is a one-to-many relation between the neurons and the activations. For example, there might be a neuron representing a word or a specific meaning of a word, but there might be several activations of this neuron, each representing an occurrence of this word within the input data set. A consequence of this decision is that we have to give up on the idea of a fixed layered topology for the network, since the sequence in which the activations are fired depends on the input data set. Within the activation network, each activation is grounded within the input data set, even if there are several activations in between. This means links between activations serve two purposes. On the one hand, they are used to sum up the synapse weights and, on the other hand they propagate the identity to higher level activations.

A good starting point to get familiar with this project are probably the following three test cases:
- PatternTest (Demonstrates how a pattern like a word or a phrase can be matched)
- MutualExclusionTest (Demonstrates a negative feed back loop)
- InductionTest (Demonstrates how new neurons are induced)
