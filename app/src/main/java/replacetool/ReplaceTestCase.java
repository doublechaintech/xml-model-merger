package replacetool;

import java.io.IOException;

// import org.junit.Test;

public class ReplaceTestCase extends TestCaseBase {

  public void repalceFile() throws IOException {

    String content = this.loadStringFromFile("./temp/input.txt");

    content = content.replace("community", "<%=od.getBeanName()%>");
    content = content.replace("Community", "<%=od.getClassName()%>");
    content = content.replace("InvitationCode", "<%=cmrfield.getElementType()%>");
    content = content.replace("invitationCode", "<%=cmrfield.getSingleVar()%>");

    this.logln(content);
  }
  //	@Test
  public void repalceNoRef() throws IOException {

    String content = this.loadStringFromFile("./temp/input.txt");

    // content = content.replace("community", "<%=od.getBeanName()%>");
    // content = content.replace("Community", "<%=od.getClassName()%>");
    content = content.replace("InvitationCode", "<%=od.getClassName()%>");
    content = content.replace("invitationCode", "<%=od.getBeanName()%>");

    this.logln(content);
  }
}
