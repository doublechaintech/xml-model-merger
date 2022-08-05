package replacetool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

class Edge {

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  private String from;
  private String to;
  private String type;

  public static Edge build(String from, String to, String type) {
    Edge e = new Edge();
    e.setFrom(from);
    e.setTo(to);
    e.setType(type);
    return e;
  }

  public String toJson() {

    String[] values = {
      "_from",
      ":",
      "names/k" + from.hashCode(),
      ",",
      "_to",
      ":",
      "names/k" + to.hashCode(),
      ",",
      "type",
      ":",
      type
    };
    List<String> valueList = Arrays.asList(values);
    return "{"
        + valueList.stream()
            .map(
                i -> {
                  if (i.equals(":") || i.equals(",")) {
                    return i;
                  }
                  return "\"" + i + "\"";
                })
            .collect(Collectors.joining())
        + "}";
  }

  public String toG6Json() {

    // String []values= {"source",":","k"+from.hashCode(),",","target",":",
    // "k"+to.hashCode(),",","label",":",type};
    String[] values = {"source", ":", from, ",", "target", ":", to, ",", "label", ":", type};
    List<String> valueList = Arrays.asList(values);
    return "{"
        + valueList.stream()
            .map(
                i -> {
                  if (i.equals(":") || i.equals(",")) {
                    return i;
                  }
                  return "\"" + i + "\"";
                })
            .collect(Collectors.joining())
        + "},";
  }
}

class Node {

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String key;
  private String name;

  public String toJson() {

    String[] values = {"name", ":", name, ",", "_key", ":", name};
    List<String> valueList = Arrays.asList(values);
    return "{"
        + valueList.stream()
            .map(
                i -> {
                  if (i.equals(":") || i.equals(",")) {
                    return i;
                  }
                  return "\"" + i + "\"";
                })
            .collect(Collectors.joining())
        + "}";
  }

  public String toG6Json() {

    String[] values = {"label", ":", name, ",", "id", ":", name};
    List<String> valueList = Arrays.asList(values);
    return "{"
        + valueList.stream()
            .map(
                i -> {
                  if (i.equals(":") || i.equals(",")) {
                    return i;
                  }
                  return "\"" + i + "\"";
                })
            .collect(Collectors.joining())
        + "},";
  }
}

public class GraphImport {
  // Stream<String> stream = Files.lines(Paths.get(fileName))

  public static void main2(String[] args) {
    String values[] = new String[] {"是", "否"};
    Arrays.asList(values)
        .forEach(
            v -> {
              Node n = new Node();
              n.setName(v);

              System.out.println(n.toJson());
            });

    Arrays.asList(values)
        .forEach(
            v -> {
              System.out.println(Edge.build(v, v, v).toJson());
            });
  }

  public static void main(String[] args) throws IOException {

    List<String> list2 = Files.lines(Paths.get("temp/graph.txt")).collect(Collectors.toList());
    List<String> list = new ArrayList<String>();
    for (int i = 0; i < list2.size(); i += 1) {
      list.add(list2.get(i));
    }

    Set<String> namesSet = new HashSet<String>();

    list.forEach(
        line -> {
          // System.out.println(line);
          String[] valus = line.split(",");
          if (valus.length < 3) {
            return;
          }

          namesSet.add(valus[0]);
          namesSet.add(valus[1]);

          // System.out.println(Edge.build(valus[0], valus[1], valus[2]).toJson());

        });
    System.out.println(
        "arangoimport --file names.json  --collection names --create-collection true");
    namesSet.forEach(
        name -> {
          Node n = new Node();
          n.setName(name);

          System.out.println(n.toG6Json());
        });
    System.out.println();

    System.out.println(
        "arangoimport --file edges.json --collection relations --create-collection false");

    list.forEach(
        line -> {
          // System.out.println(line);
          String[] valus = line.split(",");
          if (valus.length < 3) {
            return;
          }
          System.out.println(Edge.build(valus[0], valus[1], valus[2]).toG6Json());
        });
  }
}
