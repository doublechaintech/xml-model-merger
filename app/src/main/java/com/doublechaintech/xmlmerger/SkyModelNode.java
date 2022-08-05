package com.doublechaintech.xmlmerger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkyModelNode {

  protected String name;
  protected List<SkyModelAttribute> attributes;
  protected List<SkyModelNode> children;
  protected SkyModelNode parent;
  protected String type = "TYPE";
  protected Map<String, List<String>> dataInDirective = new HashMap<>();

  public SkyModelNode getParent() {
    return parent;
  }

  public void setParent(SkyModelNode parent) {
    this.parent = parent;
  }

  public List<SkyModelNode> getChildren() {
    return children;
  }

  public void setChildren(List<SkyModelNode> children) {
    this.children = children;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<SkyModelAttribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<SkyModelAttribute> attributes) {
    this.attributes = attributes;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void addChild(SkyModelNode newNode) {
    ensureChildren().add(newNode);
  }

  protected List<SkyModelNode> ensureChildren() {
    if (children != null) {
      return children;
    }
    return children = new ArrayList<>();
  }

  public void addAttribute(String attrName, String attrValue) {
    SkyModelAttribute attr = new SkyModelAttribute();
    attr.setName(attrName);
    attr.setValue(attrValue);
    ensureAttributes().add(attr);
  };

  public void removeAttribute(String attrName) {
    SkyModelAttribute attr = getAttribute(attrName);
    if (attr == null) {
      return;
    }
    attributes.remove(attr);
  }

  protected List<SkyModelAttribute> ensureAttributes() {
    if (attributes != null) {
      return attributes;
    }
    return attributes = new ArrayList<>();
  }

  public SkyModelAttribute getAttribute(String attrName) {
    if (attributes == null) {
      return null;
    }
    return attributes.stream()
        .filter(a -> a.getName().equalsIgnoreCase(attrName))
        .findFirst()
        .orElse(null);
  }

  public List<SkyModelNode> getAllDescendant() {
    List<SkyModelNode> result = new ArrayList<>();
    if (this.getChildren() == null || this.getChildren().isEmpty()) {
      return result;
    }
    for (SkyModelNode chd : this.getChildren()) {
      result.add(chd);
      result.addAll(chd.getAllDescendant());
    }
    return result;
  }

  public Map<String, List<String>> getDataInDirective() {
    return dataInDirective;
  }

  public void setDataInDirective(Map<String, List<String>> dataInDirective) {
    this.dataInDirective = dataInDirective;
  }

  public void addDataInDirective(Map<String, List<String>> moreData){
    if (moreData == null || moreData.isEmpty()){
      return;
    }
    if (dataInDirective == null){
      dataInDirective = new HashMap<>();
    }
    moreData.forEach((name,data)->{
      List<String> list = dataInDirective.computeIfAbsent(name, k -> new ArrayList<>());
      list.addAll(data);
    });
  }
}
