package org.javers.core;

public interface DiffIdResolver {
  Object resolve(String propertyName, Object id);
}
