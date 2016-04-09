package edu.uw.easysrl.dependencies;

import java.util.Objects;

/**
 *
 * @author ikonstas
 */
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

    public String getPredicateStr() {
        return predicate.getConceptName();
    }

    public AMRNode getArgument() {
        return argument;
    }

    public String getArgumentStr() {
        return argument.getConceptName();
    }

    public String getRole() {
        return role;
    }

    public boolean predicateEqualsArgument() {
        return getPredicateStr().equals(getArgumentStr());
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
