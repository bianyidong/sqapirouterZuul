package com.ztgeo.suqian.filter.Pre_type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.GlobalConstants;
import com.ztgeo.suqian.dao.AGShareDao;
import com.ztgeo.suqian.entity.ag_datashare.*;
import com.ztgeo.suqian.utils.HttpUtilsAll;
import com.ztgeo.suqian.utils.StringUtils;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class SqlConfigReqFilter extends ZuulFilter {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private String api_id;
    @Resource
    private AGShareDao agShareDao;


    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        String className = this.getClass().getSimpleName();
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        api_id = request.getHeader("api_id");
        int count = agShareDao.countApiUserFiltersByFilterBcEqualsAndApiIdEquals(className, api_id);
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object run() throws ZuulException {
        Object result = null;
        log.info("-------------开始---进入Sql配置接口过滤器-------------");
        RequestContext requestContext = RequestContext.getCurrentContext();
        try {
            HttpServletRequest request = requestContext.getRequest();
            InputStream in = request.getInputStream();
            String requestBody = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
//            JSONObject jsonObject = JSONObject.parseObject(requestBody);
            String api_id = request.getHeader("api_id");
            result = respSult(api_id, requestBody);
            int isExist = agShareDao.countApiParentChildByApiParenttableidEquals(api_id);
            //判断是否存在主子表
            if (isExist == 0) {
                result = result;
            } else {
                if (!StringUtils.isBlank(result.toString())) {
                    if (isjson(result.toString())) {
                        List<ApiParentChild> apiParentChildren = agShareDao.findApiParentChildByApiParenttableidEquals(api_id);
                        JSONObject js = JSONObject.parseObject(result.toString());
                        for (int i = 0; i < apiParentChildren.size(); i++) {
                            js.put(apiParentChildren.get(i).getChildKeyname(), respSult(apiParentChildren.get(i).getChildTableid(), result.toString()));
                        }
                        result = JSONObject.toJSONString(js, SerializerFeature.WriteNullStringAsEmpty);

                    } else {
                        JSONArray jsonArray = JSONArray.parseArray(result.toString());
                        int d = jsonArray.size();
//                    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            List<ApiParentChild> apiParentChildren = agShareDao.findApiParentChildByApiParenttableidEquals(api_id);
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            for (int j = 0; j < apiParentChildren.size(); j++) {
                                jsonObject.put(apiParentChildren.get(j).getChildKeyname(), respSult(apiParentChildren.get(j).getChildTableid(), jsonObject.toJSONString()));
                            }

                            System.out.println(jsonObject);
                        }
                        result = jsonArray;
                        System.out.println(jsonArray);

                    }
                } else {
                    result = result;
                }

            }


            requestContext.setResponseBody(result.toString());
            requestContext.set(GlobalConstants.ISSUCCESS, "success");
            requestContext.setSendZuulResponse(false);
        } catch (Exception e) {
            log.info("SQL配置请求过滤器异常", e);
            log.info("-------------结束---Sql配置接口-------------");
            throw new RuntimeException("30018-" + e.getMessage() + "SQL配置过滤器异常");
        }
        return null;

    }

    //jdbc查询数据并
    private Object respSult(String api_id, String requestBody) {
        Connection connect = null;
        Statement statement = null;
        ResultSet resultSet = null;
        Object sqresult = "";
        JSONObject jsonObject = JSON.parseObject(requestBody);
        try {
            ApiSqlConfigInfo apiSqlConfigInfo = agShareDao.findApiSqlConfigInfosByApiId(api_id).get(0);
            if ("Oracle".equals(apiSqlConfigInfo.getDbLx())) {
                //第一步：注册驱动
                Class.forName("oracle.jdbc.OracleDriver");//oracle
                //第二步：获取连接
                connect = DriverManager.getConnection("jdbc:oracle:thin:@" + apiSqlConfigInfo.getDbIp() + ":" + apiSqlConfigInfo.getDbName(), apiSqlConfigInfo.getDbUsername(), apiSqlConfigInfo.getDbPassword());//oracle
            } else if ("MySQL".equals(apiSqlConfigInfo.getDbLx())) {
                //第一步：注册驱动
                Class.forName("com.mysql.jdbc.Driver");
                //第二步：获取连接
                connect = DriverManager.getConnection("jdbc:mysql://" + apiSqlConfigInfo.getDbIp() + "/" + apiSqlConfigInfo.getDbName() + "?useUnicode=true&characterEncoding=UTF-8", apiSqlConfigInfo.getDbUsername(), apiSqlConfigInfo.getDbPassword());
            } else if ("SQL Server".equals(apiSqlConfigInfo.getDbLx())) {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                connect = DriverManager.getConnection("jdbc:sqlserver://" + apiSqlConfigInfo.getDbIp() + ";DatabaseName=" + apiSqlConfigInfo.getDbName(), apiSqlConfigInfo.getDbUsername(), apiSqlConfigInfo.getDbPassword());
            }
            //第三步：获取执行sql语句对象
            String sql = apiSqlConfigInfo.getDbSql();
            List<Apisqlwherefield> apisqlwherefieldList = agShareDao.findApisqlwherefieldsByApiIdOrderByFieldorder(api_id);
            for (int i = 0; i < apisqlwherefieldList.size(); i++) {
                {
                    Apisqlwherefield apisqlwherefield = apisqlwherefieldList.get(i);
                    jsonObject.get(apisqlwherefield.getTablefield());
                }
            }
            //条件空时的处理方法  https://www.iteye.com/blog/free-zhou-671193
            PreparedStatement preState = connect.prepareStatement(sql);

            for (int i = 1; i <= apisqlwherefieldList.size(); i++) {
                Apisqlwherefield apisqlwherefield = apisqlwherefieldList.get(i - 1);
                if ("like".equals(apisqlwherefield.getFieldtype())) {
                    Object va = jsonObject.get(apisqlwherefield.getTablefield());
                    preState.setObject(i, "%" + va + "%");
                } else {
                    Object va = jsonObject.get(apisqlwherefield.getTablefield());
                    preState.setObject(i, va);
                }
            }
            //第四步：执行sql语句
            resultSet = preState.executeQuery();

            ResultSetMetaData data = resultSet.getMetaData();
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

            //第五步：处理结果集
            while (resultSet.next()) {
                JSONObject json = new JSONObject();
                for (int i = 1; i <= data.getColumnCount(); i++) {
                    int columnCount = data.getColumnCount();
                    String columnName = data.getColumnName(i);
                    String columnValue = resultSet.getString(i);
                    json.put(columnName, columnValue == null ? "" : columnValue);
                }
                System.out.println("s" + json);
                //判断返回是数组还是对象,如果是对象直接显示第一条数据
                if ("1".equals(apiSqlConfigInfo.getDbRestype())) {
                    sqresult = JSONObject.toJSONString(json, SerializerFeature.WriteNullStringAsEmpty);
                    break;
                } else {
                    list.add(json);
                    sqresult = list;
                }

            }
            if (!StringUtils.isBlank(apiSqlConfigInfo.getDbAllowmostrownum()) && !"1".equals(apiSqlConfigInfo.getDbRestype())) {
                if (Integer.parseInt(apiSqlConfigInfo.getDbAllowmostrownum()) < list.size()) {
                    sqresult = list.subList(0, Integer.parseInt(apiSqlConfigInfo.getDbAllowmostrownum()));
                } else {
                    sqresult = list;
                }

            } else if (isjson(sqresult.toString())) {
                sqresult = sqresult;
            } else {
                sqresult = list;
            }

        } catch (Exception e) {
            throw new RuntimeException("执行sql查询信息异常");
        } finally {
            //：关闭资源
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connect != null) connect.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return sqresult;
    }

    //判断是否是json对象
    private boolean isjson(String isjson) {
        try {
            JSONObject jsonStr = JSONObject.parseObject(isjson);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
