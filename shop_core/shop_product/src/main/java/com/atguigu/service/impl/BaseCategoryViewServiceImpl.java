package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.mapper.BaseCategoryViewMapper;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * VIEW 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-02
 */
@Service
public class BaseCategoryViewServiceImpl extends ServiceImpl<BaseCategoryViewMapper, BaseCategoryView> implements BaseCategoryViewService {


    @Override
    public List<JSONObject> getIndexCategoryInfo() {
        //1.查询所有的分类信息
        List<BaseCategoryView> categoryViewList = baseMapper.selectList(null);
        ArrayList<JSONObject> categoryListJson = new ArrayList<>();
        //2.找到所有的一级分类
        Map<Long, List<BaseCategoryView>> category1Map = categoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        int index=1;
        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry:category1Map.entrySet()){
            Long category1Id = category1Entry.getKey();
            List<BaseCategoryView> category1List = category1Entry.getValue();
            //构造一个json对象 把一级分类封装到里面
            JSONObject category1 = new JSONObject();
            category1.put("index",index++);
            category1.put("categoryId",category1Id);
            category1.put("categoryName",category1List.get(0).getCategory1Name());
            //3.找到所有的二级分类
            Map<Long, List<BaseCategoryView>> category2Map = category1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            ArrayList<JSONObject> category1Children = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> category2Entry:category2Map.entrySet()) {
                Long category2Id = category2Entry.getKey();
                List<BaseCategoryView> category2List = category2Entry.getValue();
                //构造一个json对象 把一级分类封装到里面
                JSONObject category2= new JSONObject();
                category2.put("categoryId", category2Id);
                category2.put("categoryName", category2List.get(0).getCategory2Name());
                //4.找到所有的三级分类
                Map<Long, List<BaseCategoryView>> category3Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                ArrayList<JSONObject> category2Children = new ArrayList<>();
                for (Map.Entry<Long, List<BaseCategoryView>> category3Entry:category3Map.entrySet()) {
                    Long category3Id = category3Entry.getKey();
                    List<BaseCategoryView> category3List = category3Entry.getValue();
                    //构造一个json对象 把一级分类封装到里面
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId", category3Id);
                    category3.put("categoryName", category3List.get(0).getCategory3Name());
                    category2Children.add(category3);
                }
                category2.put("categoryChild",category2Children);
                category1Children.add(category2);
            }
            category1.put("categoryChild",category1Children);
            categoryListJson.add(category1);
        }
        return categoryListJson;
    }
//    @Override
//    public List<JSONObject> getIndexCategoryInfo() {
//        //查询所有
//        List<BaseCategoryView> categoryViewList = baseMapper.selectList(null);
//        ArrayList<JSONObject> categoryListJson = new ArrayList<>();
//        //找到所有一级分类
//        int index = 1;
//        Map<Long, List<BaseCategoryView>> category1Map = categoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
//        for (Map.Entry<Long,List<BaseCategoryView>> categoryView1Entry:category1Map.entrySet()){
//            Long category1Id = categoryView1Entry.getKey();
//            List<BaseCategoryView> category1List = categoryView1Entry.getValue();
//            //一级分类封装
//            JSONObject category1 = new JSONObject();
//            category1.put("index",index++);
//            category1.put("categoryId",category1Id);
//            category1.put("categoryName",category1List.get(0).getCategory1Name());
//
//            Map<Long, List<BaseCategoryView>> category2Map = category1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
//            ArrayList<JSONObject> category1Children = new ArrayList<>();
//            for (Map.Entry<Long,List<BaseCategoryView>> categoryView2Entry:category2Map.entrySet()){
//                Long category2Id = categoryView2Entry.getKey();
//                List<BaseCategoryView> category2List = categoryView2Entry.getValue();
//                //二级分类封装
//                JSONObject category2 = new JSONObject();
//                category2.put("categoryId",category2Id);
//                category2.put("categoryName",category2List.get(0).getCategory2Name());
//
//                //4.找到所有的三级分类
//                Map<Long, List<BaseCategoryView>> category3Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
//                ArrayList<JSONObject> category2Children = new ArrayList<>();
//                for (Map.Entry<Long, List<BaseCategoryView>> category3Entry:category3Map.entrySet()) {
//                    Long category3Id = category3Entry.getKey();
//                    List<BaseCategoryView> category3List = category3Entry.getValue();
//                    //构造一个json对象 把一级分类封装到里面
//                    JSONObject category3 = new JSONObject();
//                    category3.put("categoryId", category3Id);
//                    category3.put("categoryName", category3List.get(0).getCategory3Name());
//                    category2Children.add(category3);
//                }
//                category2.put("categoryChildren",category2Children);
//                category1Children.add(category2);
//            }
//            category1.put("categoryChildren",category1Children);
//            categoryListJson.add(category1);
//        }
//        return categoryListJson;
//    }
}
