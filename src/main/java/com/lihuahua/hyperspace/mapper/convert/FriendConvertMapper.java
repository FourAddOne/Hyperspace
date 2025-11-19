package com.lihuahua.hyperspace.mapper.convert;

import com.lihuahua.hyperspace.models.bo.FriendBO;
import com.lihuahua.hyperspace.models.entity.Friend;
import com.lihuahua.hyperspace.models.po.FriendPO;
import com.lihuahua.hyperspace.models.vo.FriendVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FriendConvertMapper {
    FriendConvertMapper INSTANCE = Mappers.getMapper(FriendConvertMapper.class);

    /**
     * 将 FriendPO 转换为 FriendBO
     * @param friendPO 源对象
     * @return 目标对象
     */
    FriendBO toBO(FriendPO friendPO);

    /**
     * 将 FriendBO 转换为 FriendPO
     * @param friendBO 源对象
     * @return 目标对象
     */
    @Mapping(target = "id", ignore = true)
    FriendPO toPO(FriendBO friendBO);

    /**
     * 将 Friend 转换为 FriendVO
     * @param friend 源对象
     * @return 目标对象
     */
    FriendVO toVO(Friend friend);

    /**
     * 批量将 FriendPO 列表转换为 FriendBO 列表
     * @param friendPOs 源对象列表
     * @return 目标对象列表
     */
    List<FriendBO> toBOList(List<FriendPO> friendPOs);

    /**
     * 批量将 FriendBO 列表转换为 FriendPO 列表
     * @param friendBOs 源对象列表
     * @return 目标对象列表
     */
    List<FriendPO> toPOList(List<FriendBO> friendBOs);
}