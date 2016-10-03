package me.aslanoglou.dit.compilers.pigletgenerator.containers;

/**
 * Generic container (aka wrapper) class for Classes, Functions, Variables.
 */
public class Container {
    String name;
    String type;
    String className;
    int order;
    boolean isMethod;

    public Container(String name, String type, String className, int order, boolean isMethod) {
        this.name = name;
        this.type = type;
        this.className = className;
        this.order = order;
        this.isMethod = isMethod;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getOrder() {
        return order;
    }

    public boolean isMethod() {
        return isMethod;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
