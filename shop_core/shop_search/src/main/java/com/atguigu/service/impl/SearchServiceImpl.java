package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.dao.ProductRepository;
import com.atguigu.entity.*;
import com.atguigu.search.*;
import com.atguigu.service.SearchService;
import lombok.SneakyThrows;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    ProductFeignClient productFeignClient;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public void onSale(Long skuId) {
        //创建product对象存储上架商品的所有信息
        Product product = new Product();
        //上架基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo != null) {
            product.setId(skuInfo.getId());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setProductName(skuInfo.getSkuName());
            product.setCreateTime(new Date());
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
        }
        //品牌信息
        BaseBrand brand = productFeignClient.getBrandById1(skuInfo.getBrandId());
        if (brand != null) {
            product.setBrandId(brand.getId());
            product.setBrandName(brand.getBrandName());
            product.setBrandLogoUrl(brand.getBrandLogoUrl());
        }

        //商品分类信息
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        if (categoryView != null) {
            product.setCategory1Id(categoryView.getCategory1Id());
            product.setCategory1Name(categoryView.getCategory1Name());
            product.setCategory2Id(categoryView.getCategory2Id());
            product.setCategory2Name(categoryView.getCategory2Name());
            product.setCategory3Id(categoryView.getCategory3Id());
            product.setCategory3Name(categoryView.getCategory3Name());
        }

        //平台属性信息
        List<PlatformPropertyKey> platformPropertyList = productFeignClient.getPlatformPropertyBySkuId(skuInfo.getId());
        if (!CollectionUtils.isEmpty(platformPropertyList)) {
            List<SearchPlatformProperty> searchPlatformProperties = platformPropertyList.stream().map(platformProperty -> {
                SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();

                searchPlatformProperty.setPropertyKeyId(platformProperty.getId());
                searchPlatformProperty.setPropertyKey(platformProperty.getPropertyKey());
                PlatformPropertyValue platformPropertyValue = platformProperty.getPropertyValueList().get(0);
                searchPlatformProperty.setPropertyValue(platformPropertyValue.getPropertyValue());
                return searchPlatformProperty;
            }).collect(Collectors.toList());
            product.setPlatformProperty(searchPlatformProperties);
        }
        //把上面的值放入es中
        productRepository.save(product);
    }

    @Override
    public void offSale(Long skuId) {
        productRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        //定义一个属性key的名称
        String hotKey = "sku:hotscore";
        double count = redisTemplate.opsForZSet().incrementScore(hotKey,skuId,1);
        //加到一定次数修改es中的值
        if (count%6==0){
            Optional<Product> optional = productRepository.findById(skuId);
            Product esProduct = optional.get();
            esProduct.setHotScore(Math.round(count));
            productRepository.save(esProduct);

        }
    }

    @SneakyThrows
    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) {
        //1.生成DSL查询语句
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);
        //2.通过该语句实现查询
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //3.将查询结果封装到某个对象当中
        SearchResponseVo searchResponseVo = this.parseSearchResult(searchResponse);
        //4.其他返回参数的赋值
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        //5.设置总页数
        boolean addPageFlag=searchResponseVo.getTotal()%searchParam.getPageSize()==0;
        long totalPage=0;
        if(addPageFlag){
            totalPage=searchResponseVo.getTotal()/searchParam.getPageSize();
        }else{
            totalPage=searchResponseVo.getTotal()/searchParam.getPageSize()+1;
        }
        searchResponseVo.setTotalPages(totalPage);
        return searchResponseVo;

    }

    //通过该语句实现查询
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //1.拿到商品的基本信息
        SearchHits firstHists = searchResponse.getHits();
        //设置总的个数
        searchResponseVo.setTotal(firstHists.totalHits);
        SearchHit[] secondHits = firstHists.getHits();
        //存储基本信息的对象
        List<Product> productList = new ArrayList<>();
        if(secondHits!=null&&secondHits.length>0){
            for (SearchHit secondHit : secondHits) {
                //把json转换为product对象
                Product product = JSONObject.parseObject(secondHit.getSourceAsString(), Product.class);
                //拿到高亮信息
                HighlightField highlightField = secondHit.getHighlightFields().get("productName");
                if(highlightField!=null){
                    Text highlightProductName = highlightField.getFragments()[0];
                    product.setProductName(highlightProductName.toString());
                }
                productList.add(product);
            }
            searchResponseVo.setProductList(productList);
        }
        //2.拿到品牌信息
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        List<SearchBrandVo> searchBrandVoList = brandIdAgg.getBuckets().stream().map(bucket -> {
            SearchBrandVo searchBrandVo = new SearchBrandVo();
            //拿品牌的id
            String brandId = bucket.getKeyAsString();
            searchBrandVo.setBrandId(Long.parseLong(brandId));
            //拿品牌的名称
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandName(brandName);
            //拿品牌的logo
            ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
            String brandLogoUrl = brandLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandLogoUrl(brandLogoUrl);
            return searchBrandVo;
        }).collect(Collectors.toList());
        searchResponseVo.setBrandVoList(searchBrandVoList);
        //3.拿到平台属性信息
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        //拿嵌套agg的子agg
        ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<? extends Terms.Bucket> buckets = propertyKeyIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(buckets)){
            //迭代buckets
            List<SearchPlatformPropertyVo> platformPropertyVoList = buckets.stream().map(bucket -> {
                SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
                //平台属性id
                Number propertyKeyId = bucket.getKeyAsNumber();
                searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId.longValue());
                //平台属性名称
                ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
                String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
                searchPlatformPropertyVo.setPropertyKey(propertyKey);
                //平台属性值
                ParsedStringTerms propertyValueAgg = bucket.getAggregations().get("propertyValueAgg");
                List<? extends Terms.Bucket> propertyValueBuckets = propertyValueAgg.getBuckets();
                //拿到平台属性值的集合
                List<String> propertyValueList = propertyValueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                searchPlatformPropertyVo.setPropertyValueList(propertyValueList);
                return searchPlatformPropertyVo;
            }).collect(Collectors.toList());
            searchResponseVo.setPlatformPropertyList(platformPropertyVoList);
        }
        return searchResponseVo;


    }

    //生成DSL查询语句(SQL)
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //1.构造第一个bool
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //2.构造分类过滤器
        if(!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //构造一级分类id过滤器
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            firstBool.filter(category1Id);
        }
        if(!StringUtils.isEmpty(searchParam.getCategory2Id())){
            //构造二级分类id过滤器
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            firstBool.filter(category2Id);
        }
        if(!StringUtils.isEmpty(searchParam.getCategory3Id())){
            //构造三级分类id过滤器
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            firstBool.filter(category3Id);
        }
        //3.构造品牌过滤器 参数格式brandName=1:苹果
        String brandName = searchParam.getBrandName();
        if(!StringUtils.isEmpty(brandName)){
            String[] brandParam = brandName.split(":");
            if(brandParam.length==2){
                firstBool.filter(QueryBuilders.termQuery("brandId",brandParam[0]));
            }
        }
        //4.构造平台属性过滤器 参数格式 props=4:骁龙888:CPU型号&props=5:6.55-6.64英寸:屏幕尺寸
        String[] props = searchParam.getProps();
        if(props!=null&&props.length>0){
            for (String prop : props) {
                //4:骁龙888:CPU型号
                String[] platformParams = prop.split(":");
                if(platformParams.length==3){
                    BoolQueryBuilder boolQuery=QueryBuilders.boolQuery();
                    //构造内部bool
                    BoolQueryBuilder childBoolQuery=QueryBuilders.boolQuery();
                    childBoolQuery.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", platformParams[0]));
                    childBoolQuery.must(QueryBuilders.termQuery("platformProperty.propertyValue", platformParams[1]));
                    boolQuery.must(QueryBuilders.nestedQuery("platformProperty",childBoolQuery, ScoreMode.None));
                    firstBool.filter(boolQuery);
                }
            }
        }
        //5.构造商品名称搜索关键词查询 参数格式 keyword=苹果
        String keyword = searchParam.getKeyword();
        if(!StringUtils.isEmpty(keyword)){
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("productName", keyword).operator(Operator.AND);
            firstBool.must(matchQuery);
        }
        //6.构造query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(firstBool);
        //7.构造分页 起始页(pageNo-1)*pageSize
        int from=(searchParam.getPageNo()-1)*searchParam.getPageSize();
        sourceBuilder.from(from);
        sourceBuilder.size(searchParam.getPageSize());
        /**
         * 8.构造排序  参数格式 order=2:desc
         * 1---综合(hotScore) 2----价格(price)
         */
        String order = searchParam.getOrder();
        if(!StringUtils.isEmpty(order)){
            String[] orderParams = order.split(":");
            if(orderParams.length==2){
                String filedName="";
                switch (orderParams[0]){
                    case "1":
                        filedName="hotScore";
                        break;
                    case "2":
                        filedName="price";
                        break;
                }
                sourceBuilder.sort(filedName, "asc".equals(orderParams[1])? SortOrder.ASC:SortOrder.DESC);
            }
        }else {
            //如果没有传递排序的参数 按照综合人气排序
            sourceBuilder.sort("hotScore", SortOrder.DESC);
        }
        //9.设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);
        //10.设置品牌聚合
        TermsAggregationBuilder brandIdAggBuilder = AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"));
        sourceBuilder.aggregation(brandIdAggBuilder);

        //11.设置平台属性聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("platformPropertyAgg","platformProperty")
                .subAggregation( AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId")
                        .subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey"))
                        .subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))
                ));

        //12.设置需要返回那些字段
        sourceBuilder.fetchSource(new String[]{"id", "defaultImage", "productName", "price","hotScore"},null);
        //13.设置要查询那个index和type
        SearchRequest searchRequest = new SearchRequest("product");
        searchRequest.types("info");
        searchRequest.source(sourceBuilder);
        System.out.println("拼接好的DSL语句:"+sourceBuilder.toString());
        return searchRequest;
    }

}
