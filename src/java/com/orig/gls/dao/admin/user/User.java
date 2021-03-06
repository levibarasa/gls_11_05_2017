package com.orig.gls.dao.admin.user;

import com.orig.gls.conn.AdminDb;
import com.orig.gls.prop.GlsFile;
import com.orig.gls.security.Encode;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class User {

    private static final Log log = LogFactory.getLog("origlogger");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd", Locale.getDefault());
    
    public static boolean userExists(String userName) {
        String sql = "select count(*)cnt from user_creds_tbl where user_name = ?";
        String str = AdminDb.getValue(sql, 1, 1, userName);
        return Integer.parseInt(str) > 0;
    }

    public static int getUserId(String userName) {
        String sql = "select user_id from user_creds_tbl where user_name = ?";
        String str = AdminDb.getValue(sql, 1, 1, userName);
        return Integer.parseInt(str);
    }

    public static int getRoleId(String userName) {
        String sql = "select role_id from role_profile_table where role_desc = ?";
        String str = AdminDb.getValue(sql, 1, 1, userName);
        return Integer.parseInt(str);
    }

    public static int addUserDetails(String userName, String roleId, String userPw, int numPwdHistory, String pwdHistory, int numPwdAttempts, String newUserFlg, int acctInactiveDays, String rcreUserId, String solId) {
        String sql = "insert into user_creds_tbl(ACCT_EXPY_DATE,ACCT_INACTIVE_DAYS,DISABLED_FROM_DATE,DISABLED_UPTO_DATE,"
                + "LAST_ACCESS_TIME,NEW_USER_FLG,NUM_PWD_ATTEMPTS,NUM_PWD_HISTORY,PW_EXPY_DATE,PWD_HISTORY,ROLE_ID,"
                + "USER_NAME,USER_PW,SOL_ID,USER_STATUS,LCHG_USER_ID) values(try_convert(date, ?, 111),?,"
                + "try_convert(date, ?, 111) ,try_convert(date, ?, 111),try_convert(date, ?, 111) ,?,?,?,"
                + "try_convert(date, ?, 111),?,?,?,?,?,?,?)";
        String disabledFromDate = getfutureDateString("Year", 2);
        String disabledUptoDate = getfutureDateString("Year", 3);
        String pwExpyDate = getfutureDateString("Month", 3);
        String acctExpyDate = getfutureDateString("Month", 3);
        String lastAccessTime = getfutureDateString("Month", 0);
        int role = getRoleId(roleId);
        String userN = userName.trim();
        String passwD = userPw.trim();
        String in =   acctExpyDate + "," + acctInactiveDays + "," + disabledFromDate + "," +  disabledUptoDate + "," + lastAccessTime + "," + newUserFlg + "," + numPwdAttempts + "," + numPwdHistory + "," + pwExpyDate + "," + pwdHistory + "," + role + "," + userName + "," + EncodeUserPassword(userN, passwD) + "," + solId + ",U," + rcreUserId;
        return AdminDb.dbWork(sql, 16, in);
    }
 
    public static ArrayList getAllVerifiedUsers() {
        String sql = "select user_name, user_id, role_id from user_creds_tbl";
        return AdminDb.execArrayLists(sql, 0, "", 3);
    }

    
    public static ArrayList getAllUsers(String uname) {
        String sql = "select user_name, user_id, role_id from user_creds_tbl where user_name <> ? and user_status =?";
        String in = uname + ",A";
        return AdminDb.execArrayLists(sql, 2, in, 3);
    }

    public static ArrayList getAvailableUsers() {
        String sql = "select user_name, role_id,user_id from user_creds_tbl where user_status = ?";
        return AdminDb.execArrayLists(sql, 1, "A", 3);
    }

    public static ArrayList getUnverifiedUsers(String username) {
        String sql = "select user_name, role_id,user_id from user_creds_tbl_mod where rcre_user_id <> ?";
        return AdminDb.execArrayLists(sql, 1, username, 3);
    }

    
    
    public static ArrayList getUnverifiedUsers() {
        String sql = "select user_name, user_id,role_id from user_creds_tbl_mod";
        return AdminDb.execArrayLists(sql, 0, "", 3);
    }

    public static ArrayList getUserDetails(String in) {
        String sql = "select user_name, role_id, new_user_flg, user_status, disabled_from_date, disabled_upto_date, pw_expy_date, acct_expy_date,NUM_PWD_HISTORY,SOL_ID from user_creds_tbl where user_id = ?";
        return AdminDb.execArrayLists(sql, 1, in, 10);
    }

   
    public static String lastOper(String in) {
        String sql = "select last_oper from user_creds_tbl_mod where user_id = ?";
        return AdminDb.getValue(sql, 1, 0, in);
    }

    public static boolean addUserModDetails(int userId, String userName, String roleId, String userPw, int numPwdHistory, String pwdHistory, int numPwdAttempts, String newUserFlg, int acctInactiveDays, String lastOper, String rcreUserId, String mType) {
        String disabledFromDate = "";
        String disabledUptoDate = "";
        String pwExpyDate = "";
        String acctExpyDate = "";
        String lastAccessTime = "";
        switch (mType) {
            case "Modify":
                disabledFromDate = getfutureDateString("Year", 2);
                disabledUptoDate = getfutureDateString("Year", 3);
                pwExpyDate = getfutureDateString("Month", 3);
                acctExpyDate = getfutureDateString("Month", 3);
                lastAccessTime = getfutureDateString("Month", 0);
                break;
            case "Delete":
                disabledFromDate = getfutureDateString("Year", 0);
                disabledUptoDate = getfutureDateString("Year", 0);
                pwExpyDate = getfutureDateString("Month", 0);
                acctExpyDate = getfutureDateString("Month", 0);
                lastAccessTime = getfutureDateString("Month", 0);
                break;
            case "Restore":
                break;
        }

       //1,3,4,5,9,
        String sql = "insert into user_creds_tbl_mod(ACCT_EXPY_DATE,ACCT_INACTIVE_DAYS,DISABLED_FROM_DATE,"
                + "DISABLED_UPTO_DATE,LAST_ACCESS_TIME,NEW_USER_FLG,NUM_PWD_ATTEMPTS,NUM_PWD_HISTORY,"
                + "PW_EXPY_DATE,PWD_HISTORY,ROLE_ID,USER_ID,USER_NAME,USER_PW,LAST_OPER,RCRE_USER_ID) "
                + "values(try_convert(date, ?, 111) ,?,try_convert(date, ?, 111) ,try_convert(date, ?, 111) "
                + ",try_convert(date, ?, 111),?,?,?,try_convert(date, ?, 111) ,?,?,?,?,?,?,?)";
        int role = getRoleId(roleId);
        Random randomGenerator = new Random();
//        int randomInt = randomGenerator.nextInt(1000000);
//        String formatedInt;
//        formatedInt = String.format("%06d", randomInt);
        
        String in =  acctExpyDate + "," + acctInactiveDays + "," + disabledFromDate + "," + disabledUptoDate + "," + lastAccessTime + "," + newUserFlg + "," + numPwdAttempts + "," + numPwdHistory + "," + pwExpyDate + "," + pwdHistory + "," + role + "," + userId + "," + userName + "," + EncodeUserPassword(userName, userPw) + "," + lastOper + "," + rcreUserId;
        return AdminDb.dbWork(sql, 16, in) > 0;

    }
//    //"User", "DBA", "user1234", 0, "12", 0, "Y", 0, "Eliud", "001"
//    public static void main(String[] args) {
//        System.out.println( addUserModDetails(1154, "User", "DBA",
//        "user1234", 0, "12", 
//         0, "Y", 0, 
//                "C", "Eliud", "Modify"));
//    }

    public static void verifyUser(int userId) {
        String sql = "delete from user_creds_mod where user_id = ?";
        AdminDb.dbWork(sql, 1, String.valueOf(userId));
    }

    public static void deleteAfterReg(String userName) {
        String sql = "delete from finacle_users where user_name = ?";
        AdminDb.dbWork(sql, 1, userName);
    }

     public static ArrayList getUsersList() {
        String sql = "SELECT  USER_NAME ,SOL_ID,ROLE_DESC FROM  FINACLE_USERS";
        return AdminDb.execArrayLists(sql, 0, "", 3);
    }
      
    public static ArrayList getFileUsersList() {
        ArrayList arr = new ArrayList();
        GlsFile pr = new GlsFile();
        String uploadFilepath = pr.getDBProperty().getProperty("user.file");
        String line;
        try {
            FileInputStream fs = new FileInputStream(uploadFilepath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fs));
            String[] split;
            while ((line = br.readLine()) != null) {
                split = line.split("\\|");
                ArrayList one = new ArrayList();
                one.add(split[0]);
                one.add(split[1]);
                one.add(split[2]);
                arr.add(one);
            }
            fs.close();
			br.close();
        } catch (Exception asd) {
            log.debug(asd.getMessage());
        }
        return arr;
    } 

    public static void deleteUser(int userId) {
        String sql = "update user_creds_tbl set user_status = ? where user_id = ?";
        String in = "D," + userId;
        AdminDb.dbWork(sql, 2, in);
    }

   
    public static void changePassword(String username, String passwrd) {
         String disabledFromDate = getfutureDateString("Year", 2);
        String disabledUptoDate = getfutureDateString("Year", 3);
        String pwExpyDate = getfutureDateString("Month", 3);
        String acctExpyDate = getfutureDateString("Month", 3);
        String rcre_time = getfutureDateString("Month", 0);
        String pass = EncodeUserPassword(username, passwrd);
       
        String in = disabledFromDate + "," + disabledUptoDate + "," + pwExpyDate +
                "," + acctExpyDate  +",N," + pass + "," + username;
        String sql = "update user_creds_tbl set disabled_from_date = try_convert(date, ?, 111)"
                + ", disabled_upto_date = try_convert(date, ?, 111) , pw_expy_date = try_convert(date, ?, 111) "
                + ", acct_expy_date = try_convert(date, ?, 111), last_access_time = try_convert(date, ?, 111)"
                + " , new_user_flg = 'N', user_pw = ? where user_name =?";
       
        if((AdminDb.dbWork(sql, 7, in)) > 0){
         }
          }

    public static void modifyUser(int userId, String username) {
        String sql = "select role_id, new_user_flg from user_creds_tbl_mod where user_id=?";
        String str = AdminDb.getValue(sql, 3, 1, String.valueOf(userId));
        String[] args = str.split("\\s*,\\s*");
        String in = args[0] + "," + args[1] + "," + username;
        String sq = "update user_creds_tbl set role_id = ?, new_user_flg where user_id=?";
        AdminDb.dbWork(sq, 3, in);
    }

    public static void markUserUnverified(int userName, String userStatus) {
        String sql = "update user_creds_tbl set user_status = ?, new_user_flg =? where user_id = ?";
        String in = userStatus + ",Y," + userName;
        AdminDb.dbWork(sql, 3, in);
    }

    public static boolean userExistsInMod(String userName) {
        String sql = "select count(*)cnt from user_creds_tbl_mod where user_name=?";
        String str = AdminDb.getValue(sql, 1, 1, userName);
        return Integer.parseInt(str) > 0;
    }
    static Encode enc;

    public static String generateUserKey(String username, String password) {
        return username.substring(0, 2) + password.substring(0, 4);
    }

    public static String generateUserIV(String username, String password) {
        return username.substring(0, 3) + password.substring(0, 5);
    }

    public static String EncodeUserPassword(String username, String password) {
        enc = new Encode(generateUserKey(username, password), generateUserIV(username, password));
        return enc.encrypt(password);
    }

    public String DecodeUserPassword(String username, String password, String encpass) {
        enc = new Encode(generateUserKey(username, password), generateUserIV(username, password));
        return enc.decrypt(encpass);
    }

    public static ArrayList getRoles() {
        String sql = "select role_desc from role_profile_table";
        return AdminDb.execArrayLists(sql, 0, "", 1);
    }
    
    
    private static String getfutureDateString(String intType, int months) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        DateTime dt = new DateTime();
        if (months > 0) {
            if (intType.equalsIgnoreCase("Year")) {
                dt = dt.plusYears(months);
            } else {
                dt = dt.plusMonths(months);
            }
        } else {
            dt = new DateTime();
        }
        return dt.toString(fmt);
    }
    
//    public static void main(String[] args) {
//        
//         String pass = EncodeUserPassword("LEVI", "pass1234");
//        System.out.println("pass:     "+pass+"    yea");
//    }
}
