package com.doublechaintech.xmlmerger;

import cn.hutool.core.io.FileUtil;
import org.apache.commons.codec.Charsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public class XmlMerger {

  static String PARSER_CLASS = "com.doublechaintech.util.CustomXmlParser";
  protected static Map<String, Map<String, Long>> fileTimestamps = new HashMap<>();

  protected static final String ATTR_INDENT = "    ";
  protected static final String NODE_INDENT = "  ";
  protected File xmlFile;
  protected SkyProjectModel projectModel;
  protected Map<String, List<String>> dataInDirective = new HashMap<>();
  protected boolean inDataTag = false;

  public SkyProjectModel getProjectModel() {
    return projectModel;
  }

  public void setProjectModel(SkyProjectModel projectModel) {
    this.projectModel = projectModel;
  }

  public File getXmlFile() {
    return xmlFile;
  }

  public void setXmlFile(File xmlFile) {
    this.xmlFile = xmlFile;
  }

  public boolean loadXmlFile() throws Exception {

    projectModel = new SkyProjectModel();
    if (!xmlFileChanged()) {
      return false;
    }
    fileTimestamps.put(xmlFile.getAbsolutePath(), new HashMap<>());
    loadSingleXMLFile(xmlFile, projectModel, new HashMap<>());
    return true;
  }

  protected boolean xmlFileChanged() throws Exception {
    String key = xmlFile.getAbsolutePath();
    Map<String, Long> fts = fileTimestamps.get(key);
    if (fts == null) {
      return true;
    }
    if (fts.isEmpty()) {
      return true;
    }
    if (!fts.containsKey(key)) {
      return true;
    }
    for (Map.Entry<String, Long> entry : fts.entrySet()) {
      String fileName = entry.getKey();
      Long ts = entry.getValue();
      long lstTs = new File(fileName).lastModified();
      if (lstTs > ts) {
        return true;
      }
    }
    return false;
  }

  protected void addFileTimestamp(File inputFile) throws Exception {
    String key = xmlFile.getAbsolutePath();
    Map<String, Long> fts = fileTimestamps.get(key);
    Long oldts = fts.put(inputFile.getAbsolutePath(), inputFile.lastModified());
    if (oldts != null) {
      throw new Exception(inputFile.getAbsolutePath() + " included more than once");
    }
  }

  public String getXmlString() throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(bos, false, "utf-8");
    for (SkyModelNode node : projectModel.getChildren()) {
      printNode(ps, node, "", true, true);
    }
    return bos.toString("UTF-8");
  }

  protected void printNode(
    PrintStream out, SkyModelNode node, String prefix, boolean pretty, boolean attrInNewLine) {
    writeValuesWithDataInDirective(node);
    if (pretty) {
      out.print(prefix);
    }
    if (node.getType().equals("COMMENTS")) {
      out.print("<!-- ");
      out.print(node.getName());
      if (node.getName().contains("\n")) {
        out.println();
        out.print(prefix);
      }
      out.println(" -->");
      return;
    }
    out.print(String.format("<%s", node.getName()));

    if (node.getAttributes() != null) {
      for (SkyModelAttribute attr : node.getAttributes()) {
        if (pretty) {
          if (attrInNewLine) {
            out.println();
            out.print(prefix);
            out.print(ATTR_INDENT);
          } else {
            out.println(" ");
          }
        } else {
          out.print(" ");
        }
        out.print(String.format("%s=\"%s\"", attr.getName(), escapeValue(attr.getValue())));
      }
    }
    {
      if (node.getChildren() == null) {
        out.println("");
      } else {
        out.println(">");
        out.println();
      }
    }

    if (node.getChildren() != null) {
      for (SkyModelNode child : node.getChildren()) {
        printNode(out, child, prefix + NODE_INDENT, pretty, attrInNewLine);
      }
      out.print(prefix);
      out.println(String.format("</%s>", node.getName()));
    } else {
      out.print(prefix);
      out.print(ATTR_INDENT);
      out.println("/>");
    }
    out.println();
  }

  protected SkyModelNode loadSingleXMLFile(File inputFile, SkyModelNode parentNode, Map<String, String> globalAttributes)
    throws Exception {
    File baseFolder = inputFile.getParentFile();
    System.out.println("[ XML Merger]: loading file " + inputFile.getAbsolutePath());
    addFileTimestamp(inputFile);
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance(PARSER_CLASS, null);
    factory.setNamespaceAware(true);
    XmlPullParser xpp = factory.newPullParser();
    xpp.setInput(FileUtil.getReader(inputFile, Charsets.UTF_8));
    SkyModelNode curNode = parentNode;
    int eventType = xpp.getEventType();
    do {
      if (eventType == XmlPullParser.START_DOCUMENT) {
        // System.out.println("Start document");
      } else if (eventType == XmlPullParser.END_DOCUMENT) {
        // System.out.println("End document");
      } else if (eventType == XmlPullParser.START_TAG) {
        curNode = onStartTag(xpp, baseFolder, curNode, globalAttributes);
      } else if (eventType == XmlPullParser.END_TAG) {
        curNode = onEndTag(xpp, baseFolder, curNode, globalAttributes);
      } else if (eventType == XmlPullParser.TEXT) {
        // System.out.println(xpp.getText().trim());
        curNode = onText(xpp, baseFolder, curNode, globalAttributes);
      } else if (eventType == XmlPullParser.COMMENT) {
        curNode = onComments(xpp, baseFolder, curNode, globalAttributes);
      }
      eventType = xpp.nextToken();
    } while (eventType != XmlPullParser.END_DOCUMENT);

    return curNode;
  }

  private SkyModelNode onText(XmlPullParser xpp, File baseFolder, SkyModelNode curNode, Map<String, String> globalAttributes) {
    if (!inDataTag) {
      return curNode;
    }
    String text = xpp.getText();
    if (text == null || text.trim().isEmpty()) {
      return curNode;
    }
    String[] lines = text.trim().split("[\\r\\n]+");
    for (String line : lines) {
      String str = line.trim();
      int pos = str.indexOf(":");
      String code;
      String name;
      if (pos < 0) {
        code = str;
        name = str;
      } else if (pos == 0) {
        code = "";
        name = str.substring(1);
      } else if (str.endsWith(":")) {
        code = str;
        name = "";
      } else {
        code = str.substring(0, pos);
        name = str.substring(pos + 1);
      }

      addDataValue("code", code);
      addDataValue("name", name);
    }
    return curNode;
  }

  private SkyModelNode onComments(XmlPullParser xpp, File baseFolder, SkyModelNode curNode, Map<String, String> globalAttributes) {
    String content = xpp.getText().trim();
    SkyModelNode commentNode = new SkyModelNode();
    commentNode.setType("COMMENTS");
    commentNode.setName(content);
    curNode.addChild(commentNode);
    commentNode.setParent(curNode);

    return curNode;
  }

  private void addChildAfter(SkyModelNode curNode, SkyModelNode commentNode) {
    SkyModelNode parentNode = findParentNode(curNode);
    Objects.requireNonNull(parentNode);
    parentNode.addChild(commentNode);
    commentNode.setParent(parentNode);
  }

  private SkyModelNode findParentNode(SkyModelNode curNode) {
    return curNode.getParent() == null ? curNode : curNode.getParent();
  }

  protected SkyModelNode onEndTag(XmlPullParser xpp, File baseFolder, SkyModelNode curNode, Map<String, String> globalAttributes) {
    String name = xpp.getName();
    if (isValueTag(name)) {
      return curNode;
    }
    if (this.dataInDirective.size() > 0) {
      curNode.addDataInDirective(this.dataInDirective);
    }
    dataInDirective.clear();


    // System.out.println("结束节点: " + name);
    if (name.equals("root")) {
      return curNode;
    }
    if (isImportTag(name)) {
      return curNode;
    }
    if (isDeleteTag(name)) {
      return curNode;
    }
    if (curNode.getParent() == null) {
      return curNode;
    }
    return curNode.getParent();
  }

  private boolean isDeleteTag(String name) {
    if (name.equalsIgnoreCase("#delete")) {
      return true;
    }
    return name.equalsIgnoreCase("_delete");
  }

  private boolean isValueTag(String name) {
    if (name.equalsIgnoreCase("#value")) {
      return true;
    }
    return name.equalsIgnoreCase("_value");
  }

  private boolean isImportTag(String name) {
    if (name.equalsIgnoreCase("#import")) {
      return true;
    }
    return name.equalsIgnoreCase("_import");
  }

  private void writeValuesWithDataInDirective(SkyModelNode curNode) {
    if (curNode == null || curNode.getDataInDirective().isEmpty()) {
      return;
    }
    curNode.getDataInDirective().forEach((name, values) -> {
      if (values == null) {
        System.out.println("values==null");
        return;
      }
      SkyModelAttribute attribute = curNode.getAttribute(name);
      if (attribute == null) {
        System.out.println("attribute==null");
        return;
      }

      String dataValues = String.join("|", values);
      if (attribute.getValue() == null) {
        System.out.println("attribute.getValue()==null");

        return;
      }
      String newValue = attribute.getValue().replace("value()", dataValues);

      attribute.setValue(newValue);
    });
  }

  protected SkyModelNode onStartTag(XmlPullParser xpp, File baseFolder, SkyModelNode curNode, Map<String, String> globalAttributes)
    throws Exception {
    String name = xpp.getName();
    inDataTag = isValueTag(name);
    // System.out.println("开始节点: " + name);
    String uri = xpp.getNamespace();
    if (isImportTag(name)) {
      File tgtFile = calcTargetFile(baseFolder, xpp);
      String gaSetting = getGlobalAttributeSetting(xpp);
      Map<String, String> curGlobalAttributes = new HashMap<>();
      if ("true".equalsIgnoreCase(gaSetting)) {
        curGlobalAttributes.putAll(globalAttributes);
      }
      return loadSingleXMLFile(tgtFile, curNode, curGlobalAttributes);
    }
    if (isDeleteTag(name)) {
      curNode = deleteExistingNodes(curNode, xpp);
      return curNode;
    }
    if (isValueTag(name)) {
      saveData(xpp, curNode);
      return curNode;
    }
    if (needMerge(name)) {
      return mergeNode(name, xpp, globalAttributes);
    }

    SkyModelNode newNode = new SkyModelNode();
    newNode.setName(name);

    int n = xpp.getAttributeCount();
    for (int i = 0; i < n; i++) {
      String attrName = xpp.getAttributeName(i);
      String attrValue = xpp.getAttributeValue(i);
//      if (attrValue == null
//          || attrValue.trim().isEmpty()
//          || attrValue.trim().equalsIgnoreCase("remove()")) {
//        newNode.removeAttribute(attrName);
//        continue;
//      }
      if ("root".equalsIgnoreCase(name) && (attrName.toLowerCase().startsWith("global_") || (attrName.toLowerCase().startsWith("g#")))) {
        if (attrName.toLowerCase().startsWith("global_")) {
          attrName = attrName.substring("global_".length());
        } else {
          attrName = attrName.substring("g#".length());
        }
        globalAttributes.put(attrName, attrValue);
        continue;
      }

      newNode.addAttribute(attrName, attrValue);
    }
    globalAttributes.forEach((k, v) -> {
      if (newNode.getAttribute(k) == null) {
        newNode.addAttribute(k, v);
        return;
      }
      if (newNode.getAttribute(k).getValue().equals("here()")) {
        newNode.getAttribute(k).setValue(v);
      }
    });
    Iterator<SkyModelAttribute> it = newNode.getAttributes().iterator();
    while (it.hasNext()) {
      SkyModelAttribute attr = it.next();
      String value = attr.getValue();
      if (value == null || value.isEmpty() || value.trim().equalsIgnoreCase("remove()")) {
        it.remove();
      }
    }

    curNode.addChild(newNode);
    newNode.setParent(curNode);
    return newNode;
  }

  private void saveData(XmlPullParser xpp, SkyModelNode curNode) {
    Set<String> declaredAttrNames = curNode.getAttributes().stream().filter(it -> it.getValue().contains("value()")).map(SkyModelAttribute::getName).collect(Collectors.toSet());
    if (declaredAttrNames.isEmpty()) {
      throw new RuntimeException(curNode.getName() + "中未声明任何使用value()的属性");
    }
    for (int i = 0; i < xpp.getAttributeCount(); i++) {
      String aName = xpp.getAttributeName(i);
      if (!declaredAttrNames.contains(aName)) {
        // throw new RuntimeException(curNode.getName()+"中未声明使用value()的属性\"" + aName + "\"");
        // 允许声明的value中出现未使用的字段,这个是为了新老模型兼容
        continue;
      }
      declaredAttrNames.remove(aName);
    }
    if (!declaredAttrNames.isEmpty()) {
      throw new RuntimeException(curNode.getName() + "中声明使用value()的属性\"" + declaredAttrNames + "\"未在#data中定义");
    }
    for (int i = 0; i < xpp.getAttributeCount(); i++) {
      String aName = xpp.getAttributeName(i);
      String aValue = xpp.getAttributeValue(i);

      addDataValue(aName, aValue);
    }
    curNode.addDataInDirective(this.dataInDirective);
    this.dataInDirective.clear();
  }

  private void addDataValue(String aName, String aValue) {
    List<String> values = dataInDirective.get(aName);
    if (values == null) {
      values = new LinkedList<>();
      dataInDirective.put(aName, values);
    }
    values.add(aValue);
  }

  /**
   * 删除节点, 会连带删除其下的所有节点
   *
   * @param curNode
   * @param xpp
   * @return
   * @throws Exception
   */
  protected SkyModelNode deleteExistingNodes(SkyModelNode curNode, XmlPullParser xpp)
    throws Exception {
    List<SkyModelNode> deletedNodes = new ArrayList<>();
    String attrValue = null;
    for (int i = 0; i < xpp.getAttributeCount(); i++) {
      String aName = xpp.getAttributeName(i);
      if (aName.equalsIgnoreCase("name")) {
        attrValue = xpp.getAttributeValue(i);
        break;
      }
    }
    if (attrValue == null) {
      throw new Exception("'#delete' must have a 'name' attribute ");
    }
    String[] names = attrValue.split("\\s*,\\s*");
    for (String nodeName : names) {
      deleteNode(deletedNodes, nodeName);
    }
    if (deletedNodes.contains(curNode)) {
      System.out.println(
        "Node " + curNode.getName() + " was deleted, found some node not deleted as new pin");
      for (int i = deletedNodes.size() - 1; i >= 0; i--) {
        SkyModelNode dNode = deletedNodes.get(i);
        curNode = dNode.getParent();
        if (deletedNodes.contains(curNode)) {
          continue;
        } else {
          break;
        }
      }
    }
    // 还是找不到
    if (deletedNodes.contains(curNode)) {
      System.out.println("Node " + curNode.getName() + " was deleted, use last node as new pin");
      int n = this.getProjectModel().getChildren().size();
      if (n == 0) {
        curNode = this.getProjectModel();
      } else {
        curNode = this.getProjectModel().getChildren().get(n - 1);
      }
    }
    return curNode;
  }

  private void deleteNode(List<SkyModelNode> deletedNodes, String nodeName) throws Exception {
    SkyModelNode existedNode = foundNodeByName(nodeName);
    if (existedNode == null) {
      throw new Exception("Cannot delete '" + nodeName + "': cannot found it.");
    }
    deletedNodes.add(existedNode);
    deletedNodes.addAll(existedNode.getAllDescendant());
    existedNode.getParent().getChildren().remove(existedNode);
  }

  protected String getGlobalAttributeSetting(XmlPullParser xpp) {
    int n = xpp.getAttributeCount();
    for (int i = 0; i < n; i++) {
      String aName = xpp.getAttributeName(i);
      if (aName.equalsIgnoreCase("with_global_attribute")) {
        return xpp.getAttributeValue(i);
      }
    }
    return "";
  }

  protected File calcTargetFile(File baseFolder, XmlPullParser xpp) {
    int n = xpp.getAttributeCount();
    for (int i = 0; i < n; i++) {
      String aName = xpp.getAttributeName(i);
      if (aName.equalsIgnoreCase("file")) {
        String nextFileName = xpp.getAttributeValue(i);
        return new File(FileUtils.getCleanFilePath(new File(baseFolder, nextFileName).getAbsolutePath()));
      }
    }
    throw new RuntimeException("import MUST has attribute 'file'");
  }

  protected SkyModelNode mergeNode(String name, XmlPullParser xpp, Map<String, String> globalAttributes) throws Exception {
    SkyModelNode existedNode = foundNodeByName(name);
    if (existedNode == null) {
      throw new Exception("Cannot found node '" + name + "' in loaded part");
    }
    for (int i = 0; i < xpp.getAttributeCount(); i++) {
      String attrName = xpp.getAttributeName(i);
      String attrValue = xpp.getAttributeValue(i);

      if ("root".equalsIgnoreCase(name) && (attrName.toLowerCase().startsWith("global_") || (attrName.toLowerCase().startsWith("g#")))) {
        if (attrName.toLowerCase().startsWith("global_")) {
          attrName = attrName.substring("global_".length());
        } else {
          attrName = attrName.substring("g#".length());
        }
        globalAttributes.put(attrName, attrValue);
        continue;
      }

      SkyModelAttribute attr = existedNode.getAttribute(attrName);
      if (isDeleteTag(attrName)) {
        String[] attrNames = attrValue.split("\\s*,\\s*");
        for (String an : attrNames) {
          existedNode.removeAttribute(an);
        }
      } else if (attr == null) {
        existedNode.addAttribute(attrName, attrValue);
      } else {
        attr.setValue(mergeAttrValue(attrName, attr.getValue(), attrValue));
      }
    }
    return existedNode;
  }

  protected String mergeAttrValue(String attrName, String oldValue, String newValue) {
    // if (attrName.equals("_features")) {
    // String[] all = oldValue.split(",");
    // String[] newAll = newValue.split(",");
    // List<String> together = new ArrayList<>(Arrays.asList(all));
    // for(String str: newAll) {
    // if (together.contains(str)) {
    // continue;
    // }
    // together.add(str);
    // }
    // return String.join(",", together);
    // }
    return newValue;
  }

  protected boolean needMerge(String name) {
    if (name.equalsIgnoreCase("type_definition")) {
      return false;
    }
    SkyModelNode node = foundNodeByName(name);
    return node != null;
  }

  protected SkyModelNode foundNodeByName(String name) {
    if (getProjectModel() == null || getProjectModel().getChildren() == null) {
      return null;
    }
    for (SkyModelNode node : this.getProjectModel().getChildren()) {
      SkyModelNode tgtNode = lookupNodeByName(name, node);
      if (tgtNode != null) {
        return tgtNode;
      }
    }
    return null;
  }

  protected SkyModelNode lookupNodeByName(String name, SkyModelNode node) {
    if (node.getName().equalsIgnoreCase(name)) {
      return node;
    }
    if (node.getChildren() == null) {
      return null;
    }
    for (SkyModelNode childNode : node.getChildren()) {
      SkyModelNode tgtNode = lookupNodeByName(name, childNode);
      if (tgtNode != null) {
        return tgtNode;
      }
    }
    return null;
  }

  protected String escapeValue(String value) {
    // .[apostrophe and <, & escaped],
    StringBuilder sb = new StringBuilder();
    final char quot = '"';
    final String quotEntity = "&quot;";

    int pos = 0;
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (ch == '&') {
        if (i > pos) sb.append(value.substring(pos, i));
        sb.append("&amp;");
        pos = i + 1;
      }
      if (ch == '<') {
        if (i > pos) sb.append(value.substring(pos, i));
        sb.append("&lt;");
        pos = i + 1;
      } else if (ch == quot) {
        if (i > pos) sb.append(value.substring(pos, i));
        sb.append(quotEntity);
        pos = i + 1;
      } else if (ch < 32) {
        // in XML 1.0 only legal character are #x9 | #xA | #xD
        // and they must be escaped otherwise in attribute value they are normalized to
        // spaces
        if (ch == 13 || ch == 10 || ch == 9) {
          if (i > pos) sb.append(value.substring(pos, i));
          sb.append("&#");
          sb.append(Integer.toString(ch));
          sb.append(';');
          pos = i + 1;
        } else {
          //
        }
      }
    }
    if (pos > 0) {
      sb.append(value.substring(pos));
    } else {
      sb.append(value); // this is shortcut to the most common case
    }
    return sb.toString();
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("your input is " + args.length + ":\r\n" + Arrays.asList(args));
      System.out.println("Usage:\n  java -jar XmlMerger.jar <input file> <output file>");
      return;
    }
    String inputFileName = args[0];
    String outputFileName = args[1];

    File inputFile = new File(inputFileName);
    File outputFile = new File(outputFileName);

    if (inputFile.getAbsolutePath().equalsIgnoreCase(outputFile.getAbsolutePath())) {
      outputFile =
        new File(outputFile.getParentFile(), outputFile.getName() + System.currentTimeMillis());
      System.out.println(
        "your output file is same as input, will change to " + outputFile.getAbsolutePath());
    }

    XmlMerger worker = new XmlMerger();
    worker.setXmlFile(inputFile);
    worker.loadXmlFile();
    String rstStr = worker.getXmlString();

    if (outputFile.exists()) {
    } else {
      new File(outputFile.getAbsolutePath()).getParentFile().mkdirs();
      outputFile.createNewFile();
    }

    FileWriter fw = new FileWriter(outputFile);
    fw.write(rstStr);
    fw.close();
  }
}
