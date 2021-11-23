package com.atguigu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * spu销售属性
 * </p>
 *
 * @author zhangqiang
 * @since 2021-03-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("product_sale_property_key")
@ApiModel(value="ProductSalePropertyKey对象", description="spu销售属性")
public class ProductSalePropertyKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "编号(业务中无关联)")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "商品id")
    private Long productId;

    @ApiModelProperty(value = "销售属性id")
    private Long salePropertyKeyId;

    @ApiModelProperty(value = "销售属性名称(冗余)")
    private String salePropertyKeyName;

    // 销售属性对象集合
    @TableField(exist = false)
    private List<ProductSalePropertyValue> salePropertyValueList;


}
