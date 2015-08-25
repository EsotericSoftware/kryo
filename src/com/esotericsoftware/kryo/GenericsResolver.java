package com.esotericsoftware.kryo;

import java.util.ArrayDeque;
import java.util.Deque;

/** Helper class that resolves a type name variable to a concrete class using the current class serialization stack
 *
 * @author Jeroen van Erp <jeroen@hierynomus.com> */
public class GenericsResolver {
    private Deque<Generics> stack = new ArrayDeque<Generics>();

    public GenericsResolver () {
    }

    public Class getConcreteClass (String typeVar) {
        for (Generics generics : stack) {
            Class clazz = generics.getConcreteClass(typeVar);
            if (clazz != null) return clazz;
        }
        return null;
    }

    public boolean isSet () {
        return !stack.isEmpty();
    }

    public void pushScope (Generics scope) {
        stack.push(scope);
    }

    public void popScope () {
        stack.pop();
    }
}
