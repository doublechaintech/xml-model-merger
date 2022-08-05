
package com.doublechaintech.xmlmerger;


import org.xmlpull.mxp1.MXParser;

public class CustomXmlParser extends MXParser {

  public CustomXmlParser() {
    super();
  }

  @Override
  protected boolean isNameStartChar(char ch) {
    if (ch == '#') {
      return true;
    }
    return super.isNameStartChar(ch);
  }
}