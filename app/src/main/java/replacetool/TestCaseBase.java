package replacetool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TestCaseBase {

  protected void logln(byte b) {
    // TODO Auto-generated method stub
    System.out.printf("%02XH\n", b);
  }

  protected void logln(int[] intArray) {
    // TODO Auto-generated method stub
    for (int i = 0; i < intArray.length; i++) {
      if (i > 0) System.out.print(" ");
      System.out.printf("%08X", intArray[i]);
    }
    System.out.println();
  }

  protected void logln(int value) {
    // TODO Auto-generated method stub

    System.out.printf("%08X", value);

    System.out.println();
  }

  protected void logln(byte bs[]) {
    // TODO Auto-generated method stub
    for (int i = 0; i < bs.length; i++) {
      if (i > 0) System.out.print(" ");
      System.out.printf("%02X", bs[i]);
    }
    System.out.println();
  }

  protected void log(String string) {
    // TODO Auto-generated method stub
    System.out.print(string);
  }

  protected void logln() {
    // TODO Auto-generated method stub
    System.out.println();
  }

  protected void logln(String string) {
    // TODO Auto-generated method stub
    System.out.println(string);
  }

  protected void format(String format, Object... args) {
    // TODO Auto-generated method stub
    System.out.format(format, args);
  }

  protected String loadStringFromFile(String fileName) throws IOException {

    StringBuffer stringBuffer = new StringBuffer();

    BufferedReader in = new BufferedReader(new FileReader(fileName));
    String str;
    while ((str = in.readLine()) != null) {
      stringBuffer.append(str);
      stringBuffer.append(newLine());
    }
    in.close();

    return stringBuffer.toString();
  }

  public static String toHexString(byte[] fieldData) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fieldData.length; i++) {
      int v = (fieldData[i] & 0xFF);
      if (v <= 0xF) {
        sb.append("0");
      }
      sb.append(Integer.toHexString(v));
    }
    return sb.toString();
  }

  public static String getKeyedDigest(String stringToDigist)
      throws NoSuchAlgorithmException, UnsupportedEncodingException {

    if (stringToDigist == null) {
      throw new IllegalArgumentException("the password can not be null");
    }

    if (stringToDigist.trim().length() == 0) {
      throw new IllegalArgumentException(
          "the password can not be empty or can not be emptry after trimed.");
    }

    byte[] utf16leBytes = stringToDigist.getBytes(StandardCharsets.UTF_16LE);
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    md5.update(utf16leBytes);
    byte[] digistedBytes = md5.digest();
    return toHexString(digistedBytes);
  }

  protected String[] loadStringFromFiles(String[] fileNames) throws IOException {

    String ret[] = new String[fileNames.length];

    for (int i = 0; i < fileNames.length; i++) {

      ret[i] = loadStringFromFile(fileNames[i]);
    }
    return ret;
  }

  protected String joinStringArray(String strArray[]) throws IOException {
    StringBuffer stringBuffer = new StringBuffer();
    for (int i = 0; i < strArray.length; i++) {
      String element = strArray[i];
      if (i > 0) {
        stringBuffer.append(",");
      }
      stringBuffer.append(element);
    }
    return stringBuffer.toString();
  }

  protected String newLine() {
    return System.lineSeparator();
  }
}
