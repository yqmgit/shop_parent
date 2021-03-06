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
        //??????product???????????????????????????????????????
        Product product = new Product();
        //??????????????????
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo != null) {
            product.setId(skuInfo.getId());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setProductName(skuInfo.getSkuName());
            product.setCreateTime(new Date());
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
        }
        //????????????
        BaseBrand brand = productFeignClient.getBrandById1(skuInfo.getBrandId());
        if (brand != null) {
            product.setBrandId(brand.getId());
            product.setBrandName(brand.getBrandName());
            product.setBrandLogoUrl(brand.getBrandLogoUrl());
        }

        //??????????????????
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        if (categoryView != null) {
            product.setCategory1Id(categoryView.getCategory1Id());
            product.setCategory1Name(categoryView.getCategory1Name());
            product.setCategory2Id(categoryView.getCategory2Id());
            product.setCategory2Name(categoryView.getCategory2Name());
            product.setCategory3Id(categoryView.getCategory3Id());
            product.setCategory3Name(categoryView.getCategory3Name());
        }

        //??????????????????
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
        //?????????????????????es???
        productRepository.save(product);
    }

    @Override
    public void offSale(Long skuId) {
        productRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        //??????????????????key?????????
        String hotKey = "sku:hotscore";
        double count = redisTemplate.opsForZSet().incrementScore(hotKey,skuId,1);
        //????????????????????????es?????????
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
        //1.??????DSL????????????
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);
        //2.???????????????????????????
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //3.??????????????????????????????????????????
        SearchResponseVo searchResponseVo = this.parseSearchResult(searchResponse);
        //4.???????????????????????????
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        //5.???????????????
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

    //???????????????????????????
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //1.???????????????????????????
        SearchHits firstHists = searchResponse.getHits();
        //??????????????????
        searchResponseVo.setTotal(firstHists.totalHits);
        SearchHit[] secondHits = firstHists.getHits();
        //???????????????????????????
        List<Product> productList = new ArrayList<>();
        if(secondHits!=null&&secondHits.length>0){
            for (SearchHit secondHit : secondHits) {
                //???json?????????product??????
                Product product = JSONObject.parseObject(secondHit.getSourceAsString(), Product.class);
                //??????????????????
                HighlightField highlightField = secondHit.getHighlightFields().get("productName");
                if(highlightField!=null){
                    Text highlightProductName = highlightField.getFragments()[0];
                    product.setProductName(highlightProductName.toString());
                }
                productList.add(product);
            }
            searchResponseVo.setProductList(productList);
        }
        //2.??????????????????
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        List<SearchBrandVo> searchBrandVoList = brandIdAgg.getBuckets().stream().map(bucket -> {
            SearchBrandVo searchBrandVo = new SearchBrandVo();
            //????????????id
            String brandId = bucket.getKeyAsString();
            searchBrandVo.setBrandId(Long.parseLong(brandId));
            //??????????????????
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandName(brandName);
            //????????????logo
            ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
            String brandLogoUrl = brandLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandLogoUrl(brandLogoUrl);
            return searchBrandVo;
        }).collect(Collectors.toList());
        searchResponseVo.setBrandVoList(searchBrandVoList);
        //3.????????????????????????
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        //?????????agg??????agg
        ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<? extends Terms.Bucket> buckets = propertyKeyIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(buckets)){
            //??????buckets
            List<SearchPlatformPropertyVo> platformPropertyVoList = buckets.stream().map(bucket -> {
                SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
                //????????????id
                Number propertyKeyId = bucket.getKeyAsNumber();
                searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId.longValue());
                //??????????????????
                ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
                String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
                searchPlatformPropertyVo.setPropertyKey(propertyKey);
                //???????????????
                ParsedStringTerms propertyValueAgg = bucket.getAggregations().get("propertyValueAgg");
                List<? extends Terms.Bucket> propertyValueBuckets = propertyValueAgg.getBuckets();
                //??????????????????????????????
                List<String> propertyValueList = propertyValueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                searchPlatformPropertyVo.setPropertyValueList(propertyValueList);
                return searchPlatformPropertyVo;
            }).collect(Collectors.toList());
            searchResponseVo.setPlatformPropertyList(platformPropertyVoList);
        }
        return searchResponseVo;


    }

    //??????DSL????????????(SQL)
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //1.???????????????bool
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //2.?????????????????????
        if(!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //??????????????????id?????????
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            firstBool.filter(category1Id);
        }
        if(!StringUtils.isEmpty(searchParam.getCategory2Id())){
            //??????????????????id?????????
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            firstBool.filter(category2Id);
        }
        if(!StringUtils.isEmpty(searchParam.getCategory3Id())){
            //??????????????????id?????????
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            firstBool.filter(category3Id);
        }
        //3.????????????????????? ????????????brandName=1:??????
        String brandName = searchParam.getBrandName();
        if(!StringUtils.isEmpty(brandName)){
            String[] brandParam = brandName.split(":");
            if(brandParam.length==2){
                firstBool.filter(QueryBuilders.termQuery("brandId",brandParam[0]));
            }
        }
        //4.??????????????????????????? ???????????? props=4:??????888:CPU??????&props=5:6.55-6.64??????:????????????
        String[] props = searchParam.getProps();
        if(props!=null&&props.length>0){
            for (String prop : props) {
                //4:??????888:CPU??????
                String[] platformParams = prop.split(":");
                if(platformParams.length==3){
                    BoolQueryBuilder boolQuery=QueryBuilders.boolQuery();
                    //????????????bool
                    BoolQueryBuilder childBoolQuery=QueryBuilders.boolQuery();
                    childBoolQuery.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", platformParams[0]));
                    childBoolQuery.must(QueryBuilders.termQuery("platformProperty.propertyValue", platformParams[1]));
                    boolQuery.must(QueryBuilders.nestedQuery("platformProperty",childBoolQuery, ScoreMode.None));
                    firstBool.filter(boolQuery);
                }
            }
        }
        //5.??????????????????????????????????????? ???????????? keyword=??????
        String keyword = searchParam.getKeyword();
        if(!StringUtils.isEmpty(keyword)){
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("productName", keyword).operator(Operator.AND);
            firstBool.must(matchQuery);
        }
        //6.??????query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(firstBool);
        //7.???????????? ?????????(pageNo-1)*pageSize
        int from=(searchParam.getPageNo()-1)*searchParam.getPageSize();
        sourceBuilder.from(from);
        sourceBuilder.size(searchParam.getPageSize());
        /**
         * 8.????????????  ???????????? order=2:desc
         * 1---??????(hotScore) 2----??????(price)
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
            //????????????????????????????????? ????????????????????????
            sourceBuilder.sort("hotScore", SortOrder.DESC);
        }
        //9.????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);
        //10.??????????????????
        TermsAggregationBuilder brandIdAggBuilder = AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"));
        sourceBuilder.aggregation(brandIdAggBuilder);

        //11.????????????????????????
        sourceBuilder.aggregation(AggregationBuilders.nested("platformPropertyAgg","platformProperty")
                .subAggregation( AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId")
                        .subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey"))
                        .subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))
                ));

        //12.??????????????????????????????
        sourceBuilder.fetchSource(new String[]{"id", "defaultImage", "productName", "price","hotScore"},null);
        //13.?????????????????????index???type
        SearchRequest searchRequest = new SearchRequest("product");
        searchRequest.types("info");
        searchRequest.source(sourceBuilder);
        System.out.println("????????????DSL??????:"+sourceBuilder.toString());
        return searchRequest;
    }

}
