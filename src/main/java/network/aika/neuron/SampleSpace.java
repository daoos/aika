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
import network.aika.Thought;
import network.aika.neuron.activation.Activation;
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

    public static final int RANGE_BEGIN = 0;
    public static final int RANGE_END = 1;

    private static final Logger log = LoggerFactory.getLogger(SampleSpace.class);

    private double N = 0;
    private Long offset;

    public SampleSpace() {
    }

    public double getN(int[] range) {
        return Math.max(N + getInactiveInstancesSinceLastPos(range), 0);
    }

    public void setN(int N) {
        this.N = N;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public void applyMovingAverage(double alpha) {
        N *= alpha;
    }

    public void countSkippedInstances(int[] range) {
        N += getInactiveInstancesSinceLastPos(range);
    }

    public void count() {
        N += 1;
    }

    public void initOffset(Thought t, int[] range) {
        if(offset != null)
            return;

        offset = t.getAbsoluteBegin() + range[RANGE_END];
    }

    public long getInactiveInstancesSinceLastPos(int[] range) {
        if(range == null || offset == null)
            return 0;

        return (range[RANGE_END] - offset) / (range[RANGE_END] - range[RANGE_BEGIN]);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(N);
        out.writeBoolean(offset != null);
        if(offset != null)
            out.writeLong(offset);
    }

    public static SampleSpace read(DataInput in, Model m) throws IOException {
        SampleSpace sampleSpace = new SampleSpace();
        sampleSpace.readFields(in, m);
        return sampleSpace;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        N = in.readDouble();
        if(in.readBoolean())
            offset = in.readLong();
    }

    public String toString() {
        return "N:" + N + " offset:" + offset;
    }
}
