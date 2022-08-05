package replacetool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
//for bbt ONLY
public class UserGenerator {

  public static void genSecUserSQL(String sufix, String mobile, String password) {

    String sql =
        String.format(
            "insert into sec_user_data(id,login,mobile,email,pwd,verification_code,verification_code_expire,last_login_time,domain,blocking,current_status,version)"
                + "values('AU%s','"
                + mobile
                + "','"
                + mobile
                + "','"
                + mobile
                + "@bbt.com','%s',874997,'2019-05-06 15:47:18','2019-05-06 15:47:18','UD000001',NULL,'INIT',1);",
            sufix,
            password);

    System.out.println(sql);
  }

  public static void genCommnunityUserSQL(String sufix, String mobile, String password) {

    String sql =
        String.format(
            "insert into community_user_data(id,mobile,nick_name,gender,user_type,avatar,birthday,experience_point,bonus_point,city,status,hide_info,administrator,community,experience_point_limit,experience_point_remain,experience_point_last_date,version)values"
                + "('CA%s','%s','笨笨','女','家长','','1989-04-01',0,0,'河南 安阳','',0,0,'C000001',200,200,'2019-04-29 16:07:20',1);",
            sufix, mobile);

    System.out.println(sql);
  }

  public static void genAppSQL(String sufix, String mobile, String password) {

    String sql =
        String.format(
            "insert into user_app_data(id,title,sec_user,app_icon,full_access,permission,object_type,object_id,location,version)values('CA%s','我的信息','AU%s','user',1,'MXWR','CommunityUser','CA%s','nolocation',1);",
            sufix, sufix, sufix);

    System.out.println(sql);
  }

  public static void genBonusSQL(String sufix, String mobile, String password) {

    String sql =
        String.format(
            "insert into bonus_point_data(id,name,obtain_time,points,user,version)"
                + "values('AP%s','新注册用户红利','2019-04-29 16:07:20',200,'CA%s',1);",
            sufix, sufix);

    System.out.println(sql);
  }

  public static void main(String[] args) {

    for (int i = 1; i <= 100; i++) {

      /*
       * insert into
       * sec_user_data(id,login,mobile,email,pwd,verification_code,verification_code_expire,
       * last_login_time,domain
       * ,blocking,current_status,version)values('SU000670','"+mobile+"','"+mobile+"','
       * "+mobile+"@bbt.com','
       * C94AA0793CA669B192DE688A7EDC5ECBC1B6E4197C3F1CD863F8247376F33773',874997,'2019-05-06
       * 15:47:18','2019-05-06 15:47:18','UD000001',NULL,'INIT',1);; insert into
       * community_user_data(id,mobile,nick_name,gender,user_type,avatar,birthday,
       * experience_point,bonus_point,
       * city,status,hide_info,administrator,community,experience_point_limit,
       * experience_point_remain,
       * experience_point_last_date,version)values('CU000722','18839715628','楷楷妈妈','女','家长',''
       * ,'1989-04-01',0,0,' 河南 安阳','',0,0,'C000001',200,200,'2019-04-29 16:07:20',1);; insert
       * into
       * bonus_point_data(id,name,obtain_time,points,user,version)values('BP004943','新注册用户红利',
       * '2019-04-29 16:07:20',200,'CU000722',1);;
       */

      String mobile = String.format("19900%06d", i);
      String sufix = String.format("%06d", i);
      String password = hashStringWithSHA256("543278", "AU" + sufix);

      genSecUserSQL(sufix, mobile, password);
      genCommnunityUserSQL(sufix, mobile, password);
      genAppSQL(sufix, mobile, password);
      genBonusSQL(sufix, mobile, password);
    }
    System.out.println("commit;");
  }

  public static String hashStringWithSHA256(String valueToHash, String salt) {

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String textToHash = valueToHash + ":" + salt;
      byte[] hash = digest.digest(textToHash.getBytes(StandardCharsets.UTF_8));
      StringBuilder stringBuilder = new StringBuilder();
      for (byte b : hash) {
        stringBuilder.append(String.format("%02X", b));
      }
      return stringBuilder.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
