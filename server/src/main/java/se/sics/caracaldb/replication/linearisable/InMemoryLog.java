/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.caracaldb.replication.linearisable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import org.javatuples.Pair;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.replication.linearisable.ExecutionEngine.SMROp;
import se.sics.caracaldb.replication.log.Value;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
public class InMemoryLog implements OperationsLog {

    // snapshot ready part of the log
    private TreeMap<Long, Value> log = new TreeMap<Long, Value>();
    // part of the log with gaps
    private TreeMap<Long, Value> gapLog = new TreeMap<Long, Value>();

    @Override
    public List<Pair<Long, Value>> insert(long pos, Value op) {
        List<Pair<Long, Value>> ops = new LinkedList<Pair<Long, Value>>();
        if (log.isEmpty() || (pos == (log.lastKey() + 1))) {
            log.put(pos, op);
            ops.add(Pair.with(pos, op));
            if (!gapLog.isEmpty() && (pos == (gapLog.firstKey() - 1))) {
                long lastRead = gapLog.firstKey() - 1;
                while (gapLog.firstKey() == lastRead + 1) {
                    Entry<Long, Value> e = log.pollFirstEntry();
                    Long p = e.getKey();
                    Value v = e.getValue();
                    log.put(p, v);
                    ops.add(Pair.with(p, v));
                    lastRead++;
                }
            }
        } else {
            gapLog.put(pos, op);
        }
        return ops;
    }

    @Override
    public void prune(long pos) {
        log = new TreeMap<Long, Value>(log.tailMap(pos, false));
    }

    @Override
    public void clear() {
        log.clear();
        gapLog.clear();
    }

    @Override
    public boolean isEmpty() {
        return log.isEmpty() && gapLog.isEmpty();
    }

    @Override
    public List<CaracalOp> getApplicableForOp(long pos, CaracalOp op, long lastSnapshot) {
        SortedMap<Long, Value> newMap = log.tailMap(lastSnapshot, false).headMap(pos);
        List<CaracalOp> ops = new LinkedList<CaracalOp>();
        for (Value v : newMap.values()) {
            if (v instanceof SMROp) {
                CaracalOp otherOp = ((SMROp) v).op;
                if (op.affectedBy(otherOp)) {
                    ops.add(otherOp);
                }
            }
        }
        return ops;
    }

    @Override
    public Pair<Long, List<CaracalOp>> getSnapshotDiff(long lastSnapshot) {
        List<CaracalOp> ops = new LinkedList<CaracalOp>();
        for (Value v : log.tailMap(lastSnapshot, false).values()) {
            if (v instanceof SMROp) {
                CaracalOp op = ((SMROp) v).op;
                ops.add(op);
            }
        }
        return Pair.with(log.lastKey(), ops);
    }

    @Override
    public Iterator<Value> iterator() {
        List<Value> ops = new LinkedList<Value>(log.values());
        ops.addAll(gapLog.values());
        return ops.iterator();
    }
}
