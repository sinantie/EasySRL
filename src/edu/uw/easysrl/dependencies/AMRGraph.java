package edu.uw.easysrl.dependencies;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AMRGraph implements Serializable {

    private final Multimap<AMRNode, AMREdge> incidenceList = ArrayListMultimap.create();

    /**
     *
     * Recursively visit a sub-graph by traversing the incidence list in Depth
     * First Search (DFS) order, starting from the <code>AMRNode node</code>.
     *
     * @param node
     * @param depth
     * @param result
     */
    public void visit(final AMRNode node, final int depth, final StringBuilder result) {
        boolean reEntrance = node.isVisited();
        result.append(node.isVisited() ? node.getVarName() : String.format("(%s", node));
        if (!node.isVisited()) {// don't re-visit children of re-entrancies                    
            node.setVisited(true);
            Collection<AMREdge> edges = incidenceList.get(node);
            for (AMREdge edge : edges) {
                result.append("\n").append(tabs(depth)); // adjust depth
//                result.append(tabs(depth)); // adjust depth
//                result.append(" "); // adjust depth
                result.append(edge).append(" "); // print label
                visit(edge.getTarget(), depth + 1, result);
            }
        }
        if (!reEntrance) // re-entrance variables don't like to be in parentheses
        {
            result.append(")");
        }
    }

    private StringBuilder tabs(int depth) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            str.append("\t");
        }
        return str;
    }

    public List<EasyProposition> getPropositions(final AMRNode node) {
        List<EasyProposition> props = new ArrayList<>();
        Collection<AMREdge> edges = incidenceList.get(node);
        edges.stream().forEach((edge) -> {
            props.add(new EasyProposition(node, edge.getTarget(), edge.getLabel()));
        });
        return props;
    }

    public void addLabelledEdge(final AMRNode fromNode, final AMRNode toNode, final String label) {
        incidenceList.put(fromNode, new AMREdge(label, toNode));
    }

    public void removeEdge(final AMRNode fromNode, final AMRNode toNode) {
        Collection<AMREdge> edges = incidenceList.get(fromNode);
        Iterator<AMREdge> it = edges.iterator();
        while (it.hasNext()) { // remove adjacent edge
            if (it.next().getTarget().equals(toNode)) {
                it.remove();
            }
        }
        if (edges.isEmpty()) { // if this was the only edge, then delete fromNode from the graph
            incidenceList.removeAll(fromNode);
        }
    }

    public void removeEdges(final AMRNode toNode) {
        Collection<AMREdge> edges = incidenceList.values();
        Iterator<AMREdge> it = edges.iterator();
        while (it.hasNext()) { // remove adjacent edge
            if (it.next().getTarget().equals(toNode)) {
                it.remove();
            }
        }
    }

    public Collection<AMREdge> removeAll(AMRNode parent) {
        return incidenceList.removeAll(parent);
    }

    public AMRNode getFirstParent(AMRNode node) {
        for (Map.Entry<AMRNode, AMREdge> entry : incidenceList.entries()) {
            if (entry.getValue().getTarget().equals(node)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Collection<AMREdge> get(AMRNode parent) {
        return incidenceList.get(parent);
    }

    public String getEdgeLabel(AMRNode parent, AMRNode node) {
        for (AMREdge edge : incidenceList.get(parent)) {
            if (edge.getTarget().equals(node)) {
                return edge.getLabel();
            }
        }
        return null;
    }

    public boolean containsEdge(final AMRNode toNode) {
        if (incidenceList.values().stream().anyMatch((edge) -> (edge.getTarget().equals(toNode)))) {
            return true;
        }
        return false;
    }

    /**
     *
     * Check whether graph contains <code>toNode</code> as a source node, or as
     * a target node in an edge, other than the one headed by
     * <code>fromNode</code>.
     *
     * @param fromNode
     * @param toNode
     * @return
     */
    public boolean containsEdgeExcluding(final AMRNode fromNode, final AMRNode toNode) {
        if (incidenceList.containsKey(toNode)) {
            return true;
        }
        for (AMRNode cand : incidenceList.keySet()) {
            if (!cand.equals(fromNode)) {
                if (incidenceList.get(cand).stream().anyMatch((edge) -> (edge.getTarget().equals(toNode)))) {
                    return true;
                }
            }
        }
        return false;
    }

    public class EasyProposition {

        AMRNode predicate, argument;
        String role;

        public EasyProposition(AMRNode predicate, AMRNode argument, String role) {
            this.predicate = predicate;
            this.argument = argument;
            this.role = role;
        }

        public AMRNode getPredicate() {
            return predicate;
        }

        public AMRNode getArgument() {
            return argument;
        }

        @Override
        public String toString() {
            return String.format("%s,%s,%s", predicate.getConceptName(), argument.getConceptName(), role);
        }

        @Override
        public boolean equals(Object obj) {
            assert obj instanceof EasyProposition;
            EasyProposition e = (EasyProposition) obj;
            return predicate.getConceptName().equals(e.getPredicate().getConceptName())
                    && argument.getConceptName().equals(e.getArgument().getConceptName())
                    && role.equals(e.role);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.predicate.getConceptName());
            hash = 37 * hash + Objects.hashCode(this.argument.getConceptName());
            hash = 37 * hash + Objects.hashCode(this.role);
            return hash;
        }

    }
}
