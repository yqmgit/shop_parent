package com.atguigu.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AccessFilter implements GlobalFilter {

//匹配路径对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Autowired
    private RedisTemplate redisTemplate;
    @Value("${filter.whiteList}")
    private String filterWhiteList;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (antPathMatcher.match("/sku/**",path)){
            //写数据告诉访问端，没有权限访问
            return writeDataToBrowser(exchange, RetValCodeEnum.NO_PERMISSION);
        }

        //用户未登录，提醒用户登录
        String userId = getUserId(request);

        if ("-1".equals(userId)){
            //告诉访问端，没有权限
            return writeDataToBrowser(exchange,RetValCodeEnum.IP_WARNING);
        }
        //对于某些资源 必须登录，如我的订单，我的购物车等
        if ((antPathMatcher.match("/order/**",path))&&(userId==null)){
            //告诉访问端，没有登录
            return writeDataToBrowser(exchange,RetValCodeEnum.NO_LOGIN);
        }

        //请求白名单（order.html）必须要登录
        for (String filterWhite : filterWhiteList.split(",")) {
            if (!StringUtils.isEmpty(filterWhite)){
                if (path.indexOf(filterWhite)!=-1&&StringUtils.isEmpty(userId)){
                    //跳转到登录页面
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    //重定向到登录页面
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://passport.gmall.com/login.html?originalUrl="+request.getURI());
                    return response.setComplete();
                }
            }
        }

        //拿临时 userTempId
        String userTempId = getUserTempId(request);

        //将id保存在header中，跨域传递
        if (!StringUtils.isEmpty(userId)||!StringUtils.isEmpty(userTempId)){
            if (!StringUtils.isEmpty(userId)){
                request.mutate().header("userId",userId).build();
            }
            if (!StringUtils.isEmpty(userTempId)){
                request.mutate().header("userTempId",userTempId).build();
            }
            //过滤器放开拦截 让下游继续执行(此时exchange里面的header做了修改)
            return chain.filter(exchange.mutate().request(request).build());
        }

        //放开拦截器，继续执行
        return chain.filter(exchange);
    }



    private String getUserTempId(ServerHttpRequest request) {
        //从header中拿id
        List<String> headerTempIdList = request.getHeaders().get("userTempId");
        String userTempId = null;
        if (!CollectionUtils.isEmpty(headerTempIdList)) {
            userTempId = headerTempIdList.get(0);
        } else {
            //从cookie中拿id
            HttpCookie tempId = request.getCookies().getFirst("userTempId");
            if (tempId != null) {
                userTempId = tempId.getValue();
            }
        }
        return userTempId;
    }

    private String getUserId(ServerHttpRequest request) {
        //从header中拿token
        List<String> headerTokenList = request.getHeaders().get("token");
        String token = null;
        if (!CollectionUtils.isEmpty(headerTokenList)){
            token = headerTokenList.get(0);
        }else {
            //从cookie中拿token
            HttpCookie cookie = request.getCookies().getFirst("token");
            if (cookie!=null){
                 token = cookie.getValue();
            }
        }
        //拿到了token，比较当前登录的ip和redis 中的token的ip 是否相同
        if (!StringUtils.isEmpty(token)){
            //从redis 中拿
            String userKey = "user:login:"+token;
            String loginInfoJson = (String) redisTemplate.opsForValue().get(userKey);
            JSONObject loginInfoObject = JSONObject.parseObject(loginInfoJson);
            //redis缓存中的token中的ip和当前登录的主机ip是否相同，判断是否异地登录
            //redis 中token 的ip
            String loginIp = loginInfoObject.getString("loginIp");
            //拿当前ip地址
            String gatwayIpAddress = IpUtil.getGatwayIpAddress(request);
            if (gatwayIpAddress.equals(loginIp)){
                return loginInfoObject.getString("userId");
            }else {
                //当前ip地址有问题
                return "-1";
            }
        }
        //cookie和header中都没有token 说明用户未登录，userId为空
        return null;
    }

    private Mono<Void> writeDataToBrowser(ServerWebExchange exchange, RetValCodeEnum retValCodeEnum) {
        ServerHttpResponse response = exchange.getResponse();
        RetVal<Object> retVal = RetVal.build(null, retValCodeEnum);
        //转成json字符串传送
        byte[] retBytes = JSONObject.toJSONString(retVal).getBytes(StandardCharsets.UTF_8);
        //提高效率，将字节转成一个数据的buffer
        DataBuffer buffer = response.bufferFactory().wrap(retBytes);
        //设置返回给浏览器的请求头，返回类型是json
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        //把数据写到浏览器
        return response.writeWith(Mono.just(buffer));
    }
}
