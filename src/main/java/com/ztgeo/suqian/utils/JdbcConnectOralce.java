package com.ztgeo.suqian.utils;

public class JdbcConnectOralce {
    //静态方法执行之前就已经加载了驱动程序
    static{
        try {
//          Class.forName("com.mysql.cj.jdbc.Driver");//mysql连接驱动官方新版本中驱动的加载转移到另一个包下
            Class.forName("oracle.jdbc.OracleDriver");//这里使用的是oracle数据库驱动，待会要和Oracle进行交互
            System.out.println("loading Success!!!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
