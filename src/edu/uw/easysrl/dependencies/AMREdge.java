package edu.uw.easysrl.dependencies;

import java.io.Serializable;
import java.util.Objects;

public class AMREdge implements Serializable{
    
    private static final long serialVersionUID = 1L;    
    private final String label;
    private final AMRNode target;

    public AMREdge(String label, AMRNode target) {
        this.label = label;
        this.target = target;
    }

    public AMRNode getTarget() {
        return target;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object obj) {
        assert obj instanceof AMREdge;
        AMREdge e = (AMREdge)obj;
        return e.label.equals(label) && e.target.equals(target);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.label);
        hash = 31 * hash + Objects.hashCode(this.target);
        return hash;
    }
    
    @Override
    public String toString() {
        return label;
    }
    
    
}
