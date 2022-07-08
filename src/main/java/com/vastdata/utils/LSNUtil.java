package com.vastdata.utils;

import com.vastdata.vo.LSN;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LSNUtil {
    public final static String LSN_RESULT_REGEXP = "([0-9a-fA-F]+,[0-9a-fA-F]+/[0-9a-fA-F]+)";
    public final static String LSN_VALUE_REGEXP = "[0-9a-fA-F]+";

    public static LSN parseLSN(String podName, String queryLSN){
        LSN lsn = new LSN();
        int left=0,middle=0,right=0;
        Pattern patternStr  = Pattern.compile(LSN_RESULT_REGEXP);
        Pattern patternValue  = Pattern.compile(LSN_VALUE_REGEXP);
        List<String> str = new ArrayList<String>();
        List<String> value = new ArrayList<String>();
        Matcher matcher = patternStr.matcher(queryLSN);
        while (matcher.find()){
            str.add(matcher.group());
        }
        Matcher matcherValue = patternValue.matcher(str.get(0));
        while (matcherValue.find()){
            value.add(matcherValue.group());
        }
        if(value.size()==3){
            left = hexToDecimal(value.get(0));
            middle = hexToDecimal(value.get(1));
            right = hexToDecimal(value.get(2));
        }
        lsn.setLeft(left);
        lsn.setRight(right);
        lsn.setMiddle(middle);
        lsn.setPodName(podName);
        return lsn;
    }

    /**
     * @param: [hex]
     * @return: int
     * @description: 按位计算，位值乘权重
     */
    public static int  hexToDecimal(String hex){
        int outcome = 0;
        for(int i = 0; i < hex.length(); i++){
            char hexChar = hex.charAt(i);
            outcome = outcome * 16 + charToDecimal(hexChar);
        }
        return outcome;
    }
    /**
     * @param: [c]
     * @return: int
     * @description:将字符转化为数字
     */
    public static int charToDecimal(char c){
        if(c >= 'A' && c <= 'F')
            return 10 + c - 'A';
        else
            return c - '0';
    }

    public static int getMaxLSNPod(LSN maxLsnPod, LSN lsn) {
        int result = 0;
        result = compareInt(maxLsnPod.getLeft(),lsn.getLeft());
        if(result !=0){
            return result;
        }
        result = compareInt(maxLsnPod.getMiddle(),lsn.getMiddle());
        if(result !=0){
            return result;
        }
        return  compareInt(maxLsnPod.getRight(),lsn.getRight());
    }
    
    public  static int compareInt(int a, int b){
        if (a > b) {
            return 1;
        } else if (a < b) {
            return -1;
        } else {
            return 0;
        }
    }

    //测试程序
    public static void main(String... args) {
        String content = " pg_last_xlog_replay_location \n" +
                "------------------------------\n" +
                " 0/27432E0\n" +
                "(1 row)\n";
        //将全部的小写转化为大写
        LSN lsn = parseLSN("",content);
        System.out.println(lsn.toString());

    }
}
