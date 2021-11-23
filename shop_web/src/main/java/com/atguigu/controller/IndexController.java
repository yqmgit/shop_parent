package com.atguigu.controller;

import com.atguigu.client.ProductFeignClient;
import com.atguigu.client.SearchFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private SearchFeignClient searchFeignClient;
    @GetMapping({"/","index.html"})
    public String index(Model model){
        RetVal retVal = productFeignClient.getIndexCategoryInfo();
        model.addAttribute("list",retVal.getData());
        return "index/index";
    }
    //商品的搜索功能
    @GetMapping("search.html")
    public String searchProduct(SearchParam searchParam,Model model){
        //通过远程调用shop-search微服务
        RetVal<Map> retVal = searchFeignClient.searchProduct(searchParam);
        //搜索到的商品基本信息 品牌集合 平台属性集合的展示
        model.addAllAttributes(retVal.getData());
        //1.搜索路径参数的回显
        String urlParam=pageUrlParam(searchParam);
        model.addAttribute("urlParam",urlParam);
        //2.页面回显品牌信息
        String brandName=pageBrandName(searchParam.getBrandName());
        model.addAttribute("brandNameParam",brandName);
        //3.页面回显平台属性信息
        List<Map<String, String>> propList=pageProps(searchParam.getProps());
        model.addAttribute("propsParamList",propList);
        //4.页面回显排序信息
        Map<String, Object> orderMap = pageSortInfo(searchParam.getOrder());
        model.addAttribute("orderMap",orderMap);


        return "search/index";
    }

    private Map<String, Object> pageSortInfo(String order) {
        Map<String, Object> orderMap = new HashMap<>();
        if(!StringUtils.isEmpty(order)){
            //order=2:desc
            String[] orderParams = order.split(":");
            if(orderParams.length==2){
                orderMap.put("type",orderParams[0]);
                orderMap.put("sort",orderParams[1]);
            }
        }else{
            //默认给一个排序方式
            orderMap.put("type",1);
            orderMap.put("sort","desc");
        }
        return orderMap;

    }

    private List<Map<String, String>> pageProps(String[] props) {
        List<Map<String, String>> propList = new ArrayList<>();
        if(props!=null&&props.length>0){
            for (String prop : props) {
                //&props=4:骁龙888:CPU型号
                String[] propParams = prop.split(":");
                if(propParams.length==3){
                    Map<String, String> propMap = new HashMap<>();
                    propMap.put("propertyKeyId",propParams[0]);
                    propMap.put("propertyValue",propParams[1]);
                    propMap.put("propertyKey",propParams[2]);
                    propList.add(propMap);
                }
            }
        }
        return propList;
    }

    private String pageBrandName(String brandName) {
        if(!StringUtils.isEmpty(brandName)){
            //&brandName=1:苹果
            String[] brandNameParams = brandName.split(":");
            if(brandNameParams.length==2){
                return "品牌:"+brandNameParams[1];
            }
        }
        return "";
    }

    //?keyword=三星&brandName=3:三星&props=4:骁龙888:CPU型号&props=5:5.0英寸以下:屏幕尺寸&order=2:asc
    private String pageUrlParam(SearchParam searchParam) {
        StringBuilder urlParam=new StringBuilder();
        //判断是否有搜索参数
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //判断是否有分类id
        if(!StringUtils.isEmpty(searchParam.getCategory1Id())){
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        if(!StringUtils.isEmpty(searchParam.getCategory2Id())){
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if(!StringUtils.isEmpty(searchParam.getCategory3Id())){
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        //判断品牌并拼接参数
        if(!StringUtils.isEmpty(searchParam.getBrandName())){
            if(urlParam.length()>0){
                //原有页面地址栏有参数
                urlParam.append("&brandName=").append(searchParam.getBrandName());
            }
        }
        //判断平台属性并拼接参数
        if(!StringUtils.isEmpty(searchParam.getProps())){
            if(urlParam.length()>0){
                //原有页面地址栏有参数
                for(String prop:searchParam.getProps()){
                    //&props=4:骁龙888:CPU型号&props=5:5.0英寸以下:屏幕尺寸
                    urlParam.append("&props=").append(prop);
                }

            }
        }
        return "search.html?"+urlParam.toString();
    }
}