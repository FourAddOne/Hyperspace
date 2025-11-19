package com.lihuahua.hyperspace.mapper.convert;

import com.lihuahua.hyperspace.models.bo.UserBO;
import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.po.UserPO;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserConvertMapper {
    UserConvertMapper INSTANCE = Mappers.getMapper(UserConvertMapper.class);

    /**
     * 将 UserPO 转换为 User 实体
     * @param userPO 源对象
     * @return 目标对象
     */
    User toEntity(UserPO userPO);

    /**
     * 将 User 实体转换为 UserPO
     * @param user 源对象
     * @return 目标对象
     */
    UserPO toPO(User user);

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

    /**
     * 将 User 实体转换为 UserLoginVO
     * @param user 源对象
     * @return 目标对象
     */
    UserLoginVO toUserLoginVO(User user);

    /**
     * 将 RegisterDTO 转换为 UserPO
     * @param registerDTO 源对象
     * @return 目标对象
     */
    UserPO toPO(RegisterDTO registerDTO);

    /**
     * 将 LoginDTO 转换为 UserPO (用于查询)
     * @param loginDTO 源对象
     * @return 目标对象
     */
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "registerIp", ignore = true)
    @Mapping(target = "loginIp", ignore = true)
    @Mapping(target = "lastReadTs", ignore = true)
    @Mapping(target = "loginStatus", ignore = true)
    UserPO toPOForQuery(LoginDTO loginDTO);
    
    /**
     * 将 UserPO 转换为 UserBO
     * @param userPO 源对象
     * @return 目标对象
     */
    UserBO toBO(UserPO userPO);
}