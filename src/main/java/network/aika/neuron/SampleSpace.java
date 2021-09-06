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
package network.aika.neuron;

import network.aika.Model;
import network.aika.neuron.activation.Reference;
import network.aika.utils.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The <a href="https://en.wikipedia.org/wiki/Sample_space}">Sample Space</a> keeps track of the number of
 * training instances a certain neuron or synapse has encountered. The Sample Space is used
 * to convert the counted frequencies to probabilities.
 *
 * @author Lukas Molzberger
 */
public class SampleSpace implements Writable {

    private static final Logger log = LoggerFactory.getLogger(SampleSpace.class);

    private final Model m;
    private double N = 0;
    private Long lastPos;

    public SampleSpace(Model m) {
        this.m = m;
    }

    public double getN(Reference ref) {
        return N + getNegativeInstancesSinceLastPos(ref);
    }

    public void setN(int N) {
        this.N = N;
    }

    public Long getLastPos() {
        return lastPos;
    }

    public void setLastPos(Long lastPos) {
        this.lastPos = lastPos;
    }

    public void applyMovingAverage(double alpha) {
        N *= alpha;
    }

    public void countSkippedInstances(Reference ref) {
        N += getNegativeInstancesSinceLastPos(ref);

        Long newPos = getAbsoluteEnd(m, ref);
   //     assert lastPos == null || newPos > lastPos;

        lastPos = newPos;
    }

    public void count() {
        N += 1;
    }

    public long getNegativeInstancesSinceLastPos(Reference ref) {
        if(ref == null)
            return 0;

        long n = 0;

        if(lastPos != null)
            n = getAbsoluteBegin(m, ref) - lastPos;

        if(n < 0) {
            log.warn("getNegativeInstancesSinceLastPos is not allowed to be called after update.");
            return 0;
        }

        n /= ref.length();
        return n;
    }

    public long getAbsoluteBegin(Model m, Reference ref) {
        return m.getN() + (ref != null ? ref.getBegin() : 0);
    }

    public long getAbsoluteEnd(Model m, Reference ref) {
        return m.getN() + (ref != null ? ref.getEnd() : 0);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(N);
        out.writeBoolean(lastPos != null);
        if(lastPos != null)
            out.writeLong(lastPos);
    }

    public static SampleSpace read(DataInput in, Model m) throws IOException {
        SampleSpace sampleSpace = new SampleSpace(m);
        sampleSpace.readFields(in, m);
        return sampleSpace;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        N = in.readDouble();
        if(in.readBoolean())
            lastPos = in.readLong();
    }

    public String toString() {
        return "N:" + N + " lastPos:" + lastPos;
    }
}
