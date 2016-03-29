package edu.uw.easysrl.dependencies;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import edu.uw.easysrl.dependencies.AMRGraph.EasyProposition;
import edu.uw.easysrl.lemmatizer.MorphaStemmer;
import edu.uw.easysrl.syntax.grammar.Category;
import edu.uw.easysrl.syntax.grammar.SyntaxTreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author ikonstas
 */
public class ConvertSRLToAMRGraph {

    private final SyntaxTreeNode parse;
    private final AMRLexicon lexicon;
    private final Set<AMRNode> rootNodes;
    private final AMRGraph graph;

    public ConvertSRLToAMRGraph(SyntaxTreeNode parse, AMRLexicon lexicon) {
        this.parse = parse;
        this.lexicon = lexicon;
        this.rootNodes = new HashSet<>();
        this.graph = new AMRGraph();
        convertSrl(null);
    }

    public ConvertSRLToAMRGraph(SyntaxTreeNode parse, Collection<UnlabelledDependency> dependencies, AMRLexicon lexicon) {
        this.parse = parse;
        this.lexicon = lexicon;
        this.rootNodes = new HashSet<>();
        this.graph = new AMRGraph();
        convertUnlabelled(dependencies);
    }

    public ConvertSRLToAMRGraph(SyntaxTreeNode parse, List<ResolvedDependency> dependencies, AMRLexicon lexicon) {
        this.parse = parse;
        this.lexicon = lexicon;
        this.rootNodes = new HashSet<>();
        this.graph = new AMRGraph();
        convertSrl(dependencies);
    }

    private void convertSrl(List<ResolvedDependency> dependencies) {
        Map<Integer, AMRNode> leafIdToNode = new HashMap<>();
        Multiset<Character> varCounter = HashMultiset.create();
        Set<AMRNode> toNodes = new HashSet<>();
        Set<AMRNode> copulaNodes = new HashSet<>();
        Map<Integer, ParentAMRNodeLabel> prepNodeIdToParentMap = new HashMap<>();
        List<ResolvedDependency> list = dependencies == null ? parse.getAllLabelledDependencies() : dependencies;
        for (final ResolvedDependency dep : list) {            
            if (showDependency(dep, parse)) {
                System.out.println(dependencyToString(parse, dep));
                String label;
                final int from, to;
                if (dep.getSemanticRole() != SRLFrame.NONE) {
                    // Draw adjuncts PropBank-style, from the predicate to the modifier.
                    label = lexicon.getAmrLabel(dep.getSemanticRole().toString());
                    from = dep.getPropbankPredicateIndex();
                    to = dep.getPropbankArgumentIndex();
                } else // Draw in some extra CCG dependencies for nouns and adjectives.						
                // Invert arc direction (used to be from=pred, to=arg)
                {
                    if (isCategory(parse, dep.getHead(), Category.ADJECTIVE)
                            || startsWithPos(parse, dep.getHead(), "JJ")) {
                        from = dep.getArgumentIndex();
                        to = dep.getHead();
                        label = ":mod";
                    } else {
                        from = dep.getHead();
                        to = dep.getArgumentIndex();
                        label = ":unk";
                    }
                }
                // take care of preposition nodes: if they already appeared elsewhere, then append their child to their parent      
                ParentAMRNodeLabel fromNodeLabel = prepNodeIdToParentMap.get(from);
                AMRNode fromNode;
                if (fromNodeLabel == null) {
                    fromNode = getNode(parse, leafIdToNode, varCounter, from);
                } else {
                    fromNode = fromNodeLabel.parentNode;
                    label = fromNodeLabel.label;
                }
                if (isCopulaVerb(fromNode)) {
                    copulaNodes.add(fromNode);
                }
                rootNodes.add(fromNode);
                // particle arguments: don't add separate edge, but append them to predicate node name
                if (isCategory(parse, to, Category.PR)) {
                    fromNode.setConceptName(String.format("%s-%s", fromNode.getConceptName(), parse.getLeaves().get(to).getWord()));
                } else if (startsWithPos(parse, to, "IN")) { // make note of preposition node and its' parent, but don't add to incidence list
                    prepNodeIdToParentMap.put(to, new ParentAMRNodeLabel(fromNode, label));
                } else {
                    AMRNode toNode = getNode(parse, leafIdToNode, varCounter, to);
                    toNodes.add(toNode);
                    if (isCopulaVerb(fromNode)) {
                        copulaNodes.add(fromNode);
                    }
                    graph.addLabelledEdge(fromNode, toNode, label);
                    //result.append(String.format("<%s, %s, %s>\n", fromNode.getConceptName(), toNode.getConceptName(), label));
                }
            } // if
        } // for
        rootNodes.removeAll(toNodes); // root nodes are only the ones that have no inner edge pointing at them.            
        // tackle predicate adjectives ('noun is adj') and 'noun is noun' cases, by introducing the :domain edge
        processCopulaNodes(copulaNodes, rootNodes, toNodes, graph);
        processPrepositionNodes(rootNodes, graph);
//        assert !rootNodes.isEmpty() : "No root(s) node(s) found";
    }

    private void convertUnlabelled(Collection<UnlabelledDependency> dependencies) {
        Map<Integer, AMRNode> leafIdToNode = new HashMap<>();
        Multiset<Character> varCounter = HashMultiset.create();
        Set<AMRNode> toNodes = new HashSet<>();
        Set<AMRNode> copulaNodes = new HashSet<>();
        Map<Integer, ParentAMRNodeLabel> prepNodeIdToParentMap = new HashMap<>();
        for (final UnlabelledDependency dep : dependencies) {            
            if (showDependency(dep, parse)) {
                System.out.println(dependencyToString(parse, dep));
                String label;
                final int from, to;
//                if (startsWithPos(parse, dep.getHead(), "VB")) {
//                    // Draw adjuncts PropBank-style, from the predicate to the modifier.
//                    label = ":AVNUM" + dep.getArgNumber();
//                    from = dep.getHead();
//                    to = dep.getFirstArgumentIndex();
//                } else if (startsWithPos(parse, dep.getFirstArgumentIndex(), "VB")) {
//                    // Draw adjuncts PropBank-style, from the predicate to the modifier.
//                    label = ":AVNUM" + dep.getArgNumber();                    
//                    from = dep.getFirstArgumentIndex();
//                    to = dep.getHead();
//                } else // Draw in some extra CCG dependencies for nouns and adjectives.
                       // Invert arc direction (used to be from=pred, to=arg)
//                {
                if (isCategory(parse, dep.getHead(), Category.ADJECTIVE)
                        || startsWithPos(parse, dep.getHead(), "JJ")) {
                    from = dep.getFirstArgumentIndex();
                    to = dep.getHead();
                    label = ":mod";
//                    } else if (isCategory(parse, dep.getFirstArgumentIndex(), Category.PREPOSITION) || 
//                            startsWithPos(parse, dep.getFirstArgumentIndex(), "IN")) {
//                        from = dep.getFirstArgumentIndex();
//                        to = dep.getHead();
//                        label = ":APREPNUM" + dep.getFirstArgumentIndex();
                } else if(dep.getCategory().isFunctionIntoModifier()) {
                    from = dep.getFirstArgumentIndex();
                    to = dep.getHead();
                    label = String.format(":%s_%s", getWord(parse, to), dep.getCategory());
                } else {
                    from = dep.getHead();
                    to = dep.getFirstArgumentIndex();
//                        label = ":unk";
                    label = ":ANUM" + dep.getArgNumber();
                }
//                }
                // take care of preposition nodes: if they already appeared elsewhere, then append their child to their parent      
                ParentAMRNodeLabel fromNodeLabel = prepNodeIdToParentMap.get(from);
                AMRNode fromNode;
                if (fromNodeLabel == null) {
                    fromNode = getNode(parse, leafIdToNode, varCounter, from);
                } else {
                    fromNode = fromNodeLabel.parentNode;
                    label = fromNodeLabel.label;
                }
                if (isCopulaVerb(fromNode)) {
                    copulaNodes.add(fromNode);
                }
                rootNodes.add(fromNode);
                // particle arguments: don't add separate edge, but append them to predicate node name
                if (isCategory(parse, to, Category.PR)) {
                    fromNode.setConceptName(String.format("%s-%s", fromNode.getConceptName(), parse.getLeaves().get(to).getWord()));
//                } else if (startsWithPos(parse, to, "IN")) { // make note of preposition node and its' parent, but don't add to incidence list
//                    prepNodeIdToParentMap.put(to, new ParentAMRNodeLabel(fromNode, label));
                } else {
                    AMRNode toNode = getNode(parse, leafIdToNode, varCounter, to);
                    toNodes.add(toNode);
                    if (isCopulaVerb(fromNode)) {
                        copulaNodes.add(fromNode);
                    }
                    graph.addLabelledEdge(fromNode, toNode, label);
                    //result.append(String.format("<%s, %s, %s>\n", fromNode.getConceptName(), toNode.getConceptName(), label));
                }
            } // if
        } // for
        rootNodes.removeAll(toNodes); // root nodes are only the ones that have no inner edge pointing at them.            
        // tackle predicate adjectives ('noun is adj') and 'noun is noun' cases, by introducing the :domain edge
//        processCopulaNodes(copulaNodes, rootNodes, toNodes, graph);
//        processPrepositionNodes(rootNodes, graph);
//        assert !rootNodes.isEmpty() : "No root(s) node(s) found";
    }

    public void printAmrGraph(final StringBuilder result) {
        assert !rootNodes.isEmpty() : "No root(s) node(s) found";
        // visit (potentially more than one) root nodes in DFS order and print AMR-like output.
        // Note that AMR does not allow disconnected graphs, so this is a close proxy.
//                        result.append(sentenceNumber).append("\t");
        rootNodes.stream().forEach((root) -> {
            graph.visit(root, 1, result);
            result.append("\n");
        });
    }

    public void unvisit() {
        rootNodes.stream().forEach(graph::unvisit);
    }
    
    public void printAmrPropositions(final StringBuilder result) {

        getPropositions().stream().forEach((p) -> result.append(p).append("\n"));
    }

    public List<EasyProposition> getPropositions() {
        unvisit();
        final List<EasyProposition> props = new ArrayList<>();
        rootNodes.stream().forEach(root -> graph.getPropositions(root, props));
        return props;
    }

    /**
     *
     * Identify cases of predicate adjectives (noun is adj) and 'noun is noun'.
     * The (90% accurate) algorithm is as follows:
     * <p/>
     * For every node <code>n</code> in the <code>copulaNodes</code> set<br />
     * IF <code>n</code> has exactly 2 children AND<br/>
     * (let <code>firstChild</code> be the child node that is to the left of its
     * sibling <code>rightChild</code>)<br/>
     * the POS tag of <code>firstChild</code> is NN*|PRP AND<br/>
     * the POS tag of <code>secondChild</code> is NN*|JJ*<br/>
     * THEN add the edge <code>secondChild</code> -> (<code>firstChild</code>,
     * :domain) and<br />
     * remove <code>n</code> and its children from the
     * <code>incidenceList</code>, <code>rootNodes</code> and
     * <code>toNodes</code>.<br/>
     * ALSO remove any <code>firstChild</code> -> (<code>secondChild</code>, *)
     * edge from <code>incidenceList</code> ELSE IF <code>n</code> has exactly 1
     * child<br/>
     * THEN discard <code>n</code> and its child from the
     * <code>incidenceList</code><br/>
     * FINALLY, go through the <code>toNodes</code> set and remove any left-over
     * copula verbs that don't have any children (otherwise they would have been
     * tackled above).
     *
     * @param copulaNodes
     * @param rootNodes
     * @param graph
     */
    private void processCopulaNodes(Set<AMRNode> copulaNodes, Set<AMRNode> rootNodes, Set<AMRNode> toNodes, AMRGraph graph) {
        for (AMRNode node : copulaNodes) {
            Collection<AMREdge> children = graph.get(node);
            if (children != null && children.size() == 2) {
                AMREdge ar[] = children.toArray(new AMREdge[0]);
                boolean inOrder = ar[0].getTarget().isBefore(ar[1].getTarget());
                AMRNode firstChild = inOrder ? ar[0].getTarget() : ar[1].getTarget();
                AMRNode secondChild = !inOrder ? ar[0].getTarget() : ar[1].getTarget();
                if ((startsWithPos(firstChild, "NN") || startsWithPos(firstChild, "PRP"))
                        && (startsWithPos(secondChild, "NN") || startsWithPos(secondChild, "JJ"))) {
                    graph.addLabelledEdge(secondChild, firstChild, ":domain");
                    if (rootNodes.contains(node)) { // if the node is a root then replace with the new edge head
                        rootNodes.add(secondChild); // add secondChild as a candidate root node...
                    } else { // otherwise replace new edge in place of the old one headed by the copula verb
                        AMRNode parent = graph.getFirstParent(node);
                        graph.addLabelledEdge(parent, secondChild, graph.getEdgeLabel(parent, node));
                    }
                    toNodes.remove(secondChild); // ...and remove it from a target node
                    toNodes.add(firstChild); // instead, add firstChild as a target node 
                    graph.removeAll(node); // completely remove node and children from graph
                    graph.removeEdge(firstChild, secondChild); // remove any potential edges between siblings
                    rootNodes.remove(node); // node only had these two children, so it can no longer be a root candidate
                }
            } else if (children != null && children.size() == 1) {
                AMRNode child = children.toArray(new AMREdge[0])[0].getTarget();
                graph.removeAll(node);
                rootNodes.remove(node);
                if (!graph.containsEdge(child)) {
                    toNodes.remove(child);
                }
            }
        }
        Iterator<AMRNode> it = toNodes.iterator(); // tackle any left-over copula verbs with no children
        while (it.hasNext()) {
            AMRNode node = it.next();
            if (isCopulaVerb(node)) {
                graph.removeEdges(node);
                it.remove();
            }
        }
    }

    /**
     *
     * Remove single edges rooted at preposition nodes that contain nodes being
     * used elsewhere. Essentially, we want to get rid of superfluous edges
     * between prepositions and re-entrance nodes.
     *
     * @param rootNodes
     * @param graph
     */
    private void processPrepositionNodes(Set<AMRNode> rootNodes, AMRGraph graph) {
        Set<AMRNode> newCandRootNodes = new HashSet<>();
        Iterator<AMRNode> it = rootNodes.iterator();
        while (it.hasNext()) {
            AMRNode root = it.next();
            if (startsWithPos(root, "IN") && graph.get(root).size() == 1) {
                AMRNode toNode = graph.get(root).toArray(new AMREdge[0])[0].getTarget();
                if (graph.containsEdgeExcluding(root, toNode)) {
                    it.remove();
                    graph.removeEdge(root, toNode);
                    if (isOrphanNodeWithChildren(toNode, rootNodes, graph)) {
                        newCandRootNodes.add(toNode);
                    }
                }
            }
        }
        rootNodes.addAll(newCandRootNodes);
    }

    private boolean isOrphanNodeWithChildren(AMRNode node, Set<AMRNode> rootNodes, AMRGraph graph) {
        Collection<AMREdge> children = graph.get(node);
        return !(graph.containsEdge(node) || children == null || children.isEmpty() || rootNodes.contains(node));
    }

    private AMRNode getNode(final SyntaxTreeNode parse, final Map<Integer, AMRNode> leafIdToNode, Multiset<Character> varCounter, final int leafId) {
        AMRNode node = leafIdToNode.get(leafId);
        if (node == null) {
            SyntaxTreeNode.SyntaxTreeNodeLeaf leaf = parse.getLeaves().get(leafId);
            String lemma = leafToLemma(leaf);
            String varName = getVariableName(lemma, varCounter);
            node = new AMRNode(lemma, varName, leaf.getPos(), leafId);
            leafIdToNode.put(leafId, node);
        }
        return node;
    }

    private String leafToLemma(final SyntaxTreeNode.SyntaxTreeNodeLeaf leaf) {
        return MorphaStemmer.stemToken(leaf.getWord().toLowerCase().
                replaceAll(" ", "_").replaceAll(":", "-").replaceAll("/", "-").replaceAll("\\(", "LRB").replaceAll("\\)", "RRB"), leaf.getPos());
    }

    /**
     *
     * Create the variable name for the input AMR concept. By convention, we
     * return the first letter of the concept. In case it already exists we just
     * append a unique id.
     *
     * @param conceptName
     * @return
     */
    private String getVariableName(String conceptName, Multiset<Character> varCounter) {
        char firstChar = conceptName.charAt(0);
        int uniqueId = varCounter.add(firstChar, 1);
        return firstChar + (uniqueId == 0 ? "" : String.valueOf(uniqueId + 1));
    }

    private boolean isCategory(final SyntaxTreeNode parse, final int leafId, final Category category) {
        return parse.getLeaves().get(leafId).getCategory() == category;
    }

    private boolean startsWithPos(final SyntaxTreeNode parse, final int leafId, final String posTag) {
        return parse.getLeaves().get(leafId).getPos().startsWith(posTag);
    }

    private boolean startsWithPos(final AMRNode node, final String posTag) {
        return node.getPos().startsWith(posTag);
    }

    private boolean isCopulaVerb(final AMRNode node) {
        return node.getConceptName().equals("be");
    }

    private String getWord(final SyntaxTreeNode parse, final int leafId) {
        return parse.getLeaves().get(leafId).getWord();
    }
    
    private String dependencyToString(final SyntaxTreeNode parse, UnlabelledDependency dep) {
        int head = dep.getHead();
        int arg = dep.getFirstArgumentIndex();
        return String.format("%s\t%s -> %s [%s] %s %s", dep, getWord(parse, head), getWord(parse, arg), dep.getArgNumber(), dep.getCategory(), dep.getCategory().isFunctionIntoModifier());
    }
    
    private String dependencyToString(final SyntaxTreeNode parse, ResolvedDependency dep) {
        int head = dep.getPropbankPredicateIndex();
        int arg = dep.getPropbankArgumentIndex();
        return String.format("%s\t%s -> %s [%s] %s %s", dep, getWord(parse, head), getWord(parse, arg), dep.getArgument(), dep.getCategory(), dep.getCategory().isFunctionIntoModifier());
    }
    
    /**
     *
     * Decide which dependencies are worth showing.
     *
     * @param dep
     * @param parse
     * @return
     */
    protected boolean showDependency(final ResolvedDependency dep, final SyntaxTreeNode parse) {
        final SyntaxTreeNode.SyntaxTreeNodeLeaf predicateNode = parse.getLeaves().get(dep.getHead());
        if (dep.getHead() == dep.getArgumentIndex()) {
            return false;
        } else if (dep.getSemanticRole() != SRLFrame.NONE) {
            return true;
        } else if (predicateNode.getPos().startsWith("JJ") && dep.getCategory() != Category.valueOf("NP/N")) {
            // Adjectives, excluding determiners
            return true;
        } else if (isAuxilliaryVerb(dep, parse, predicateNode)) {
            // Exclude auxiliary verbs. Hopefully the SRL will have already identified the times this category isn't
            // an auxiliary.
            return false;
        } else if (predicateNode.getPos().startsWith("NN") && dep.getCategory().isFunctionInto(Category.N)) {
            // Checking category to avoid annoying getting a "subject" dep on yesterday|NN|(S\NP)\(S\NP)
            return true;
        } else if (predicateNode.getPos().startsWith("VB")) {
            // Other verb arguments, e.g. particles
            return true;
        } else if (predicateNode.getPos().startsWith("IN")) {
            // Prepositions: CCG treats them as heads, but we need to swap them later on with their dependent
            return true;
        } else if (dep.getCategory() == Category.CONJ) {
            return true;       
        } else {
            return false;
        }
    }

    protected boolean showDependency(final UnlabelledDependency dep, final SyntaxTreeNode parse) {
        final SyntaxTreeNode.SyntaxTreeNodeLeaf predicateNode = parse.getLeaves().get(dep.getHead());
        if (dep.getHead() == dep.getFirstArgumentIndex()) {
            return false;
//        } else if (dep.getSemanticRole() != SRLFrame.NONE) {
//            return true;
        } else if (predicateNode.getPos().startsWith("JJ") && dep.getCategory() != Category.valueOf("NP/N")) {
            // Adjectives, excluding determiners
            return true;
        } else if (isAuxilliaryVerb(dep, parse, predicateNode)) {
            // Exclude auxiliary verbs. Hopefully the SRL will have already identified the times this category isn't
            // an auxiliary.
            return false;
        } else if (predicateNode.getPos().startsWith("NN") && dep.getCategory().isFunctionInto(Category.N)) {
            // Checking category to avoid annoying getting a "subject" dep on yesterday|NN|(S\NP)\(S\NP)
            return true;
        } else if (predicateNode.getPos().startsWith("VB")) {
            // Other verb arguments, e.g. particles
            return true;
        } else if (predicateNode.getPos().startsWith("IN")) {
            // Prepositions: CCG treats them as heads, but we need to swap them later on with their dependent
            return true;
        } else if (dep.getCategory() == Category.CONJ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * Identify auxiliary verbs, i.e., have, be, etc.. but not the copula verb,
     * e.g., The boa constrictor is a dangerous creature
     *
     * @param dep
     * @param parse
     * @param predicateNode
     * @return
     */
    private boolean isAuxilliaryVerb(final ResolvedDependency dep, final SyntaxTreeNode parse, final SyntaxTreeNode.SyntaxTreeNodeLeaf predicateNode) {
        if (dep.getCategory().toString().replaceAll("\\[\\w+\\]", "").equals("(S\\NP)/(S\\NP)")) {
            return !(leafToLemma(predicateNode).equals("be") && !startsWithPos(parse, dep.getArgumentIndex(), "VB"));
        }
        return false;
    }

    private boolean isAuxilliaryVerb(final Dependency dep, final SyntaxTreeNode parse, final SyntaxTreeNode.SyntaxTreeNodeLeaf predicateNode) {
        if (dep.getCategory().toString().replaceAll("\\[\\w+\\]", "").equals("(S\\NP)/(S\\NP)")) {
            return !(leafToLemma(predicateNode).equals("be") && !startsWithPos(parse, dep.getArgNumber(), "VB"));
        }
        return false;
    }

    /**
     *
     * Helper class that stores the source node and the label of an edge in the
     * AMR graph
     */
    private final class ParentAMRNodeLabel {

        AMRNode parentNode;
        String label;

        public ParentAMRNodeLabel(AMRNode parent, String label) {
            this.parentNode = parent;
            this.label = label;
        }
    }

}
