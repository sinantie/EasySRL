package edu.uw.easysrl.dependencies;

import java.io.Serializable;
import java.util.Objects;

public class AMRNode implements Serializable {
    
    
    private static final long serialVersionUID = 1L;    
    private boolean visited;
    private String conceptName;
    private final String varName, pos;
    private final int leafId;

    public AMRNode(String concept, String varName, String posTag, int leafId) {        
        this.conceptName = concept;
        this.varName = varName;        
        this.pos = posTag;
        this.leafId = leafId;
    }

    public boolean isBefore(AMRNode node) {
        return node.leafId > leafId;
    }
    
    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public String getConceptName() {
        return conceptName;
    }

    public String getVarName() {
        return varName;
    }

    public String getPos() {
        return pos;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    @Override
    public boolean equals(Object obj) {
        assert obj instanceof AMRNode;
        return varName.equals(((AMRNode)obj).varName);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.varName);
        return hash;
    }
    
    
    @Override
    public String toString() {
        return String.format("%s / %s", varName, conceptName);
    }
    
    
    
}
