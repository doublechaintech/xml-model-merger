package com.doublechaintech.xmlmerger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class FileUtils {

  public static boolean isReadableFile(File file) {
    return (file.exists() && file.isFile() && file.canRead());
  }

  public static PrintStream createFileForPrint(File file) throws IOException {
    if (!file.exists()) {
      file.getParentFile().mkdirs();
    }
    file.createNewFile();

    FileOutputStream fout = new FileOutputStream(file);
    PrintStream fprint = new PrintStream(fout);
    return fprint;
  }

  public static String readFileAsString(File inputFile) throws Exception {
    FileInputStream fin = new FileInputStream(inputFile);
    InputStreamReader reader = new InputStreamReader(fin, StandardCharsets.UTF_8);
    StringBuilder sb = new StringBuilder();
    char[] buff = new char[1024];
    int n;
    while ((n = reader.read(buff)) > 0) {
      sb.append(buff, 0, n);
    }
    fin.close();
    return sb.toString();
  }

  public static String getCleanFilePath(String filePath) {
    if (filePath == null) {
      return "";
    }
    String[] segments = filePath.split("/|\\\\");
    LinkedList<String> list = new LinkedList<>();
    for (String segment : segments) {
      if (segment.equals("..")) {
        list.removeLast();
        continue;
      }
      if (segment.equals(".")) {
        continue;
      }
      list.add(segment);
    }
    return String.join(File.separator, list);
  }
}
