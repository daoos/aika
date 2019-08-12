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
package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public class MultiRelation extends Relation {
    public static final int ID = 1;

    private SortedSet<Relation> relations;

    static {
        registerRelation(ID, () -> new MultiRelation());
    }


    public MultiRelation() {
        relations = new TreeSet<>();
    }


    public MultiRelation(Relation... rels) {
        relations = new TreeSet<>(Arrays.asList(rels));
    }


    public MultiRelation(SortedSet<Relation> rels) {
        relations = rels;
    }


    public SortedSet<Relation> getRelations() {
        return relations;
    }


    public int size() {
        return relations.size();
    }


    public void addRelation(Relation r) {
        for(Relation rel: relations) {
            if(rel.compareTo(r) == 0) {
                return;
            }
        }
        relations.add(r);
    }


    public void removeRelation(Relation r) {
        relations.removeIf(rel -> rel.compareTo(r) == 0);
    }


    @Override
    public int getType() {
        return ID;
    }


    @Override
    public boolean test(Activation act, Activation linkedAct, boolean allowUndefined) {
        for (Relation rel : relations) {
            if (!rel.test(act, linkedAct, allowUndefined)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public Relation invert() {
        SortedSet<Relation> invRels = new TreeSet<>();
        for(Relation rel: relations) {
            invRels.add(rel.invert());
        }
        return new MultiRelation(invRels);
    }


    @Override
    public void mapSlots(Map<Integer, Position> slots, Activation act) {
        for(Relation rel: relations) {
            rel.mapSlots(slots, act);
        }
    }


    @Override
    public boolean isExact() {
        for(Relation rel: relations) {
            if(rel.isExact()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
        if(relations.isEmpty()) {
            return n.getActivations(linkedAct.getDocument());
        } else {
            return relations.first()
                    .getActivations(n, linkedAct)
                    .filter(act -> {
                        for (Relation rel : relations) {
                            if (!rel.test(act, linkedAct, false)) {
                                return false;
                            }
                        }
                        return true;
                    });
        }
    }


    @Override
    public int compareTo(Relation rel) {
        int r = super.compareTo(rel);
        if(r != 0) return r;

        MultiRelation mr = (MultiRelation) rel;
        r = Integer.compare(relations.size(), mr.relations.size());
        if(r != 0) return r;

        Iterator<Relation> ita = relations.iterator();
        Iterator<Relation> itb = mr.relations.iterator();

        while(ita.hasNext() || itb.hasNext()) {
            Relation a = ita.next();
            Relation b = itb.next();
            r = a.compareTo(b);
            if(r != 0) return r;
        }
        return 0;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(relations.size());
        for(Relation rel: relations) {
            rel.write(out);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);
        int l = in.readInt();
        for(int i = 0; i < l; i++) {
            relations.add(Relation.read(in, m));
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MULTI(");
        boolean first = true;
        for(Relation rel: relations) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(rel.toString());
        }
        sb.append(")");
        return sb.toString();
    }
}
