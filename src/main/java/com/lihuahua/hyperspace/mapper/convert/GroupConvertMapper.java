package com.lihuahua.hyperspace.mapper.convert;

import com.lihuahua.hyperspace.models.bo.GroupBO;
import com.lihuahua.hyperspace.models.bo.GroupMemberBO;
import com.lihuahua.hyperspace.models.entity.Group;
import com.lihuahua.hyperspace.models.entity.GroupMember;
import com.lihuahua.hyperspace.models.po.GroupPO;
import com.lihuahua.hyperspace.models.po.GroupMemberPO;
import com.lihuahua.hyperspace.models.vo.GroupVO;
import com.lihuahua.hyperspace.models.vo.GroupMemberVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GroupConvertMapper {
    GroupConvertMapper INSTANCE = Mappers.getMapper(GroupConvertMapper.class);

    /**
     * 将 GroupPO 转换为 GroupBO
     * @param groupPO 源对象
     * @return 目标对象
     */
    GroupBO toBO(GroupPO groupPO);

    /**
     * 将 GroupBO 转换为 GroupPO
     * @param groupBO 源对象
     * @return 目标对象
     */
    GroupPO toPO(GroupBO groupBO);

    /**
     * 将 GroupMemberPO 转换为 GroupMemberBO
     * @param memberPO 源对象
     * @return 目标对象
     */
    GroupMemberBO toBO(GroupMemberPO memberPO);

    /**
     * 将 GroupMemberBO 转换为 GroupMemberPO
     * @param memberBO 源对象
     * @return 目标对象
     */
    GroupMemberPO toPO(GroupMemberBO memberBO);

    /**
     * 批量将 GroupMemberPO 列表转换为 GroupMemberBO 列表
     * @param memberPOs 源对象列表
     * @return 目标对象列表
     */
    List<GroupMemberBO> toBOList(List<GroupMemberPO> memberPOs);

    /**
     * 批量将 GroupMemberBO 列表转换为 GroupMemberPO 列表
     * @param memberBOs 源对象列表
     * @return 目标对象列表
     */
    List<GroupMemberPO> toPOList(List<GroupMemberBO> memberBOs);

    /**
     * 将 Group 转换为 GroupVO
     * @param group 源对象
     * @return 目标对象
     */
    GroupVO toVO(Group group);

    /**
     * 将 GroupMember 转换为 GroupMemberVO
     * @param groupMember 源对象
     * @return 目标对象
     */
    GroupMemberVO toVO(GroupMember groupMember);
}