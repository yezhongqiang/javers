package org.javers.core.model;

import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pawel Cierpiatka
 */
public class Category {

    @Id
    private Long id;
    private String name;
    private Category parent;
    private List<Category> categories = new ArrayList<>();

    public Category(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public void addChild(Category child) {
        child.setParent(this);
        categories.add(child);
    }
}