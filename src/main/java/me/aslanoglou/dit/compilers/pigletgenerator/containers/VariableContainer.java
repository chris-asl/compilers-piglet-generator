package me.aslanoglou.dit.compilers.pigletgenerator.containers;

/**
 * Holds information for variables.
 */
public class VariableContainer extends Container {
    public VariableContainer(String name, String type, String className, int index) {
        super(name, type, className, index, false);
    }

    public boolean isIdentical(VariableContainer other) {
        return this.name.equals(other.name) && this.type.equals(other.type);
    }

    public boolean isIdenticalOrdered(VariableContainer other) {
        return this.name.equals(other.name) && this.type.equals(other.type) && (this.order == other.order);
    }

    public boolean hasSameType(String othType) {
        return this.type.equals(othType);
    }

    public static boolean isInteger(String s) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i), 10) < 0) return false;
        }
        return true;
    }
}
