package com.tianji.api.dto;

import com.tianji.common.utils.CollUtils;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * id和num模型，一个id对应的数量可以用与查询id和num的关系
 **/
@Data
public class IdAndNumDTO {
    private Long id;
    private Integer num;

    public static Map<Long, Integer> toMap(List<IdAndNumDTO> list){
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(IdAndNumDTO::getId, IdAndNumDTO::getNum));
    }
}
