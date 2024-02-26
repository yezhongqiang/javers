package org.javers.core;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import nonapi.io.github.classgraph.reflection.ReflectionUtils;
import org.javers.common.collections.Arrays;
import java.util.List;
import java.util.Optional;
import org.javers.common.collections.Maps;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ReferenceChange;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ArrayChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.javers.core.metamodel.object.GlobalId;
import org.javers.core.metamodel.object.ValueObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ObjectChange {
    private static final Logger logger = LoggerFactory.getLogger(ObjectChange.class);
    public CommitMetadata getCommitMetadata() {
      return commitMetadata;
    }

    public GlobalId getGlobalId() {
      return globalId;
    }

    public enum ChangeType {
      NEW,
      UPDATE,
      DELETE
    }
    public static class ChangedProperty {
      private ChangeType changeType;
      private String propertyName;
      private Object left;
      private Object right;
      private String rightAsString;

      public Object getLeft() {
        return left;
      }
      public Object getRight() {
        return right;
      }
      public String getPropertyName() {
        return propertyName;
      }
      public String getRightAsString() { return rightAsString; }

      public void setLeft(Object left) {
        this.left = left;
      }
      public void setRight(Object right) {
        this.right = right;
      }
      public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
      }
      public void setRightAsString(String rightAsString) {
        this.rightAsString = rightAsString;
      }

      public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
      }

      public ChangeType getChangeType() {
        return changeType;
      }
    }

    private final CommitMetadata commitMetadata;
    private final GlobalId globalId;
    private final long snapshotVersion;
    private final List<ChangedProperty> changedProperties = new ArrayList<>();

    public ObjectChange(CommitMetadata commitMetadata, GlobalId globalId, long snapshotVersion) {
      this.commitMetadata = commitMetadata;
      this.globalId = globalId;
      this.snapshotVersion = snapshotVersion;
    }

    public long getSnapshotVersion() {
      return snapshotVersion;
    }

    public void convertChanges(List<Change> changes) {
      List<Change> newOrRemoveObjectChanges = changes.stream().filter(change -> change instanceof NewObject || change instanceof ObjectRemoved).collect(
          Collectors.toList());
      if (!newOrRemoveObjectChanges.isEmpty()) {
        changes = newOrRemoveObjectChanges;
      }
      changes.forEach(change -> {
        if (change instanceof NewObject) {
          addNewObjectChange((NewObject) change);
        } else if (change instanceof ObjectRemoved) {
          addObjectRemovedChange((ObjectRemoved) change);
        } else if (change instanceof ValueChange) {
          addValueChange((ValueChange) change);
        } else if (change instanceof ReferenceChange) {
          addReferenceChange((ReferenceChange) change);
        } else if (change instanceof ContainerChange) {
          addContainerChange((ContainerChange) change);
        } else if (change instanceof MapChange) {
          addMapChange((MapChange) change);
        } else {
          logger.trace("not processed for change type " + change.getClass().getName());
        }
      });
    }

  private void addMapChange(MapChange mapChange) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.UPDATE);
    changedProperty.setLeft(mapChange.getLeft());

    Map processedMap = getPopulatedMap(mapChange);
    changedProperty.setRight(processedMap);
    changedProperty.setPropertyName(mapChange.getPropertyNameWithPath());
    changedProperty.setRightAsString(convertMapToString(processedMap));
    changedProperties.add(changedProperty);
  }

  private void addContainerChange(ContainerChange containerChange) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.UPDATE);
    changedProperty.setLeft(containerChange.getLeft());
    List<?> processedList = getPopulatedList(containerChange);
    changedProperty.setRight(processedList);
    changedProperty.setPropertyName(containerChange.getPropertyNameWithPath());
    changedProperty.setRightAsString(convertContainerToString(processedList));
    changedProperties.add(changedProperty);
  }

  private void addReferenceChange(ReferenceChange change) {
    ReferenceChange referenceChange = change;
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.UPDATE);
    changedProperty.setLeft(referenceChange.getLeft());

    changedProperty.setRight(referenceChange.getRight());
    changedProperty.setPropertyName(referenceChange.getPropertyNameWithPath());
    changedProperty.setRightAsString(convertReferenceToString(referenceChange.getRightObject()));
    changedProperties.add(changedProperty);
  }

  private void addValueChange(ValueChange valueChange) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.UPDATE);
    changedProperty.setLeft(valueChange.getLeft());
    Object processedObject = getPopulatedObject(valueChange);
    changedProperty.setRight(processedObject);
    changedProperty.setPropertyName(valueChange.getPropertyNameWithPath());
    changedProperty.setRightAsString(convertValueToString(processedObject));
    changedProperties.add(changedProperty);
  }

  private void addObjectRemovedChange(ObjectRemoved objectRemoved) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.DELETE);
    changedProperty.setLeft(null);
    changedProperty.setRight(null);
    changedProperty.setPropertyName(objectRemoved.getAffectedGlobalId().value());
    changedProperty.setRightAsString(null);
    changedProperties.add(changedProperty);
  }

  private void addNewObjectChange(NewObject newObject) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.NEW);
    changedProperty.setLeft(null);
    changedProperty.setRight(null);
    changedProperty.setPropertyName(newObject.getAffectedGlobalId().value());
    changedProperty.setRightAsString(null);
    changedProperties.add(changedProperty);
  }

  private String convertObjectToString(Optional<Object> object) {
      return object.orElse("").toString();
    }

    private String convertMapToString(Object right) {
      return Joiner.on(",").withKeyValueSeparator("=").join(Maps.wrapNull(right));
    }

    private String convertContainerToString(List<?> processedList) {
      if (processedList == null) {
        return null;
      }
      return processedList.toString();
    }

  private List getPopulatedList(ContainerChange change) {
      Object right = change.getRight();
      if (right == null) return null;
      List<?> changeList;
      if (change instanceof ArrayChange) {
        changeList = Arrays.asList(right);
      } else {
        changeList = (List<?>) right;
      }
      return changeList.stream().map(element -> {
        if (element instanceof ValueObjectId) {
          ValueObjectId valueObjectId = (ValueObjectId) element;
          return getRealObject(valueObjectId, change);
        }
        return element;
      }).collect(Collectors.toList());
  }

  private Map getPopulatedMap(MapChange change) {
    Object right = change.getRight();
    if (right == null) return null;
    Map<Object, Object> rightMap = (Map<Object, Object>)right;
    Map<Object, Object> result = new HashMap<>();
    rightMap.forEach((k,v) -> {
      if (v instanceof ValueObjectId) {
        result.put(k, getRealObject((ValueObjectId) v, change));
      }
    });
    return result;
  }

  private Object getPopulatedObject(ValueChange change) {
    Object right = change.getRight();
    if (right == null) return null;
    if (right instanceof ValueObjectId) {
      return getRealObject((ValueObjectId) right, change);
    }
    return right;
  }

  private Object getRealObject(ValueObjectId valueObjectId, Change change) {
      Object affectedObj = change.getAffectedObject().orElse(null);
      if (affectedObj == null) return valueObjectId;
      String[] fragmentSplit = valueObjectId.getFragment().split("/");
      Object fieldValue = ReflectionUtils.getFieldVal(false, affectedObj, fragmentSplit[0]);
      if (fieldValue instanceof List) {
        return ((List<?>)fieldValue).get(Integer.parseInt(fragmentSplit[1]));
      } else {
        return fieldValue;
      }
  }

  private String convertReferenceToString(Optional<Object> rightObject) {
        return rightObject.orElse("").toString();
    }

    private String convertValueToString(Object right) {
        return String.valueOf(right);
    }

    public List<ChangedProperty> getChanges() {
        return this.changedProperties;
    }
}
