package com.ztgeo.suqian.filter.Error_type;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.ztgeo.suqian.common.GlobalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;


@Component
public class ErrorFilter extends ZuulFilter {

    private static final Logger logger = LoggerFactory.getLogger(ErrorFilter.class);

    @Override
    public String filterType() {
        return "error";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        try {
            RequestContext context = RequestContext.getCurrentContext();
            ZuulException exception = this.findZuulException(context.getThrowable());
            logger.error("进入系统异常拦截");
            HttpServletResponse response = context.getResponse();
            String gx_log_id = context.get(GlobalConstants.RECORD_PRIMARY_KEY).toString();
            if (exception.getMessage().contains("Forwarding error")){
                logger.info("平台网关内部错误,请检查网络等状态");
                response.addHeader("gx_resp_code","10006");
                response.addHeader("gx_resp_logid",gx_log_id);
                response.addHeader("gx_resp_msg", URLEncoder.encode("平台网关内部错误，转发失败,请检查网络等状态","UTF-8"));
            }else {
                String caseMsg = exception.getCause().getMessage();

                String code = caseMsg.split("-")[0]; // gx_resp_code
                String msg = caseMsg.split("-")[1]; // gx_resp_msg


                String gxRespCode = response.getHeader("gx_resp_code");

                if (StringUtils.isEmpty(gxRespCode)) {
                    System.out.println("gx_resp_code字段为空！需要根据实际异常进行判断后在响应头中写入代码");
                    response.addHeader("gx_resp_code", code);
                    response.addHeader("gx_resp_logid", gx_log_id);
                    response.addHeader("gx_resp_msg", URLEncoder.encode(msg,"UTF-8"));
                    response.setContentType("application/json");
                    response.setStatus(200);
                    context.setResponseBody("");
                } else {
                    System.out.println("gx_resp_code字段不为空！不做操作，只记录zuul第一次的异常！");
                    if (gxRespCode == "10000") {
                        response.setHeader("gx_resp_code", "10006");
                    }
                }
            }
//            String gxRespLogid = context.get("gx_resp_logid").toString();
//            if(StringUtils.isEmpty(gxRespLogid)){
//                System.out.println("gx_resp_logid字段为空！表明第一个过滤器异常！需要重新写入UUID");
//                String uuid = UUID.randomUUID().toString().replaceAll("-","");
//                System.out.println("gx_resp_logid：" + uuid);
//                response.addHeader("gx_resp_logid",uuid);
//                response.setStatus(200);
//            }else{
//                System.out.println("gx_resp_logid字段不为空！不做操作，只记录zuul第一次的异常！");
//                response.addHeader("gx_resp_logid",gxRespLogid);
//                response.setStatus(200);
//            }
        } catch (Exception var5) {
            ReflectionUtils.rethrowRuntimeException(var5);
        }

        return null;
    }

    ZuulException findZuulException(Throwable throwable) {
        if (ZuulRuntimeException.class.isInstance(throwable.getCause())) {
            return (ZuulException) throwable.getCause().getCause();
        } else if (ZuulException.class.isInstance(throwable.getCause())) {
            return (ZuulException) throwable.getCause();
        } else {
            return ZuulException.class.isInstance(throwable) ? (ZuulException) throwable : new ZuulException(throwable, 500, (String) null);
        }
    }
}