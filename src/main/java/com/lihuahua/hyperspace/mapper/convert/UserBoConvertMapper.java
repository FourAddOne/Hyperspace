package com.lihuahua.hyperspace.mapper.convert;

import com.lihuahua.hyperspace.models.bo.UserBO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.po.UserPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserBoConvertMapper {
    UserBoConvertMapper INSTANCE = Mappers.getMapper(UserBoConvertMapper.class);

    /**
     * 将 UserPO 转换为 UserBO
     * @param userPO 源对象
     * @return 目标对象
     */
    UserBO toBO(UserPO userPO);

    /**
     * 将 UserBO 转换为 UserPO
     * @param userBO 源对象
     * @return 目标对象
     */
    UserPO toPO(UserBO userBO);

    /**
     * 将 User 实体转换为 UserBO
     * @param user 源对象
     * @return 目标对象
     */
    UserBO toBO(User user);

    /**
     * 将 UserBO 转换为 User 实体
     * @param userBO 源对象
     * @return 目标对象
     */
    User toEntity(UserBO userBO);
}