package org.javers.core;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
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
import org.javers.core.diff.changetype.map.EntryAdded;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.javers.core.json.JsonConverter;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.GlobalId;
import org.javers.core.metamodel.object.InstanceId;
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

    private final transient Function<GlobalId, Optional<CdoSnapshot>> cdoSnapshotCall;

    private final transient JsonConverter jsonConverter;

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
      private String leftAsString;
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

      public String getLeftAsString() {
        return leftAsString;
      }

      public void setLeftAsString(String leftAsString) {
        this.leftAsString = leftAsString;
      }
    }

    private final CommitMetadata commitMetadata;
    private final GlobalId globalId;
    private final long snapshotVersion;
    private final List<ChangedProperty> changedProperties = new ArrayList<>();

    public ObjectChange(Function<GlobalId, Optional<CdoSnapshot>> cdoSnapshotCall,
                        JsonConverter jsonConverter, CommitMetadata commitMetadata, GlobalId globalId, long snapshotVersion) {
      this.cdoSnapshotCall = cdoSnapshotCall;
      this.jsonConverter = jsonConverter;
      this.commitMetadata = commitMetadata;
      this.globalId = globalId;
      this.snapshotVersion = snapshotVersion;
    }

    public long getSnapshotVersion() {
      return snapshotVersion;
    }

    public void convertChanges(List<Change> changes) {
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
      mapChange.getEntryAddedChanges().forEach(c -> {
          EntryAdded addChange = ((EntryAdded)c);
          ObjectChange.ChangedProperty changedProperty = new ChangedProperty();
          changedProperty.setChangeType(ChangeType.NEW);
          changedProperty.setPropertyName(mapChange.getPropertyNameWithPath() + "." + addChange.getKey().toString());
          changedProperty.setRight(addChange.getValue());
          changedProperty.setRightAsString(toDiffString(addChange.getValue()));
          changedProperties.add(changedProperty);
      });


      mapChange.getEntryRemovedChanges().forEach(c -> {
          EntryRemoved removeChange = (EntryRemoved) c;
          ChangedProperty changedProperty = new ChangedProperty();
          changedProperty.setChangeType(ChangeType.DELETE);
          changedProperty.setPropertyName(mapChange.getPropertyNameWithPath() + "." + removeChange.getKey().toString());
          changedProperty.setLeft(removeChange.getValue());
          changedProperty.setLeftAsString(toDiffString(removeChange.getValue()));
          changedProperties.add(changedProperty);
      });

      mapChange.getEntryValueChanges().forEach(c -> {
          EntryValueChange updateChange = ((EntryValueChange) c);
          ChangedProperty changedProperty = new ChangedProperty();
          changedProperty.setChangeType(ChangeType.UPDATE);
          changedProperty.setPropertyName(mapChange.getPropertyNameWithPath() + "." + updateChange.getKey().toString());
          changedProperty.setLeft(updateChange.getLeftValue());
          changedProperty.setRight(updateChange.getRightValue());
          changedProperty.setLeftAsString(toDiffString(updateChange.getLeftValue()));
          changedProperty.setRightAsString(toDiffString(updateChange.getRightValue()));
          changedProperties.add(changedProperty);
      });
  }

  private void addContainerChange(ContainerChange containerChange) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.UPDATE);
    changedProperty.setPropertyName(containerChange.getPropertyNameWithPath());

    List<?> leftList = getPopulatedList(containerChange, containerChange.getLeft());
    changedProperty.setLeft(leftList);
    changedProperty.setLeftAsString(convertContainerToString(leftList));

    List<?> rightList = getPopulatedList(containerChange, containerChange.getRight());
    changedProperty.setRight(rightList);
    changedProperty.setRightAsString(convertContainerToString(rightList));

    changedProperties.add(changedProperty);
  }

  private void addReferenceChange(ReferenceChange referenceChange) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.UPDATE);
    changedProperty.setPropertyName(referenceChange.getPropertyNameWithPath());

    changedProperty.setLeft(referenceChange.getLeft());
    Optional<Object> leftObject = getLeftObject(referenceChange.getLeftObject());
    changedProperty.setLeftAsString(convertReferenceToString(leftObject));

    changedProperty.setRight(referenceChange.getRight());
    changedProperty.setRightAsString(convertReferenceToString(referenceChange.getRightObject()));

    changedProperties.add(changedProperty);
  }

  private Optional<Object> getLeftObject(Optional<Object> leftObject) {
    Object valueObject = ((Optional<?>) leftObject).orElse(null);
    if (valueObject instanceof InstanceId) {
      valueObject = getValueObject( ((InstanceId) valueObject).masterObjectId(), valueObject);
      leftObject = Optional.ofNullable(valueObject);
    }
    return leftObject;
  }

  private Object getValueObject(GlobalId globalId, Object valueObject) {
    try {
      CdoSnapshot cdoSnapshot = cdoSnapshotCall.apply(globalId).orElse(null);
      if (cdoSnapshot != null) {
        valueObject =convertToObject(cdoSnapshot.getState().getProperties(), Class.forName(
            globalId.getTypeName()));
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    return valueObject;
  }

  private void addValueChange(ValueChange valueChange) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.UPDATE);
    changedProperty.setPropertyName(valueChange.getPropertyNameWithPath());

    Object leftObject = getPopulatedObject(valueChange, valueChange.getLeft());
    changedProperty.setLeft(leftObject);
    changedProperty.setLeftAsString(convertObjectToString(leftObject));

    Object rightObject = getPopulatedObject(valueChange, valueChange.getRight());
    changedProperty.setRight(rightObject);
    changedProperty.setRightAsString(convertObjectToString(rightObject));

    changedProperties.add(changedProperty);
  }

  private void addObjectRemovedChange(ObjectRemoved objectRemoved) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.DELETE);
    changedProperty.setPropertyName(objectRemoved.getAffectedGlobalId().value());
    changedProperty.setLeft(null);
    changedProperty.setLeftAsString(convertObjectToString(objectRemoved.getAffectedObject()));

    changedProperty.setRight(null);
    changedProperty.setRightAsString(null);
    changedProperties.add(changedProperty);
  }

  private void addNewObjectChange(NewObject newObject) {
    ChangedProperty changedProperty = new ChangedProperty();
    changedProperty.setChangeType(ChangeType.NEW);
    changedProperty.setPropertyName(newObject.getAffectedGlobalId().value());

    changedProperty.setLeft(null);
    changedProperty.setLeftAsString(null);

    changedProperty.setRight(null);
    changedProperty.setRightAsString(convertObjectToString(newObject.getAffectedObject()));
    changedProperties.add(changedProperty);
  }

  private List getPopulatedList(ContainerChange change, Object obj) {
      if (obj == null) return null;
      List<?> changeList;
      if (change instanceof ArrayChange) {
        changeList = Arrays.asList(obj);
      } else {
        changeList = (List<?>) obj;
      }
      return changeList.stream().map(element -> {
        if (element instanceof ValueObjectId) {
          ValueObjectId valueObjectId = (ValueObjectId) element;
          return getRealObject(valueObjectId, change);
        }
        return element;
      }).collect(Collectors.toList());
  }

  private Map getPopulatedMap(MapChange change, Object obj) {
    if (obj == null) return null;
    Map<Object, Object> objMap = (Map<Object, Object>)obj;
    Map<Object, Object> result = new HashMap<>();
    objMap.forEach((k,v) -> {
      if (v instanceof ValueObjectId) {
        result.put(k, getRealObject((ValueObjectId) v, change));
      }
    });
    return result;
  }

  private Object getPopulatedObject(ValueChange change, Object obj) {
    if (obj == null) return null;
    if (obj instanceof ValueObjectId) {
      return getRealObject((ValueObjectId) obj, change);
    } else {
      Object affectedObject = change.getAffectedObject().orElse(null);
      if (affectedObject != null) {
        obj = getReferToObject(change, affectedObject, obj);
      }
    }
    return obj;
  }

  private Object getReferToObject(ValueChange change, Object affectedObject, Object obj) {
      if (affectedObject instanceof DiffIdResolver) {
        DiffIdResolver diffIdResolver = (DiffIdResolver) affectedObject;
        return diffIdResolver.resolve(change.getPropertyName(), obj);
      }
      return obj;
  }

  private Object getRealObject(ValueObjectId valueObjectId, Change change) {
      Object affectedObj = change.getAffectedObject().orElse(null);
      if (affectedObj == null) return valueObjectId;
      String[] fragmentSplit = valueObjectId.getFragment().split("/");
      Object fieldValue = ReflectionUtils.getFieldVal(false, affectedObj, fragmentSplit[0]);
      if (fieldValue instanceof List) {
        int index = Integer.parseInt(fragmentSplit[1]);
        if (((List<?>)fieldValue).size() > index) {
          return ((List<?>)fieldValue).get(index);
        }
        else {
          return getValueObject(valueObjectId, null);
        }
      } else {
        return fieldValue;
      }
  }

  public List<ChangedProperty> getChanges() {
    return this.changedProperties;
  }

  private String convertReferenceToString(Optional<Object> referenceObject) {
      Object obj = referenceObject.orElse(null);
      return toDiffString(obj);
  }

  private String toDiffString(Object obj) {
      if (obj instanceof Optional) {
        obj = ((Optional<?>) obj).orElse(null);
      }
      if (obj instanceof DiffString) {
        return ((DiffString) obj).toDiffString();
      }
      return obj == null ? null : obj.toString();
  }

  private String convertObjectToString(Object obj) {
      return toDiffString(obj);
  }

  private String convertMapToString(Object obj) {
    Map map = Maps.wrapNull(obj);
    if (map.isEmpty()) return null;
    StringBuilder stringBuilder = new StringBuilder();
    map.forEach((k,v)->{
      stringBuilder.append(toDiffString(k)).append("=").append(toDiffString(v)).append("=");
    });
    return stringBuilder.deleteCharAt(stringBuilder.length()-1).toString();
  }

  private String convertContainerToString(List<?> processedList) {
    if (processedList == null || processedList.isEmpty()) {
      return null;
    }
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[");
    processedList.forEach(v->{
      stringBuilder.append(toDiffString(v)).append(",");
    });
    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
    stringBuilder.append("]");
    return stringBuilder.toString();
  }

  public <T> T convertToObject(Map<String, Object> properties, Class<T> entityClass) {
      Map<String, Object> objectMap = populateProperties(cdoSnapshotCall, properties);
      JsonElement jsonElement = jsonConverter.toJsonElement(objectMap);
      return jsonConverter.fromJson(jsonElement, entityClass);
  }

  private Map<String, Object> populateProperties(Function<GlobalId, Optional<CdoSnapshot>> cdoSnapshotCall, Map<String, Object> properties) {
      Map<String, Object> result = new HashMap<>();
      properties.forEach((k,v)-> {
        if (v instanceof GlobalId) {
          GlobalId globalId1 = (GlobalId)v;
          CdoSnapshot cdoSnapshot = cdoSnapshotCall.apply(globalId1).orElse(null);
          if (cdoSnapshot != null) {
            try {
              v = convertToObject(cdoSnapshot.getState().getProperties(), Class.forName(globalId1.getTypeName()));
            } catch (ClassNotFoundException e) {
              throw new RuntimeException(e);
            }
          }
        }
        result.put(k, v);
      });
      return result;
  }

}
